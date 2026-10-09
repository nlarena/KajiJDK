package javax.management.remote;

import java.io.IOException;
import java.util.Map;
import javax.management.MBeanServer;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorServerProvider -- knows how to make servers of
 * a protocol.
 *
 * <p>{@link JMXConnectorProvider}'s mirror for the server side. It is looked for in the same way
 * and throws the same things; see there.
 */
public interface JMXConnectorServerProvider {

    /**
     * An unstarted server for that address.
     *
     * @param mbeanServer which MBean server it exposes, or null to tie it later when registering it
     * @throws JMXProviderException if it recognizes the protocol and cannot cope with this
     *     environment
     * @throws IOException if it failed for something else
     */
    JMXConnectorServer newJMXConnectorServer(JMXServiceURL serviceURL, Map<String, ?> environment,
                                             MBeanServer mbeanServer) throws IOException;
}
