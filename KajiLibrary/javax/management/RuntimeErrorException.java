package javax.management;

/**
 * Envuelve un `Error` que salio del MBean.
 *
 * <p>Existe por una razon de tipos, no de diagnostico: un `Error` no es un `Exception`, asi que
 * {@link MBeanException} no lo puede llevar. Sin este envoltorio un `OutOfMemoryError` del MBean
 * subiria crudo por el agente y no se distinguiria de uno del agente mismo.
 */
public class RuntimeErrorException extends JMRuntimeException {

    private static final long serialVersionUID = 704338937753949796L;

    /**
     * @serial el Error envuelto
     */
    private java.lang.Error error;

    public RuntimeErrorException(java.lang.Error e) {
        super();
        error = e;
    }

    public RuntimeErrorException(java.lang.Error e, String message) {
        super(message);
        error = e;
    }

    /** El `Error` envuelto. */
    public java.lang.Error getTargetError() {
        return error;
    }

    /** Lo mismo que {@link #getTargetError()}, por la via moderna. */
    public Throwable getCause() {
        return error;
    }
}
