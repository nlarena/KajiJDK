package javax.management.remote;

import javax.management.MBeanServer;

/**
 * KajiLibrary's javax.management.remote.MBeanServerForwarder -- un {@link MBeanServer} que envuelve a
 * otro.
 *
 * <p>Es un {@code MBeanServer} completo mas dos metodos para encadenarlo. Se pone entre el conector y
 * el servidor real, y ahi puede registrar cada operacion, filtrar por permisos, o cachear.
 *
 * <p>Se encadenan varios: cada uno apunta al siguiente y el ultimo al servidor de verdad. Se arma con
 * {@code JMXConnectorServer.setMBeanServerForwarder}, que va poniendo cada nuevo delante de lo que ya
 * habia.
 *
 * <p>El orden importa y es al reves de lo que parece: el <b>ultimo</b> que se agrega es el
 * <b>primero</b> que ve las llamadas.
 */
public interface MBeanServerForwarder extends MBeanServer {

    /** A quien le delega. */
    MBeanServer getMBeanServer();

    /** Cambia a quien le delega. */
    void setMBeanServer(MBeanServer mbs);
}
