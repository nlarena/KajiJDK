package com.sun.net.httpserver.spi;

/**
 * The error of a provider named by a system property that could not be instantiated.
 *
 * <p>Package-private: it is a detail of {@link HttpServerProvider#provider()}. It exists so
 * that the failure carries <strong>the name that was asked for</strong> as well as the cause --
 * without that, a typo in the property produces a bare {@code ClassNotFoundException} that does
 * not say where the name came from.
 */
final class ServiceConfigurationErrorLocal extends Error {

    private static final long serialVersionUID = 8712374126493827162L;

    ServiceConfigurationErrorLocal(String name, Throwable cause) {
        super("the HttpServerProvider could not be instantiated '" + name + "'", cause);
    }
}
