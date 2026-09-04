package javax.management.remote;

/**
 * KajiLibrary's javax.management.remote.JMXAddressable -- esto tiene una direccion.
 *
 * <p>Un solo metodo. La implementan los conectores y servidores que pueden decir a que
 * {@link JMXServiceURL} corresponden.
 *
 * <p>Es opcional, y por eso es una interfaz aparte: hay conectores que se arman sobre una conexion ya
 * abierta y no tienen ninguna direccion que dar. Se pregunta con {@code instanceof}.
 */
public interface JMXAddressable {

    /** La direccion, o null si todavia no se sabe. */
    JMXServiceURL getAddress();
}
