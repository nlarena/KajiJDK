package java.rmi.server;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * La carga de clases de RMI, en su forma vieja.
 *
 * @deprecated reemplazado por {@link RMIClassLoader} y su {@link RMIClassLoaderSpi}. Nunca fue algo
 *     que el codigo de usuario tuviera que implementar.
 */
@Deprecated(since = "1.2")
public interface LoaderHandler {

    /** El paquete donde vive la implementacion. */
    static final String packagePrefix = "sun.rmi.server";

    /** Carga una clase desde el codebase por omision. */
    Class<?> loadClass(String name) throws MalformedURLException, ClassNotFoundException;

    /** Carga una clase desde ese codebase. */
    Class<?> loadClass(URL codebase, String name)
            throws MalformedURLException, ClassNotFoundException;

    /** El contexto de seguridad de ese cargador. */
    Object getSecurityContext(ClassLoader loader);
}
