package javax.management;

/**
 * Envuelve una excepcion **de aplicacion** que tiró el MBean.
 *
 * <p>Es la frontera que hace utilizable a un servidor de MBeans generico: sin ella, el que invoca
 * una operacion recibiria la excepcion cruda del MBean y no podria distinguirla de una del agente.
 * Con ella, `MBeanException` significa siempre "tu MBean fallo" y cualquier otra cosa significa "el
 * agente fallo".
 *
 * <p>Guarda la envuelta en un campo propio ({@code exception}) porque JMX es anterior al encadenado
 * de `Throwable`; {@link #getCause()} la publica ademas por la via moderna para que las trazas de
 * pila la impriman.
 */
public class MBeanException extends JMException {

    private static final long serialVersionUID = 4066342430588744142L;

    /**
     * @serial la excepcion de aplicacion envuelta
     */
    private java.lang.Exception exception;

    /** Envuelve `e` sin mensaje propio. */
    public MBeanException(java.lang.Exception e) {
        super();
        exception = e;
    }

    /** Envuelve `e` con un mensaje del agente. */
    public MBeanException(java.lang.Exception e, String message) {
        super(message);
        exception = e;
    }

    /** La excepcion de aplicacion envuelta. */
    public java.lang.Exception getTargetException() {
        return exception;
    }

    /** Lo mismo que {@link #getTargetException()}, por la via moderna. */
    public Throwable getCause() {
        return exception;
    }
}
