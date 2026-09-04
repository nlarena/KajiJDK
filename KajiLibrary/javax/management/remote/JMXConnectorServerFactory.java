package javax.management.remote;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import javax.management.MBeanServer;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorServerFactory -- consigue un servidor de
 * conectores.
 *
 * <p>El espejo de {@link JMXConnectorFactory} para el lado servidor, con un metodo solo. El servidor
 * que devuelve <b>no esta arrancado</b>: hay que llamarle {@code start()}, y eso es a proposito, para
 * poder registrarlo como MBean o encadenarle interceptores antes de abrir el puerto.

 * <h2>Como se encuentra el proveedor de un protocolo</h2>
 *
 * <p>Se prueban dos caminos, en orden:
 *
 * <ol>
 *   <li>los declarados como servicio y encontrados con {@link java.util.ServiceLoader}. Es la forma
 *       moderna y la que no pide configuracion;
 *   <li>por <b>nombre de clase deducido</b>: para cada paquete de la propiedad
 *       {@link #PROTOCOL_PROVIDER_PACKAGES} se busca la clase
 *       {@code <paquete>.<protocolo>.ServerProvider}. Los paquetes se separan con {@code |}, y el
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
public class JMXConnectorServerFactory {

    /** Clave del entorno: con que cargador de clases deserializar lo que llega. */
    public static final String DEFAULT_CLASS_LOADER = "jmx.remote.default.class.loader";

    /**
     * Clave del entorno: el {@code ObjectName} del MBean cargador de clases a usar.
     *
     * <p>Es la alternativa a {@link #DEFAULT_CLASS_LOADER} y son excluyentes: uno da el cargador,
     * el otro lo nombra para que lo busque en el servidor de MBeans. Dar los dos es un error.
     */
    public static final String DEFAULT_CLASS_LOADER_NAME = "jmx.remote.default.class.loader.name";

    /** Propiedad y clave del entorno: en que paquetes buscar proveedores, separados por {@code |}. */
    public static final String PROTOCOL_PROVIDER_PACKAGES = "jmx.remote.protocol.provider.pkgs";

    /** Clave del entorno: con que cargador buscar la clase del proveedor. */
    public static final String PROTOCOL_PROVIDER_CLASS_LOADER =
        "jmx.remote.protocol.provider.class.loader";

    /** No tiene estado; el constructor publico es el que el JDK dejo. */
    public JMXConnectorServerFactory() {
    }

    /**
     * Un servidor sin arrancar para esa direccion.
     *
     * @param mbeanServer a que servidor de MBeans expone, o null para atarlo al registrarlo
     * @throws MalformedURLException si no hay proveedor para ese protocolo
     * @throws JMXProviderException si lo hay y no pudo
     * @throws IOException si fallo por otra cosa
     */
    public static JMXConnectorServer newJMXConnectorServer(JMXServiceURL serviceURL,
                                                           Map<String, ?> environment,
                                                           MBeanServer mbeanServer)
        throws IOException {
        if (serviceURL == null) {
            throw new NullPointerException("Null JMXServiceURL");
        }
        Map<String, Object> env;
        if (environment == null) {
            env = new HashMap<String, Object>();
        } else {
            FactorySupport.checkKeys(environment);
            env = new HashMap<String, Object>(environment);
        }
        String protocol = serviceURL.getProtocol();
        Iterator<JMXConnectorServerProvider> loaded =
            ServiceLoader.load(JMXConnectorServerProvider.class).iterator();
        while (hasNextQuietly(loaded)) {
            JMXConnectorServerProvider p = loaded.next();
            JMXConnectorServer made = p.newJMXConnectorServer(serviceURL, env, mbeanServer);
            if (made != null) {
                return made;
            }
        }
        JMXConnectorServerProvider named =
            FactorySupport.byName(env, protocol, "ServerProvider",
                                  JMXConnectorServerProvider.class);
        if (named != null) {
            JMXConnectorServer made = named.newJMXConnectorServer(serviceURL, env, mbeanServer);
            if (made != null) {
                return made;
            }
        }
        throw new MalformedURLException("Unsupported protocol: " + protocol);
    }

    /** Ver {@link JMXConnectorFactory}: un proveedor roto no tumba la busqueda. */
    private static boolean hasNextQuietly(Iterator<JMXConnectorServerProvider> it) {
        try {
            return it.hasNext();
        } catch (Throwable e) {
            return false;
        }
    }
}
