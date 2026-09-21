package java.rmi.server;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Where the classes that arrive over the network come from.
 *
 * <h2>The codebase, and why this is delicate</h2>
 *
 * <p>When a serialised object arrives and its class is not present locally, RMI can
 * <strong>download it</strong> from the URL the sender announced. That is what makes it possible to
 * send an implementation the receiver did not know — and it is also running code somebody else
 * chose.
 *
 * <p>That is why, in RMI, remote loading is off unless it is enabled explicitly, and why
 * {@link RMIClassLoaderSpi} exists: the policy belongs to the environment, not to the library.
 * This note used to read as if this VM had such a switch; it has none. Nothing in KajiLibrary reads
 * a codebase or provider property (a grep for `useCodebaseOnly`, `java.rmi.server.codebase` and
 * `java.rmi.server.RMIClassLoaderSpi` finds no lookup), so remote loading cannot be enabled here.
 *
 * <p>In this VM there is no provider installed, so the methods that would download code decline to
 * do so and the ones that resolve locally work.
 */
public class RMIClassLoader {

    private RMIClassLoader() {
    }

    /**
     * @deprecated use {@link #loadClass(String, String)}, which says where from
     */
    @Deprecated(since = "1.2")
    public static Class<?> loadClass(String name)
            throws MalformedURLException, ClassNotFoundException {
        return Class.forName(name, false, ClassLoader.getSystemClassLoader());
    }

    /** @deprecated use {@link #loadClass(String, String)} */
    @Deprecated(since = "1.2")
    public static Class<?> loadClass(URL codebase, String name)
            throws MalformedURLException, ClassNotFoundException {
        return loadClass(codebase == null ? null : codebase.toString(), name, null);
    }

    /** It loads the class, downloading it from the codebase if needed and allowed. */
    public static Class<?> loadClass(String codebase, String name)
            throws MalformedURLException, ClassNotFoundException {
        return loadClass(codebase, name, null);
    }

    /**
     * The same, trying that loader first.
     *
     * <p>With no provider installed it resolves locally only: a remote codebase is not downloaded.
     */
    public static Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException {
        ClassLoader cl = defaultLoader == null ? ClassLoader.getSystemClassLoader() : defaultLoader;
        return Class.forName(name, false, cl);
    }

    /** It loads a proxy that implements those interfaces. */
    public static Class<?> loadProxyClass(String codebase, String[] interfaces,
            ClassLoader defaultLoader) throws ClassNotFoundException, MalformedURLException {
        ClassLoader cl = defaultLoader == null ? ClassLoader.getSystemClassLoader() : defaultLoader;
        Class<?>[] ifaces = new Class<?>[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            ifaces[i] = Class.forName(interfaces[i], false, cl);
        }
        return java.lang.reflect.Proxy.getProxyClass(cl, ifaces);
    }

    /** The loader for that codebase. */
    public static ClassLoader getClassLoader(String codebase)
            throws MalformedURLException, SecurityException {
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * The codebase announced along with that class when it is serialised.
     *
     * <p>{@code null} means "I announce none", which is the right thing when there is nowhere to
     * download it from: announcing a URL that does not work would make the receiver try it and fail
     * later.
     */
    public static String getClassAnnotation(Class<?> cl) {
        return null;
    }

    /**
     * The default provider.
     *
     * @throws UnsupportedOperationException in this VM: there is no remote loading provider
     */
    public static RMIClassLoaderSpi getDefaultProviderInstance() {
        throw new UnsupportedOperationException(
                "this VM ships no remote class loading provider");
    }

    /** @deprecated no replacement; it was part of the old security model */
    @Deprecated(since = "1.2")
    public static Object getSecurityContext(ClassLoader loader) {
        return null;
    }
}
