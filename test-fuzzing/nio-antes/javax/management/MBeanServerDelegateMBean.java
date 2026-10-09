package javax.management;

/**
 * The public face of the agent itself: the standard MBean that describes the MBean server.
 *
 * <p>It is always registered under {@code JMImplementation:type=MBeanServerDelegate} and it is also
 * the one that emits the registration and unregistration {@link MBeanServerNotification}s. That is:
 * the agent manages itself through the same channels it manages the others, without a separate
 * API.
 */
public interface MBeanServerDelegateMBean {

    /** Unique identifier of this agent instance. */
    String getMBeanServerId();

    /** The name of the specification followed: "Java Management Extensions". */
    String getSpecificationName();

    /** The version of the specification. */
    String getSpecificationVersion();

    /** Who published the specification. */
    String getSpecificationVendor();

    /** The name of this implementation. */
    String getImplementationName();

    /** Its version. */
    String getImplementationVersion();

    /** Its vendor. */
    String getImplementationVendor();
}
