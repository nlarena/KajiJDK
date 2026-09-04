package javax.management;

/**
 * Envuelve un `RuntimeException` que tiró el MBean.
 *
 * <p>Es el gemelo no verificado de {@link MBeanException}: mismo significado --"tu MBean fallo"--
 * pero para lo que el MBean no declara. Envolverlo en vez de dejarlo pasar es lo que permite al
 * cliente saber de que lado de la frontera ocurrio.
 */
public class RuntimeMBeanException extends JMRuntimeException {

    private static final long serialVersionUID = 5274912751982730171L;

    /**
     * @serial el RuntimeException envuelto
     */
    private java.lang.RuntimeException runtimeException;

    public RuntimeMBeanException(java.lang.RuntimeException e) {
        super();
        runtimeException = e;
    }

    public RuntimeMBeanException(java.lang.RuntimeException e, String message) {
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
