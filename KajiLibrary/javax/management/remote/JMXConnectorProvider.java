package javax.management.remote;

import java.io.IOException;
import java.util.Map;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorProvider -- sabe hacer conectores de un protocolo.
 *
 * <p>Lo que implementa quien agrega un protocolo nuevo. {@link JMXConnectorFactory} lo encuentra por
 * {@link java.util.ServiceLoader}, o por el nombre de clase derivado del protocolo; ver ahi las dos
 * formas.
 *
 * <p>Un proveedor que reconoce el protocolo pero no puede con <b>ese</b> entorno lanza
 * {@link JMXProviderException}, y la fabrica sigue probando con los demas. Devolver null no esta
 * permitido.
 */
public interface JMXConnectorProvider {

    /**
     * Un conector sin conectar para esa direccion.
     *
     * @throws JMXProviderException si reconoce el protocolo y no puede con este entorno
     * @throws IOException si fallo por otra cosa
     */
    JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment)
        throws IOException;
}
