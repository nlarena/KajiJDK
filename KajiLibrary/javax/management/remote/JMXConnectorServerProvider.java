package javax.management.remote;

import java.io.IOException;
import java.util.Map;
import javax.management.MBeanServer;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorServerProvider -- sabe hacer servidores de un
 * protocolo.
 *
 * <p>El espejo de {@link JMXConnectorProvider} para el lado servidor. Se busca igual y lanza lo mismo;
 * ver ahi.
 */
public interface JMXConnectorServerProvider {

    /**
     * Un servidor sin arrancar para esa direccion.
     *
     * @param mbeanServer a que servidor de MBeans expone, o null para atarlo despues al registrarlo
     * @throws JMXProviderException si reconoce el protocolo y no puede con este entorno
     * @throws IOException si fallo por otra cosa
     */
    JMXConnectorServer newJMXConnectorServer(JMXServiceURL serviceURL, Map<String, ?> environment,
                                             MBeanServer mbeanServer) throws IOException;
}
