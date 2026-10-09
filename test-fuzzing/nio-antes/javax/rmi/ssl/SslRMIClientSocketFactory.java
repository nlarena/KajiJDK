package javax.rmi.ssl;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.util.StringTokenizer;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * The factory that makes the client side of an RMI call travel over SSL.
 *
 * <p>An RMI stub carries inside it the socket factory it has to be contacted with, and the factory
 * travels **serialised** from the server to the client. Hence the two oddities of this class, which
 * otherwise make no sense:
 *
 * <ul>
 *   <li>It has no state. Everything it configures --the enabled suites and protocols-- comes from
 *       system properties that are read **on the client**, not from fields that would travel with
 *       the object. It is deliberate: the client's SSL configuration is chosen by the client.
 *   <li>{@link #equals} compares the **class** and not the contents. Two instances with no state
 *       are interchangeable, and RMI uses that equality to reuse a single connection with several
 *       stubs of the same server; if it compared by identity, each stub would open its own.
 * </ul>
 *
 * <p>The two properties it reads, separated by commas:
 * {@code javax.rmi.ssl.client.enabledCipherSuites} and
 * {@code javax.rmi.ssl.client.enabledProtocols}. If they are not there, the socket is left as its
 * factory left it.
 */
public class SslRMIClientSocketFactory implements RMIClientSocketFactory, Serializable {

    private static SocketFactory defaultSocketFactory = null;

    private static final long serialVersionUID = -8310631444933958385L;

    /** A new factory. */
    public SslRMIClientSocketFactory() {
    }

    /**
     * An SSL socket connected to that machine and port.
     *
     * @throws IOException if it cannot connect, or if either of the two properties names a suite or
     *     a protocol the socket does not support
     */
    public Socket createSocket(String host, int port) throws IOException {
        SocketFactory factory = getDefaultClientSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

        String[] suites = readList("javax.rmi.ssl.client.enabledCipherSuites");
        if (suites != null) {
            try {
                socket.setEnabledCipherSuites(suites);
            } catch (IllegalArgumentException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        String[] protocols = readList("javax.rmi.ssl.client.enabledProtocols");
        if (protocols != null) {
            try {
                socket.setEnabledProtocols(protocols);
            } catch (IllegalArgumentException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return socket;
    }

    /** The property, split by commas, or `null` if it is not there. */
    private static String[] readList(String property) {
        String value = System.getProperty(property);
        if (value == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(value, ",");
        int n = st.countTokens();
        String[] out = new String[n];
        for (int i = 0; i < n; i++) {
            out[i] = st.nextToken();
        }
        return out;
    }

    /**
     * Two factories of this class are equal.
     *
     * <p>It compares the exact class and not `instanceof`, so that a subclass that does have state
     * does not come out equal to its base.
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return obj.getClass().equals(this.getClass());
    }

    /** Coherent with {@link #equals}: it depends only on the class. */
    public int hashCode() {
        return this.getClass().hashCode();
    }

    /**
     * The usual SSL factory, memoised.
     *
     * <p>It is memoised because `SSLSocketFactory.getDefault()` may have to build a whole context,
     * and this is called once per connection.
     */
    private static synchronized SocketFactory getDefaultClientSocketFactory() {
        if (defaultSocketFactory == null) {
            defaultSocketFactory = SSLSocketFactory.getDefault();
        }
        return defaultSocketFactory;
    }
}
