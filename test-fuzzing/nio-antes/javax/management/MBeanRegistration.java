package javax.management;

/**
 * Implemented by the MBean that wants to know about its own registration.
 *
 * <p>It gives two powers that are not seen at first sight:
 *
 * <ul>
 *   <li>{@link #preRegister} <b>returns</b> an {@link ObjectName}. An MBean can choose its own
 *       name, and in fact it can be registered with {@code null} and let it name itself;
 *   <li>{@link #preRegister} can throw, and that <b>cancels</b> the registration. It is the only
 *       way an MBean has of refusing to exist in an agent that does not suit it.
 * </ul>
 *
 * <p>{@link #postRegister} receives a {@code Boolean} --not a {@code boolean}-- because it is also
 * called when the registration failed.
 */
public interface MBeanRegistration {

    /**
     * Before registering. Returns the final name.
     *
     * @throws Exception cancels the registration
     */
    ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception;

    /** After trying to register; {@code registrationDone} says whether it worked. */
    void postRegister(Boolean registrationDone);

    /**
     * Before unregistering.
     *
     * @throws Exception cancels the unregistration
     */
    void preDeregister() throws Exception;

    /** After unregistering. */
    void postDeregister();
}
