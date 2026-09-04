package jdk.internal.net;

/**
 * El puente por el que un canal de `java.nio.channels` consigue el socket de `java.net` que lo
 * envuelve.
 *
 * <h2>Por que hace falta un puente</h2>
 *
 * <p>{@code SocketChannel.socket()} tiene que devolver un {@link java.net.Socket} **sobre el mismo
 * socket del sistema** que el canal ya tiene abierto. Fabricar uno asi no es publico ni puede serlo:
 * `java.net.Socket` no expone forma de decir "envolveme este handle", y agregarsela seria un miembro
 * que el JDK no tiene, o sea romper el contrato para arreglarlo.
 *
 * <p>Los dos paquetes tampoco pueden hablarse directo --nada de `java.net` es visible de paquete
 * desde `java.nio.channels`-- asi que el puente vive aca, en el unico lugar que los dos ya conocen.
 * Es el mismo patron que el JDK resuelve con sus *shared secrets*, por la misma razon y con la misma
 * forma: el paquete de arriba **registra** como fabricar, el de abajo **pide**.
 *
 * <h2>Por que `Object` y no los tipos</h2>
 *
 * <p>Los metodos devuelven `Object` y quien pide castea. No es pereza: si esta clase nombrara
 * `java.net.Socket`, `java.net` y `jdk.internal.net` se referenciarian **en los dos sentidos**, y
 * una dependencia circular entre paquetes de la biblioteca base es justo lo que conviene no tener
 * (ver el hallazgo #474 sobre lo que pasa compilando en lote). Con `Object` la flecha va en una sola
 * direccion.
 *
 * <h2>Quien registra, y cuando</h2>
 *
 * <p>`java.net` se registra en el inicializador estatico de {@link java.net.Socket}, asi que basta
 * con que esa clase se **inicialice**. Como quien pide puede ser el primero en llegar, {@link
 * #exigir} lo fuerza antes de darse por vencido: sin eso, el orden de carga decidiria si `socket()`
 * anda, que es el peor tipo de error --el que aparece segun quien corrio antes--.
 *
 * <p>Se fuerza **por nombre**, que es justamente lo que `Class.forName` promete y lo unico que no
 * vuelve circular la dependencia entre los dos paquetes. Durante un rato no se pudo: `forName` no
 * corria el inicializador (hallazgo #487, ya arreglado), y este puente fue lo que lo destapo.
 */
public final class Adopcion {

    private Adopcion() {
    }

    /** Lo que `java.net` sabe hacer y `java.nio.channels` necesita. */
    public interface Fabrica {

        /** Un `java.net.Socket` sobre ese socket ya conectado. */
        Object tcp(int handle);

        /** Un `java.net.ServerSocket` sobre ese socket ya atado. */
        Object servidor(int handle);

        /** Un `java.net.DatagramSocket` sobre ese socket ya atado. */
        Object datagrama(int handle);
    }

    private static volatile Fabrica fabrica;

    /** La instala `java.net`. Llamarla dos veces pisa la anterior, que es lo que se quiere. */
    public static void registrar(Fabrica f) {
        fabrica = f;
    }

    /** Un `java.net.Socket` sobre ese handle. */
    public static Object tcp(int handle) {
        return Adopcion.exigir().tcp(handle);
    }

    /** Un `java.net.ServerSocket` sobre ese handle. */
    public static Object servidor(int handle) {
        return Adopcion.exigir().servidor(handle);
    }

    /** Un `java.net.DatagramSocket` sobre ese handle. */
    public static Object datagrama(int handle) {
        return Adopcion.exigir().datagrama(handle);
    }

    private static Fabrica exigir() {
        Fabrica f = fabrica;
        if (f == null) {
            // Todavia no se cargo. Se fuerza, y con eso corre su inicializador estatico, que es
            // donde se registra.
            try {
                Class.forName("java.net.Socket");
            } catch (ClassNotFoundException e) {
                // No esta. El mensaje de abajo dice lo unico que se sabe.
            }
            f = fabrica;
        }
        if (f == null) {
            throw new IllegalStateException("java.net no registro como adoptar un socket");
        }
        return f;
    }
}
