package javax.management.remote;

import java.io.IOException;
import java.util.Map;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorServerMBean -- la cara de MBean de un servidor de
 * conectores.
 *
 * <p>Existe para que el servidor se pueda <b>registrar en el propio servidor de MBeans que expone</b>.
 * Suena circular y es util: asi se lo puede arrancar, parar y consultar por las mismas vias que
 * cualquier otro MBean, incluso desde una consola remota conectada a traves de el.
 *
 * <p>Es una interfaz de MBean estandar: el nombre termina en {@code MBean} y la clase que lo cumple es
 * {@link JMXConnectorServer}, sin el sufijo. Esa convencion es lo que hace que el registro funcione.
 */
public interface JMXConnectorServerMBean {

    /**
     * Empieza a escuchar.
     *
     * @throws IOException si no se pudo
     * @throws IllegalStateException si ya se paro; un servidor parado no se reinicia
     */
    void start() throws IOException;

    /**
     * Deja de escuchar y cierra las conexiones abiertas.
     *
     * <p>Es definitivo: despues de esto, {@link #start} falla.
     *
     * @throws IOException si algo fallo al cerrar
     */
    void stop() throws IOException;

    /** Si esta escuchando. */
    boolean isActive();

    /** Encadena un interceptor delante del servidor de MBeans. Ver {@link MBeanServerForwarder}. */
    void setMBeanServerForwarder(MBeanServerForwarder mbsf);

    /** Los identificadores de las conexiones abiertas. */
    String[] getConnectionIds();

    /** La direccion donde escucha, o null si no arranco. */
    JMXServiceURL getAddress();

    /** El entorno con el que se creo, de solo lectura. */
    Map<String, ?> getAttributes();

    /**
     * Un conector cliente hacia este mismo servidor.
     *
     * <p>Es lo que permite que algo del mismo proceso hable con el servidor por el camino remoto, sin
     * atajos. Sirve para probar.
     *
     * @throws UnsupportedOperationException si este servidor no lo soporta
     * @throws IllegalStateException si no esta activo
     * @throws IOException si no se pudo
     */
    JMXConnector toJMXConnector(Map<String, ?> env) throws IOException;
}
