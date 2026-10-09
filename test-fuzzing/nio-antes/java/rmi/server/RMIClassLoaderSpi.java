package java.rmi.server;

import java.net.MalformedURLException;

/**
 * The extension point behind {@link RMIClassLoader}.
 *
 * <p>It exists because where classes are downloaded from is a deployment decision, not the
 * program's: an environment may want to resolve the codebase against a repository of its own, or
 * forbid it altogether. Without this SPI the choice would be between the JDK's policy and none.
 */
public abstract class RMIClassLoaderSpi {

    public RMIClassLoaderSpi() {
    }

    /** It loads a class from that codebase. */
    public abstract Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException;

    /** It loads a proxy that implements those interfaces. */
    public abstract Class<?> loadProxyClass(String codebase, String[] interfaces,
            ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException;

    /** The loader for that codebase. */
    public abstract ClassLoader getClassLoader(String codebase) throws MalformedURLException;

    /** The codebase announced along with that class when it is serialised. */
    public abstract String getClassAnnotation(Class<?> cl);
}
