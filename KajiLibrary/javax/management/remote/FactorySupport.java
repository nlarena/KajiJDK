package javax.management.remote;

import java.util.Iterator;
import java.util.Map;

/**
 * What the two factories of this package share.
 *
 * <p>Package access: it is not API. It exists so that the search by deduced name and the
 * environment check are written only once, instead of duplicated in
 * {@link JMXConnectorFactory} and {@link JMXConnectorServerFactory}.
 */
final class FactorySupport {

    /** The packages that are tried if nobody says otherwise. */
    private static final String DEFAULT_PACKAGES = "com.sun.jmx.remote.protocol";

    private FactorySupport() {
    }

    /**
     * Checks that the environment's keys are strings.
     *
     * <p>The {@code Map<String,?>} type is not enough: through erasure a map with keys of anything
     * may arrive, and the error would come out much later.
     *
     * @throws IllegalArgumentException if some key is not a string
     */
    static void checkKeys(Map<?, ?> env) {
        Iterator<?> it = env.keySet().iterator();
        while (it.hasNext()) {
            Object k = it.next();
            if (!(k instanceof String)) {
                throw new IllegalArgumentException("Environment contains non-string key");
            }
        }
    }

    /**
     * Looks for a provider by the class name deduced from the protocol.
     *
     * <p>See {@link JMXConnectorFactory}'s note on the translation from protocol to package.
     *
     * @param suffix {@code "ClientProvider"} or {@code "ServerProvider"}
     * @return the provider, or null if there is none with that name
     */
    static <T> T byName(Map<String, Object> env, String protocol, String suffix, Class<T> type) {
        String packages = packagesFrom(env);
        ClassLoader loader = loaderFrom(env);
        String pkgProtocol = protocol.replace('+', '.').replace('-', '_');
        int start = 0;
        while (start <= packages.length()) {
            int bar = packages.indexOf('|', start);
            int end;
            if (bar < 0) {
                end = packages.length();
            } else {
                end = bar;
            }
            String pkg = packages.substring(start, end).trim();
            if (pkg.length() > 0) {
                T found = tryClass(pkg + "." + pkgProtocol + "." + suffix, loader, type);
                if (found != null) {
                    return found;
                }
            }
            if (bar < 0) {
                return null;
            }
            start = bar + 1;
        }
        return null;
    }

    /** From the environment, from the system property, or the usual ones. */
    private static String packagesFrom(Map<String, Object> env) {
        Object v = env.get(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES);
        if (v instanceof String) {
            return (String) v;
        }
        String prop;
        try {
            prop = System.getProperty(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES);
        } catch (Throwable e) {
            prop = null;
        }
        if (prop != null) {
            return prop;
        }
        return DEFAULT_PACKAGES;
    }

    /** The environment's, or the context's. */
    private static ClassLoader loaderFrom(Map<String, Object> env) {
        Object v = env.get(JMXConnectorFactory.PROTOCOL_PROVIDER_CLASS_LOADER);
        if (v instanceof ClassLoader) {
            return (ClassLoader) v;
        }
        try {
            return Thread.currentThread().getContextClassLoader();
        } catch (Throwable e) {
            return null;
        }
    }

    /** Loads that class and instantiates it, or null if it is not there or does not serve. */
    private static <T> T tryClass(String name, ClassLoader loader, Class<T> type) {
        Class<?> c;
        try {
            c = Class.forName(name, true, loader);
        } catch (Throwable e) {
            return null;
        }
        if (!type.isAssignableFrom(c)) {
            return null;
        }
        try {
            return type.cast(c.getDeclaredConstructor().newInstance());
        } catch (Throwable e) {
            return null;
        }
    }
}
