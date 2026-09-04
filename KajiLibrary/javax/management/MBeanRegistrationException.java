package javax.management;

/**
 * El MBean se opuso a su propio registro (o a su baja) desde {@link MBeanRegistration}.
 *
 * <p>Hereda de {@link MBeanException} y no de {@link OperationsException} porque quien fallo es el
 * **MBean**, en su codigo de `preRegister`/`preDeregister`, no el agente.
 */
public class MBeanRegistrationException extends MBeanException {

    private static final long serialVersionUID = 4482382455277067805L;

    public MBeanRegistrationException(java.lang.Exception e) {
        super(e);
    }

    public MBeanRegistrationException(java.lang.Exception e, String message) {
        super(e, message);
    }
}
