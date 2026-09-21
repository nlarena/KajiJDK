package javax.management.remote;

import java.io.IOException;
import java.util.Map;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorProvider -- knows how to make connectors of a
 * protocol.
 *
 * <p>What whoever adds a new protocol implements. {@link JMXConnectorFactory} finds it through
 * {@link java.util.ServiceLoader}, or by the class name derived from the protocol; see both ways
 * there.
 *
 * <p>A provider that recognizes the protocol but cannot cope with <b>that</b> environment throws
 * {@link JMXProviderException}, and the factory goes on trying the others. Returning null is not
 * allowed.
 */
public interface JMXConnectorProvider {

    /**
     * An unconnected connector for that address.
     *
     * @throws JMXProviderException if it recognizes the protocol and cannot cope with this
     *     environment
     * @throws IOException if it failed for something else
     */
    JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment)
        throws IOException;
}
