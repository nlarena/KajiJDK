package com.sun.net.httpserver.spi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.ServiceLoader;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;

/**
 * Who makes the servers; the extension point behind {@link HttpServer#create}.
 *
 * <h2>How it is chosen</h2>
 *
 * <p>Three places, in order: the system property
 * {@code com.sun.net.httpserver.HttpServerProvider}, then the providers
 * {@link ServiceLoader} finds, and last the platform's default provider.
 *
 * <p><strong>This VM does not bring the third</strong>, so with none registered
 * {@link #provider()} throws {@link UnsupportedOperationException} with the reason. It is not
 * a stub: the first two mechanisms work, and registering a provider makes the whole of
 * `com.sun.net.httpserver` work without touching a line of this.
 */
public abstract class HttpServerProvider {

    private static HttpServerProvider chosen;

    /** For the implementations. */
    protected HttpServerProvider() {
    }

    /** An HTTP server; {@code addr} may be {@code null} so as not to bind it yet. */
    public abstract HttpServer createHttpServer(InetSocketAddress addr, int backlog)
            throws IOException;

    /** An HTTPS server, which afterwards has to be given its TLS configurator. */
    public abstract HttpsServer createHttpsServer(InetSocketAddress addr, int backlog)
            throws IOException;

    /**
     * The provider to use, looked up only once.
     *
     * @throws UnsupportedOperationException if there is none -- see the class note
     */
    public static synchronized HttpServerProvider provider() {
        if (chosen != null) {
            return chosen;
        }
        String name = System.getProperty("com.sun.net.httpserver.HttpServerProvider");
        if (name != null) {
            try {
                Class<?> c = Class.forName(name, true, ClassLoader.getSystemClassLoader());
                chosen = (HttpServerProvider) c.getDeclaredConstructor().newInstance();
                return chosen;
            } catch (Exception e) {
                throw new ServiceConfigurationErrorLocal(name, e);
            }
        }
        Iterator<HttpServerProvider> it =
                ServiceLoader.load(HttpServerProvider.class).iterator();
        if (it.hasNext()) {
            chosen = it.next();
            return chosen;
        }
        throw new UnsupportedOperationException(
                "there is no HttpServerProvider: this VM does not bring the default provider");
    }
}
