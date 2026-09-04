package jdk.internal.vm;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * KajiLibrary's jdk.internal.vm.SharedThreadContainer — un contenedor **sin dueño**.
 *
 * <p>La diferencia con un {@link ThreadContainer} común está en el nombre: éste no pertenece al hilo
 * que lo creó. Es lo que necesita un pool de hilos, donde el que abre el contenedor y los que corren
 * adentro no tienen nada que ver, y donde cerrarlo lo puede hacer cualquiera.
 *
 * <p>Por eso {@link #owner()} da `null` y por eso es {@link AutoCloseable}: su tiempo de vida no está
 * atado a una llamada que empieza y termina, así que hay que cerrarlo a mano —o con
 * `try`-con-recursos, que es la forma de no olvidarse—.
 *
 * <p>El conjunto de hilos es un {@link ConcurrentHashMap} de sólo claves, y tiene que ser concurrente
 * de verdad: los hilos entran y salen desde ellos mismos, en paralelo, sin nada que los serialice.
 */
public class SharedThreadContainer extends ThreadContainer implements AutoCloseable {

    private final String nombre;
    private final Set<Thread> hilos = ConcurrentHashMap.newKeySet();
    private volatile Object llave;
    private volatile boolean cerrado;

    private SharedThreadContainer(String name) {
        super(true);
        this.nombre = name;
    }

    /**
     * Crea uno anidado en `parent`.
     *
     * <p>El `parent` se acepta y **no se guarda**, y conviene decir por qué no se pierde nada: el
     * padre de un contenedor sale de la pila de ámbitos donde se creó ({@link ThreadContainers}), no
     * de un campo. Guardarlo además abriría la posibilidad de que los dos digan cosas distintas.
     */
    public static SharedThreadContainer create(ThreadContainer parent, String name) {
        return SharedThreadContainer.create(name);
    }

    /** Crea uno y lo registra. */
    public static SharedThreadContainer create(String name) {
        SharedThreadContainer c = new SharedThreadContainer(name);
        c.llave = ThreadContainers.registerContainer(c);
        return c;
    }

    public String name() {
        return this.nombre;
    }

    /** Siempre `null`: es compartido, no tiene dueño. */
    public Thread owner() {
        return null;
    }

    public void onStart(Thread thread) {
        this.hilos.add(thread);
    }

    public void onExit(Thread thread) {
        this.hilos.remove(thread);
    }

    public Stream<Thread> threads() {
        return this.hilos.stream();
    }

    /**
     * Arranca un hilo dentro de este contenedor.
     *
     * <p>Se anota **antes** de arrancarlo y se lo saca si el arranque falla. El orden importa: al
     * revés habría una ventana en la que el hilo ya corre y el contenedor todavía no lo conoce, y en
     * esa ventana `threadCount()` mentiría.
     *
     * @throws IllegalStateException si el contenedor ya se cerró
     */
    public void start(Thread thread) {
        if (this.cerrado) {
            throw new IllegalStateException("este contenedor ya se cerro");
        }
        this.onStart(thread);
        try {
            thread.start();
        } catch (RuntimeException e) {
            this.onExit(thread);
            throw e;
        }
    }

    /**
     * Cierra el contenedor y lo saca del registro.
     *
     * <p>**No espera a los hilos ni los interrumpe**, igual que el JDK: cerrar es decir "no entra
     * nadie más", no "terminen". Esperar es responsabilidad de quien abrió el ámbito, que es el único
     * que sabe qué significa que hayan terminado. Cerrar dos veces no hace nada.
     */
    public void close() {
        if (this.cerrado) {
            return;
        }
        this.cerrado = true;
        ThreadContainers.deregisterContainer(this.llave);
    }

    /** Cerrar un contenedor compartido no puede fallar, así que el gancho dice que sí. */
    protected boolean tryClose() {
        this.close();
        return true;
    }
}
