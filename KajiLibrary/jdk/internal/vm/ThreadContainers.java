package jdk.internal.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * KajiLibrary's jdk.internal.vm.ThreadContainers — el registro de {@link ThreadContainer}s vivos.
 *
 * <p>Un contenedor sabe qué hilos tiene, pero nadie sabe qué contenedores hay: hacen falta las dos
 * cosas para que una herramienta de diagnóstico pueda recorrer el árbol entero de un proceso. Este
 * registro es la segunda mitad.
 *
 * <p>El registro es **débil por diseño en el JDK** --guarda referencias débiles, así que un contenedor
 * que nadie usa se recolecta y desaparece solo--. Acá se usa una lista con sincronización y
 * {@link #deregisterContainer} explícito, porque esta biblioteca no tiene referencias débiles con
 * cola. La diferencia visible: un contenedor que se abandona sin cerrar **queda en el registro**.
 * `SharedThreadContainer.close()` lo da de baja, que es el camino normal.
 *
 * <p>{@link #root()} no es un contenedor real registrado, sino el que representa "todo lo que no está
 * adentro de ninguno": los hilos de la plataforma que arrancaron por su cuenta.
 */
public class ThreadContainers {

    private static final List<ThreadContainer> REGISTRADOS = new ArrayList<ThreadContainer>();
    private static final Object CANDADO = new Object();
    private static final ThreadContainer RAIZ = new ContenedorRaiz();

    private ThreadContainers() {
    }

    /**
     * Si se pueden encontrar **todos** los hilos o sólo los que están dentro de un contenedor.
     *
     * <p>Acá es `false`, y llegar a esa respuesta costó una prueba. La primera versión devolvía
     * `true` con el argumento de que {@link #root()} enumera la plataforma entera recorriendo el
     * `ThreadGroup` raíz. **Ese argumento es falso en esta VM**: `ThreadGroup` no lleva registro de
     * sus miembros, `activeCount()` da 0 y `enumerate()` no devuelve nada, aun con hilos corriendo.
     *
     * <p>Así que un hilo que arrancó por su cuenta, fuera de todo contenedor, no se puede encontrar.
     * Decir `false` es lo que permite que quien pregunte sepa que el recorrido va a estar incompleto,
     * en vez de creer que vio todo.
     */
    public static boolean trackAllThreads() {
        return false;
    }

    /**
     * Registra un contenedor y devuelve la **llave** para darlo de baja.
     *
     * <p>Devuelve un `Object` opaco en vez del contenedor mismo: quien registra es el único que puede
     * desregistrar, y con una llave el registro no depende de que el que llama conserve --y compare
     * bien-- una referencia al contenedor.
     */
    public static Object registerContainer(ThreadContainer container) {
        if (container == null) {
            throw new NullPointerException("container");
        }
        synchronized (ThreadContainers.CANDADO) {
            ThreadContainers.REGISTRADOS.add(container);
        }
        return container;
    }

    /** Da de baja lo que {@link #registerContainer} devolvió. */
    public static void deregisterContainer(Object key) {
        synchronized (ThreadContainers.CANDADO) {
            ThreadContainers.REGISTRADOS.remove(key);
        }
    }

    /** El contenedor raíz: los hilos que no están dentro de ninguno. */
    public static ThreadContainer root() {
        return ThreadContainers.RAIZ;
    }

    /** El contenedor que encierra a `container`, deducido de la pila de ámbitos. */
    static ThreadContainer parent(ThreadContainer container) {
        ThreadContainer arriba = container.enclosingScope(ThreadContainer.class);
        if (arriba != null) {
            return arriba;
        }
        return container == ThreadContainers.RAIZ ? null : ThreadContainers.RAIZ;
    }

    /** Los contenedores registrados cuyo padre es `container`. */
    static Stream<ThreadContainer> children(ThreadContainer container) {
        List<ThreadContainer> hijos = new ArrayList<ThreadContainer>();
        synchronized (ThreadContainers.CANDADO) {
            for (ThreadContainer c : ThreadContainers.REGISTRADOS) {
                if (ThreadContainers.parent(c) == container) {
                    hijos.add(c);
                }
            }
        }
        return hijos.stream();
    }

    /**
     * El contenedor donde está `thread`, o el raíz si no está en ninguno.
     *
     * <p>Se busca preguntándole a cada contenedor registrado si tiene ese hilo. El JDK lo lee de un
     * campo del propio `Thread`, que es O(1); acá no se puede tocar `Thread` desde este paquete, y la
     * respuesta es la misma.
     */
    public static ThreadContainer container(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        synchronized (ThreadContainers.CANDADO) {
            for (ThreadContainer c : ThreadContainers.REGISTRADOS) {
                if (c.threads().anyMatch(t -> t == thread)) {
                    return c;
                }
            }
        }
        return ThreadContainers.RAIZ;
    }

    // El contenedor raiz: representa "los hilos que no estan dentro de ningun contenedor".
    //
    // Los busca recorriendo el grupo de hilos raiz, y **en esta VM eso encuentra poco**: `ThreadGroup`
    // no lleva registro de sus miembros (`enumerate` devuelve 0 aun con hilos vivos), asi que en la
    // practica el recorrido da el hilo que pregunta y nada mas. Se deja el recorrido igual --el dia
    // que `ThreadGroup` lleve registro, esto empieza a dar la respuesta completa sin tocar nada-- y
    // se garantiza al menos el hilo actual, porque un contenedor que dice tener cero hilos mientras
    // uno lo esta llamando estaria mintiendo. `trackAllThreads()` devuelve `false` por esto mismo.
    private static final class ContenedorRaiz extends ThreadContainer {

        ContenedorRaiz() {
            super(true);
        }

        public String name() {
            return "<root>";
        }

        public ThreadContainer parent() {
            return null;
        }

        public Stream<Thread> threads() {
            ThreadGroup g = Thread.currentThread().getThreadGroup();
            while (g != null && g.getParent() != null) {
                g = g.getParent();
            }
            if (g == null) {
                return Stream.of(Thread.currentThread());
            }
            Thread[] buf = new Thread[g.activeCount() + 8];
            int n = g.enumerate(buf, true);
            List<Thread> vivos = new ArrayList<Thread>();
            for (int i = 0; i < n; i++) {
                if (buf[i] != null) {
                    vivos.add(buf[i]);
                }
            }
            Thread yo = Thread.currentThread();
            if (!vivos.contains(yo)) {
                vivos.add(yo);
            }
            return vivos.stream();
        }
    }
}
