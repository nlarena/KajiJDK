package javax.management;

/**
 * La raiz **no verificada** de JMX: lo que el que llama no puede prever ni atender.
 *
 * <p>Las tres subclases que importan envuelven algo que reventó del otro lado --
 * {@link RuntimeMBeanException} un `RuntimeException` del MBean, {@link RuntimeErrorException} un
 * `Error`, {@link RuntimeOperationsException} un `RuntimeException` del agente. El envoltorio no es
 * ceremonia: cruzar la frontera del servidor de MBeans convierte "el MBean tiene un bug" en un tipo
 * que el cliente puede distinguir de "el agente tiene un bug".
 */
public class JMRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 6573344628407841861L;

    /** Sin mensaje. */
    public JMRuntimeException() {
        super();
    }

    /** Con el mensaje que explica que fallo. */
    public JMRuntimeException(String msg) {
        super(msg);
    }

    /**
     * De paquete a proposito, igual que en el JDK: la causa la exponen las subclases por su campo
     * propio y su `getCause()`, no este constructor.
     */
    JMRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
