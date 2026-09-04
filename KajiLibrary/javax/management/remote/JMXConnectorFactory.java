package javax.management.remote;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorFactory -- consigue un conector cliente.
 *
 * <p>El punto de entrada del lado cliente. {@link #connect} es el atajo normal;
 * {@link #newJMXConnector} devuelve el conector <b>sin conectar</b>, que es lo que hace falta para
 * registrar escuchas antes de que pase nada -- ver {@link JMXConnector}.

 * <h2>Como se encuentra el proveedor de un protocolo</h2>
 *
 * <p>Se prueban dos caminos, en orden:
 *
 * <ol>
 *   <li>los declarados como servicio y encontrados con {@link java.util.ServiceLoader}. Es la forma
 *       moderna y la que no pide configuracion;
 *   <li>por <b>nombre de clase deducido</b>: para cada paquete de la propiedad
 *       {@link #PROTOCOL_PROVIDER_PACKAGES} se busca la clase
 *       {@code <paquete>.<protocolo>.ClientProvider}. Los paquetes se separan con {@code |}, y el
 *       protocolo se traduce cambiando {@code +} por punto y {@code -} por raya baja, porque un
 *       protocolo puede tener caracteres que un nombre de paquete no admite.
 * </ol>
 *
 * <p>Si un proveedor reconoce el protocolo pero no puede con ese entorno, lanza
 * {@link JMXProviderException} y se sigue con el siguiente. Si ninguno lo reconoce, sale
 * {@link java.net.MalformedURLException} con {@code "Unsupported protocol"}.
 *
 * <p>Esa distincion es la que le sirve a quien llama: la primera dice "esta roto", la segunda dice "no
 * existe". Ver {@link JMXProviderException}.

 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae ningun protocolo. RMI necesita una capa de transporte remota que no esta,
 * y JMXMP nunca estuvo en el JDK. La busqueda esta implementada de verdad --recorre el
 * {@link java.util.ServiceLoader} y prueba los nombres deducidos-- y termina en
 * {@code "Unsupported protocol"}, que es exactamente lo que hace el JDK 25 con un protocolo que nadie
 * provee. Agregando un proveedor, esto funciona sin cambios.
 */
public class JMXConnectorFactory {

    /** Clave del entorno: con que cargador de clases deserializar lo que llega. */
    public static final String DEFAULT_CLASS_LOADER = "jmx.remote.default.class.loader";

    /** Propiedad y clave del entorno: en que paquetes buscar proveedores, separados por {@code |}. */
    public static final String PROTOCOL_PROVIDER_PACKAGES = "jmx.remote.protocol.provider.pkgs";

    /** Clave del entorno: con que cargador buscar la clase del proveedor. */
    public static final String PROTOCOL_PROVIDER_CLASS_LOADER =
        "jmx.remote.protocol.provider.class.loader";

    /** Los paquetes que se prueban si no se dice otra cosa. */
    private static final String DEFAULT_PACKAGES = "com.sun.jmx.remote.protocol";

    /** No tiene estado; el constructor publico es el que el JDK dejo. */
    public JMXConnectorFactory() {
    }

    /**
     * Crea un conector y lo conecta.
     *
     * @throws MalformedURLException si no hay proveedor para ese protocolo
     * @throws IOException si no se pudo conectar
     * @throws NullPointerException si la direccion es null
     */
    public static JMXConnector connect(JMXServiceURL serviceURL) throws IOException {
        return connect(serviceURL, null);
    }

    /**
     * Idem, con entorno.
     *
     * @throws MalformedURLException si no hay proveedor para ese protocolo
     * @throws IOException si no se pudo conectar
     */
    public static JMXConnector connect(JMXServiceURL serviceURL, Map<String, ?> environment)
        throws IOException {
        if (serviceURL == null) {
            throw new NullPointerException("Null JMXServiceURL");
        }
        JMXConnector conn = newJMXConnector(serviceURL, environment);
        conn.connect(environment);
        return conn;
    }

    /**
     * Crea un conector sin conectarlo. Ver la nota de la clase.
     *
     * @throws MalformedURLException si no hay proveedor para ese protocolo
     * @throws JMXProviderException si lo hay y no pudo
     * @throws IOException si fallo por otra cosa
     */
    public static JMXConnector newJMXConnector(JMXServiceURL serviceURL,
                                               Map<String, ?> environment) throws IOException {
        if (serviceURL == null) {
            throw new NullPointerException("Null JMXServiceURL");
        }
        Map<String, Object> env = copyEnvironment(environment);
        String protocol = serviceURL.getProtocol();
        Iterator<JMXConnectorProvider> loaded =
            ServiceLoader.load(JMXConnectorProvider.class).iterator();
        while (hasNextQuietly(loaded)) {
            JMXConnectorProvider p = loaded.next();
            JMXConnector made = p.newJMXConnector(serviceURL, env);
            if (made != null) {
                return made;
            }
        }
        JMXConnectorProvider named = FactorySupport.byName(env, protocol, "ClientProvider",
                                                           JMXConnectorProvider.class);
        if (named != null) {
            JMXConnector made = named.newJMXConnector(serviceURL, env);
            if (made != null) {
                return made;
            }
        }
        throw new MalformedURLException("Unsupported protocol: " + protocol);
    }

    /** Una copia mutable, comprobando que las claves sean cadenas. */
    private static Map<String, Object> copyEnvironment(Map<String, ?> env) {
        if (env == null) {
            return new HashMap<String, Object>();
        }
        FactorySupport.checkKeys(env);
        return new HashMap<String, Object>(env);
    }

    /** Un proveedor roto no puede tumbar la busqueda; los que siguen todavia pueden servir. */
    private static boolean hasNextQuietly(Iterator<JMXConnectorProvider> it) {
        try {
            return it.hasNext();
        } catch (Throwable e) {
            return false;
        }
    }
}
