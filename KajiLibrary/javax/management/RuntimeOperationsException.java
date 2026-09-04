package javax.management;

/**
 * Envuelve un `RuntimeException` que tiró **el agente**, no el MBean.
 *
 * <p>El caso tipico es un argumento invalido: pasarle `null` donde el contrato pide un
 * {@link ObjectName} produce un `IllegalArgumentException` envuelto en esto. La diferencia con
 * {@link RuntimeMBeanException} es de autoria, no de tipo envuelto: los dos guardan un
 * `RuntimeException` y solo difieren en quien lo tiró.
 */
public class RuntimeOperationsException extends JMRuntimeException {

    private static final long serialVersionUID = -8408923047489133588L;

    /**
     * @serial el RuntimeException envuelto
     */
    private java.lang.RuntimeException runtimeException;

    public RuntimeOperationsException(java.lang.RuntimeException e) {
        super();
        runtimeException = e;
    }

    public RuntimeOperationsException(java.lang.RuntimeException e, String message) {
        super(message);
        runtimeException = e;
    }

    /** El `RuntimeException` envuelto. */
    public java.lang.RuntimeException getTargetException() {
        return runtimeException;
    }

    /** Lo mismo que {@link #getTargetException()}, por la via moderna. */
    public Throwable getCause() {
        return runtimeException;
    }
}
