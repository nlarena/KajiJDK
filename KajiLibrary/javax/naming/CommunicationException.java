package javax.naming;

/**
 * Se lanza cuando fallo la comunicacion con el servicio de nombres: conexion caida, protocolo
 * roto, respuesta ilegible. La causa concreta suele venir encadenada como causa raiz.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class CommunicationException extends NamingException {

    private static final long serialVersionUID = 3618507780299986611L;

    public CommunicationException(String explanation) {
        super(explanation);
    }

    public CommunicationException() {
        super();
    }
}
