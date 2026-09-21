package javax.net.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.net.SocketFactory;

/**
 * Factory of {@link SSLSocket}s.
 *
 * <h2>Why a factory and not a constructor</h2>
 *
 * <p>So that the code that opens connections does not know whether they are secure. A method that
 * receives a {@link SocketFactory} and calls {@code createSocket} serves TLS just as well as plain
 * text, and choosing which is a configuration decision somewhere else. That decoupling is the whole
 * reason {@code javax.net} exists.
 *
 * <h2>The {@code createSocket} that wraps another socket</h2>
 *
 * <p>{@link #createSocket(Socket, String, int, boolean)} opens nothing: it takes an already open
 * connection and puts TLS on top. It is what allows <strong>starting in the clear and encrypting
 * later</strong>, which is how {@code STARTTLS} and HTTP proxies with {@code CONNECT} work.
 *
 * <h2>Without a provider installed</h2>
 *
 * <p>{@link #getDefault} does not fail: it returns a factory whose {@code createSocket} methods
 * throw {@link SocketException}. It is exactly what the JDK does, and the reason is that this
 * signature cannot declare an exception — so the error is postponed until the moment somebody tries
 * to use it, and there it does have somewhere to come out.
 */
public abstract class SSLSocketFactory extends SocketFactory {

    private static SSLSocketFactory theDefault;

    public SSLSocketFactory() {
    }

    /**
     * The default factory.
     *
     * <p>It comes from the default {@link SSLContext}. Without a TLS provider installed --this VM's
     * case-- it returns one that fails when used; see the class note.
     */
    public static synchronized SocketFactory getDefault() {
        if (theDefault == null) {
            try {
                theDefault = (SSLSocketFactory) SSLContext.getDefault().getSocketFactory();
            } catch (Exception e) {
                theDefault = new DefaultSSLSocketFactory(e);
            }
        }
        return theDefault;
    }

    /** The suites enabled by default in what it makes. */
    public abstract String[] getDefaultCipherSuites();

    /** All the suites that could be enabled. */
    public abstract String[] getSupportedCipherSuites();

    /**
     * Puts TLS on an already open connection.
     *
     * @param s the existing connection
     * @param host the peer's name, to check it and for SNI
     * @param autoClose whether to close {@code s} when closing the returned socket
     */
    public abstract Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException;

    /**
     * The same, but first handing back some bytes that had already been read.
     *
     * <p>It solves a multiplexing problem: whoever looks at the first byte to decide whether the
     * connection is TLS already took it out of the stream, and the handshake needs to see it. This
     * method puts it back in front.
     */
    public Socket createSocket(Socket s, InputStream consumed, boolean autoClose)
            throws IOException {
        throw new UnsupportedOperationException(
                "this factory cannot re-inject bytes already consumed");
    }
}
