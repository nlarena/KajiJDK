package javax.management.remote;

/**
 * KajiLibrary's javax.management.remote.JMXAddressable -- this has an address.
 *
 * <p>A single method. The connectors and servers that can say which {@link JMXServiceURL} they
 * correspond to implement it.
 *
 * <p>It is optional, and that is why it is a separate interface: there are connectors built over
 * an already open connection that have no address to give. It is asked with {@code instanceof}.
 */
public interface JMXAddressable {

    /** The address, or null if it is not known yet. */
    JMXServiceURL getAddress();
}
