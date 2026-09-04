package java.rmi.server;

import java.net.MalformedURLException;

/**
 * El punto de extension detras de {@link RMIClassLoader}.
 *
 * <p>Existe porque de donde se bajan las clases es una decision de despliegue y no del programa: un
 * entorno puede querer resolver el codebase contra un repositorio propio, o prohibirlo del todo. Sin
 * este SPI habria que elegir entre la politica del JDK y ninguna.
 */
public abstract class RMIClassLoaderSpi {

    public RMIClassLoaderSpi() {
    }

    /** Carga una clase desde ese codebase. */
    public abstract Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException;

    /** Carga un proxy que implementa esas interfaces. */
    public abstract Class<?> loadProxyClass(String codebase, String[] interfaces,
            ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException;

    /** El cargador para ese codebase. */
    public abstract ClassLoader getClassLoader(String codebase) throws MalformedURLException;

    /** El codebase que se anuncia junto con esa clase al serializarla. */
    public abstract String getClassAnnotation(Class<?> cl);
}
