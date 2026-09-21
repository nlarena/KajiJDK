package javax.management.remote;

import java.io.IOException;

/**
 * KajiLibrary's javax.management.remote.JMXProviderException -- there is a provider for that
 * protocol but it could not be used.
 *
 * <p>The distinction from {@code MalformedURLException} is what makes this class useful, and it
 * is subtle:
 *
 * <ul>
 *   <li>if there is <b>no</b> provider for the protocol, {@link JMXConnectorFactory} throws
 *       {@code MalformedURLException} with "Unsupported protocol";
 *   <li>if there <b>is</b> one and something went wrong --the class could not be loaded, it does
 *       not have the expected constructor, it failed while building itself-- it throws this one.
 * </ul>
 *
 * <p>The first means "you asked for something that does not exist"; this one means "it exists
 * and it is broken". A program that retries with another protocol should only do so with the
 * first.
 *
 * <p>It overrides {@link #getCause} because it is from 2003 and keeps the cause in a field of its
 * own.
 */
public class JMXProviderException extends IOException {

    private static final long serialVersionUID = -3166703627550447198L;

    /** The original one. */
    private Throwable cause = null;

    /** Without detail. */
    public JMXProviderException() {
    }

    /** With a message. */
    public JMXProviderException(String message) {
        super(message);
    }

    /** With a message and a cause. */
    public JMXProviderException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    /** The cause. */
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
