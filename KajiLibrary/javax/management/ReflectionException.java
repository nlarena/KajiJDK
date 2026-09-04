package javax.management;

/**
 * Envuelve una excepcion de `java.lang.reflect` que salio al construir o invocar por reflexion.
 *
 * <p>Se distingue de {@link MBeanException} justamente por donde se rompio: aca fallo el **acceso**
 * (no existe la clase, no existe el metodo, no se puede acceder), no el cuerpo del MBean. Un
 * `ClassNotFoundException` al crear un MBean es esto; un `IllegalStateException` tirado dentro de su
 * constructor es `MBeanException`.
 */
public class ReflectionException extends JMException {

    private static final long serialVersionUID = 9170809325636915553L;

    /**
     * @serial la excepcion de reflexion envuelta
     */
    private java.lang.Exception exception;

    public ReflectionException(java.lang.Exception e) {
        super();
        exception = e;
    }

    public ReflectionException(java.lang.Exception e, String message) {
        super(message);
        exception = e;
    }

    /** La excepcion de reflexion envuelta. */
    public java.lang.Exception getTargetException() {
        return exception;
    }

    /** Lo mismo que {@link #getTargetException()}, por la via moderna. */
    public Throwable getCause() {
        return exception;
    }
}
