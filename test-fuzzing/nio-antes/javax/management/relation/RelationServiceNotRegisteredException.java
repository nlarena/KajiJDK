package javax.management.relation;

/**
 * The relation service exists but is not registered in any MBean server.
 *
 * <p>It is a useful distinction: the object can be built and configured before registering it, but
 * almost no operation works until it is -- it needs the server to verify that the referenced MBeans
 * exist.
 */
public class RelationServiceNotRegisteredException extends RelationException {

    private static final long serialVersionUID = 8454744887157122910L;

    /** Without detail. */
    public RelationServiceNotRegisteredException() {
        super();
    }

    /** With a message. */
    public RelationServiceNotRegisteredException(String message) {
        super(message);
    }
}
