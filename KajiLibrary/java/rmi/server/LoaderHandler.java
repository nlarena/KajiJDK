package java.rmi.server;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * RMI's class loading, in its old form.
 *
 * @deprecated replaced by {@link RMIClassLoader} and its {@link RMIClassLoaderSpi}. It was never
 *     something user code had to implement.
 */
@Deprecated(since = "1.2")
public interface LoaderHandler {

    /** The package the implementation lives in. */
    static final String packagePrefix = "sun.rmi.server";

    /** It loads a class from the default codebase. */
    Class<?> loadClass(String name) throws MalformedURLException, ClassNotFoundException;

    /** It loads a class from that codebase. */
    Class<?> loadClass(URL codebase, String name)
            throws MalformedURLException, ClassNotFoundException;

    /** That loader's security context. */
    Object getSecurityContext(ClassLoader loader);
}
