package javax.net.ssl;

import javax.net.ServerSocketFactory;

/**
 * Factory of {@link SSLServerSocket}s.
 *
 * <p>The server-side mirror of {@link SSLSocketFactory}, with the same purpose: that the code that
 * opens the port does not have to know whether what it accepts goes encrypted.
 *
 * <p>Without a TLS provider installed, {@link #getDefault} returns a factory that fails when used
 * instead of {@code null} — see the note of {@link SSLSocketFactory}.
 */
public abstract class SSLServerSocketFactory extends ServerSocketFactory {

    private static SSLServerSocketFactory theDefault;

    protected SSLServerSocketFactory() {
    }

    /** The default factory, taken from the default {@link SSLContext}. */
    public static synchronized ServerSocketFactory getDefault() {
        if (theDefault == null) {
            try {
                theDefault =
                        (SSLServerSocketFactory) SSLContext.getDefault().getServerSocketFactory();
            } catch (Exception e) {
                theDefault = new DefaultSSLServerSocketFactory(e);
            }
        }
        return theDefault;
    }

    /** The suites enabled by default in what it makes. */
    public abstract String[] getDefaultCipherSuites();

    /** All the suites that could be enabled. */
    public abstract String[] getSupportedCipherSuites();
}
