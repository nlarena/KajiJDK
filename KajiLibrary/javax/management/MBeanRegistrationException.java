package javax.management;

/**
 * The MBean objected to its own registration (or unregistration) from {@link MBeanRegistration}.
 *
 * <p>It extends {@link MBeanException} and not {@link OperationsException} because whoever failed
 * is the <b>MBean</b>, in its {@code preRegister}/{@code preDeregister} code, not the agent.
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
