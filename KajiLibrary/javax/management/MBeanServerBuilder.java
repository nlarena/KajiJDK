package javax.management;

/**
 * The point where the agent's implementation is swapped.
 *
 * <p>It is an object and not a static method for a single reason, and it is the one that gives it
 * meaning: the factory does not instantiate it with {@code new MBeanServerBuilder()} but by loading
 * the class named by the {@code javax.management.builder.initial} property. Redefining this class
 * is how an agent of your own --one that audits, one that replicates-- is slipped into a program
 * that is already written, without touching the program.
 *
 * <p>The two methods are separate on purpose. A subclass that only wants to change the delegate's
 * data --the implementer's name, the version-- redefines {@code newMBeanServerDelegate} and leaves
 * the agent as it is.
 */
public class MBeanServerBuilder {

    public MBeanServerBuilder() {
    }

    /**
     * The delegate that will carry the {@code MBeanServerId} and emit registrations and
     * unregistrations.
     */
    public MBeanServerDelegate newMBeanServerDelegate() {
        return new MBeanServerDelegate();
    }

    /**
     * The agent.
     *
     * @param defaultDomain the domain used when an {@link ObjectName} does not bring one
     * @param outer the agent passed to the MBeans in {@code preRegister}, so that a wrapper can
     *        pose as the real agent. If it is {@code null}, the agent passes itself, which is the
     *        normal case
     * @param delegate the one {@link #newMBeanServerDelegate} returned
     */
    public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer,
                                      MBeanServerDelegate delegate) {
        return new LocalServer(defaultDomain, outer, delegate);
    }
}
