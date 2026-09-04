package javax.management.remote;

import java.util.Iterator;
import java.util.Map;

/**
 * Lo que comparten las dos fabricas de este paquete.
 *
 * <p>De acceso de paquete: no es API. Existe para que la busqueda por nombre deducido y el control del
 * entorno esten escritos una sola vez, en lugar de duplicados en
 * {@link JMXConnectorFactory} y {@link JMXConnectorServerFactory}.
 */
final class FactorySupport {

    /** Los paquetes que se prueban si nadie dice otra cosa. */
    private static final String DEFAULT_PACKAGES = "com.sun.jmx.remote.protocol";

    private FactorySupport() {
    }

    /**
     * Comprueba que las claves del entorno sean cadenas.
     *
     * <p>El tipo {@code Map<String,?>} no alcanza: por borrado de tipos puede llegar un mapa con
     * claves de cualquier cosa, y el error saldria mucho despues.
     *
     * @throws IllegalArgumentException si alguna clave no es una cadena
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
     * Busca un proveedor por el nombre de clase que se deduce del protocolo.
     *
     * <p>Ver la nota de {@link JMXConnectorFactory} sobre la traduccion del protocolo a paquete.
     *
     * @param suffix {@code "ClientProvider"} o {@code "ServerProvider"}
     * @return el proveedor, o null si no hay ninguno con ese nombre
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

    /** Del entorno, de la propiedad del sistema, o los de siempre. */
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

    /** El del entorno, o el del contexto. */
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

    /** Carga esa clase y la instancia, o null si no esta o no sirve. */
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
