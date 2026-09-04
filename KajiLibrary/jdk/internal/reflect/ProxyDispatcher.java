package jdk.internal.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El punto al que cae TODO metodo de TODA clase de proxy generada.
 *
 * <h2>Por que existe esta clase y no bytecode</h2>
 *
 * <p>Un proxy podria generarse con el cuerpo entero en bytecode -- es lo que hace el JDK: cada
 * metodo carga el campo `h`, busca su `Method` en un campo estatico, arma el arreglo, llama a
 * `invoke` y trae un `try/catch` completo para traducir excepciones. Eso son un `<clinit>` que
 * resuelve metodos por nombre, una tabla de campos estaticos y una tabla de excepciones por
 * metodo: tres generadores mas.
 *
 * <p>Aca el bytecode generado hace lo unico que no se puede escribir en Java -- tener una firma
 * arbitraria -- y delega el resto: empuja `this`, un `int` con el indice del metodo y los
 * argumentos boxeados, y llama a {@link #despachar}. Todo lo demas (encontrar el `Method`, sacar
 * el manejador, traducir lo que se tire) esta escrito aca, en Java, una sola vez y no una vez por
 * metodo generado.
 *
 * <h2>Por que un indice y no un nombre</h2>
 *
 * <p>El indice es la posicion del metodo en la tabla que {@link Proxy} registro para esa clase
 * justo despues de definirla. Numerar en vez de nombrar es lo que deja el bytecode sin ninguna
 * busqueda -- y ademas es lo unico que distingue dos metodos que comparten nombre y parametros y
 * difieren solo en el tipo de retorno, cosa legal en un archivo de clase y que pasa de verdad
 * cuando dos interfaces declaran retornos covariantes.
 */
public final class ProxyDispatcher {

    /**
     * De clase de proxy a su tabla. Concurrente y no sincronizada porque se lee en cada llamada a
     * cada proxy del programa: un candado ahi convierte a todo proxy en un punto de serializacion.
     * La escritura pasa una sola vez por clase, antes de que exista la primera instancia.
     */
    private static final ConcurrentHashMap<Class<?>, Tabla> TABLAS =
            new ConcurrentHashMap<Class<?>, Tabla>();

    private ProxyDispatcher() {
    }

    /** Lo que hace falta saber de un metodo generado, en dos arreglos paralelos. */
    private static final class Tabla {
        final Method[] metodos;
        final Class<?>[][] declaradas;

        Tabla(Method[] metodos, Class<?>[][] declaradas) {
            this.metodos = metodos;
            this.declaradas = declaradas;
        }
    }

    /**
     * Registra la tabla de una clase de proxy recien definida.
     *
     * <p>La llama {@link Proxy} entre `defineClass` y la primera instancia: en ese hueco la clase
     * existe pero nadie la puede invocar todavia, asi que no hay carrera que resolver.
     *
     * @param clase la clase generada
     * @param metodos los `Method` en el orden en que el bytecode los indexa
     * @param declaradas por cada metodo, las excepciones que declara despues de fundir las de
     *        todas las interfaces que lo declaran
     */
    public static void registrar(Class<?> clase, Method[] metodos, Class<?>[][] declaradas) {
        TABLAS.put(clase, new Tabla(metodos, declaradas));
    }

    /**
     * Ejecuta una llamada a un metodo de proxy.
     *
     * <p>La traduccion de excepciones es la clausula del contrato que no cae sola de ninguna
     * eleccion de bytecode y por eso se escribe: lo no chequeado sale tal cual, lo chequeado que
     * el metodo declara sale tal cual, y cualquier otra cosa sale envuelta en
     * {@link UndeclaredThrowableException} -- porque un metodo no puede tirar una chequeada que
     * su firma no menciona, y mentir sobre eso rompe a quien lo llama.
     *
     * @param proxy la instancia sobre la que se llamo
     * @param indice la posicion del metodo en la tabla de su clase
     * @param args los argumentos boxeados, o `null` si el metodo no toma ninguno
     * @return lo que el manejador devuelva; el bytecode que llamo lo castea y desboxea
     * @throws Throwable lo que el manejador tire, ya traducido
     */
    public static Object despachar(Object proxy, int indice, Object[] args) throws Throwable {
        Tabla tabla = TABLAS.get(proxy.getClass());
        if (tabla == null) {
            // Solo puede pasar si alguien definio a mano una clase que llama aca. No es un caso
            // que el contrato contemple, pero fallar con un mensaje es mejor que con un NPE.
            throw new InternalError("proxy sin tabla registrada: " + proxy.getClass().getName());
        }
        Method metodo = tabla.metodos[indice];
        InvocationHandler manejador = Proxy.getInvocationHandler(proxy);
        try {
            return manejador.invoke(proxy, metodo, args);
        } catch (RuntimeException noChequeada) {
            throw noChequeada;
        } catch (Error error) {
            throw error;
        } catch (Throwable chequeada) {
            Class<?>[] permitidas = tabla.declaradas[indice];
            int i = 0;
            while (i < permitidas.length) {
                if (permitidas[i].isInstance(chequeada)) {
                    throw chequeada;
                }
                i = i + 1;
            }
            throw new UndeclaredThrowableException(chequeada);
        }
    }
}
