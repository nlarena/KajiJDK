package com.sun.tools.jconsole;

import java.beans.PropertyChangeListener;
import javax.management.MBeanServerConnection;

/**
 * La conexion de jconsole con **una** aplicacion vigilada.
 *
 * <p>Es lo que un complemento recibe para hacer su trabajo: por
 * {@link #getMBeanServerConnection()} llega a los MBean de la aplicacion, que es de donde sale
 * todo lo que jconsole muestra.
 *
 * <p>El estado es una propiedad ligada --{@link #CONNECTION_STATE_PROPERTY}-- y no un dato que se
 * consulta cada tanto, porque una conexion JMX se cae sola: la aplicacion vigilada puede terminar,
 * o la red cortarse, en cualquier momento y sin aviso. Un complemento que no escuche ese cambio se
 * queda dibujando datos viejos.
 *
 * <p>La implementa jconsole, no el complemento.
 */
public interface JConsoleContext {

    /** El nombre de la propiedad ligada del estado de la conexion. */
    String CONNECTION_STATE_PROPERTY = "connectionState";

    /**
     * La conexion con el servidor de MBean de la aplicacion vigilada.
     *
     * <p>Puede estar caida: comprobar {@link #getConnectionState()} antes de usarla.
     */
    MBeanServerConnection getMBeanServerConnection();

    /** En que estado esta la conexion. */
    ConnectionState getConnectionState();

    /**
     * Agrega un escucha de las propiedades de esta conexion.
     *
     * <p>Un complemento no suele llamarlo directamente: para eso esta
     * {@link JConsolePlugin#addContextPropertyChangeListener}, que ademas sobrevive a un cambio de
     * contexto.
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /** Saca un escucha. */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /** En que estado esta una conexion de jconsole. */
    enum ConnectionState {

        /** Conectada: los MBean se pueden consultar. */
        CONNECTED,

        /** Caida. Puede volver: jconsole reintenta. */
        DISCONNECTED,

        /** Conectando. Es un estado propio y no un `DISCONNECTED` porque dura: la conexion inicial
         * a una VM remota puede tardar, y la interfaz tiene que poder decir "esperando" en vez de
         * "no hay". */
        CONNECTING
    }
}
