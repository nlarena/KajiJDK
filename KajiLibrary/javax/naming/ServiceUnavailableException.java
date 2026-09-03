package javax.naming;

/**
 * Se lanza cuando se llego al servicio y el servicio dijo que no esta disponible. Contra
 * `CommunicationException`, aca el canal anduvo: el que no esta es el servicio.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class ServiceUnavailableException extends NamingException {

    private static final long serialVersionUID = -4996964726566773444L;

    public ServiceUnavailableException(String explanation) {
        super(explanation);
    }

    public ServiceUnavailableException() {
        super();
    }
}
