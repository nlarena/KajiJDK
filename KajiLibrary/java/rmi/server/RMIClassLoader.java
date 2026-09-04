package java.rmi.server;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * De donde salen las clases que llegan por la red.
 *
 * <h2>El codebase, y por que esto es delicado</h2>
 *
 * <p>Cuando un objeto serializado llega y su clase no esta localmente, RMI puede
 * <strong>bajarla</strong> de la URL que el remitente anuncio. Eso es lo que hace posible mandar una
 * implementacion que el receptor no conocia — y es tambien ejecutar codigo que eligio otro.
 *
 * <p>Por eso la carga remota esta apagada salvo que se la habilite explicitamente, y por eso existe
 * {@link RMIClassLoaderSpi}: la politica es del entorno, no de la biblioteca.
 *
 * <p>En esta VM no hay proveedor instalado, asi que los metodos que bajarian codigo declinan hacerlo
 * y los que resuelven localmente funcionan.
 */
public class RMIClassLoader {

    private RMIClassLoader() {
    }

    /**
     * @deprecated usar {@link #loadClass(String, String)}, que dice de donde
     */
    @Deprecated(since = "1.2")
    public static Class<?> loadClass(String name)
            throws MalformedURLException, ClassNotFoundException {
        return Class.forName(name, false, ClassLoader.getSystemClassLoader());
    }

    /** @deprecated usar {@link #loadClass(String, String)} */
    @Deprecated(since = "1.2")
    public static Class<?> loadClass(URL codebase, String name)
            throws MalformedURLException, ClassNotFoundException {
        return loadClass(codebase == null ? null : codebase.toString(), name, null);
    }

    /** Carga la clase, bajandola del codebase si hace falta y esta permitido. */
    public static Class<?> loadClass(String codebase, String name)
            throws MalformedURLException, ClassNotFoundException {
        return loadClass(codebase, name, null);
    }

    /**
     * Igual, probando primero con ese cargador.
     *
     * <p>Sin proveedor instalado se resuelve solo localmente: un codebase remoto no se baja.
     */
    public static Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException {
        ClassLoader cl = defaultLoader == null ? ClassLoader.getSystemClassLoader() : defaultLoader;
        return Class.forName(name, false, cl);
    }

    /** Carga un proxy que implementa esas interfaces. */
    public static Class<?> loadProxyClass(String codebase, String[] interfaces,
            ClassLoader defaultLoader) throws ClassNotFoundException, MalformedURLException {
        ClassLoader cl = defaultLoader == null ? ClassLoader.getSystemClassLoader() : defaultLoader;
        Class<?>[] ifaces = new Class<?>[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            ifaces[i] = Class.forName(interfaces[i], false, cl);
        }
        return java.lang.reflect.Proxy.getProxyClass(cl, ifaces);
    }

    /** El cargador para ese codebase. */
    public static ClassLoader getClassLoader(String codebase)
            throws MalformedURLException, SecurityException {
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * El codebase que se anuncia junto con esa clase al serializarla.
     *
     * <p>{@code null} significa "no anuncio ninguno", que es lo correcto cuando no hay de donde
     * bajarla: anunciar una URL que no sirve haria que el receptor la intente y falle mas tarde.
     */
    public static String getClassAnnotation(Class<?> cl) {
        return null;
    }

    /**
     * El proveedor por omision.
     *
     * @throws UnsupportedOperationException en esta VM: no hay proveedor de carga remota
     */
    public static RMIClassLoaderSpi getDefaultProviderInstance() {
        throw new UnsupportedOperationException(
                "esta VM no trae proveedor de carga de clases remota");
    }

    /** @deprecated sin reemplazo; era parte del modelo de seguridad viejo */
    @Deprecated(since = "1.2")
    public static Object getSecurityContext(ClassLoader loader) {
        return null;
    }
}
