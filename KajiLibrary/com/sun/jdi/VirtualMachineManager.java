package com.sun.jdi;

import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.spi.Connection;
import java.util.List;

/**
 * El punto de entrada a JDI: de donde salen los conectores.
 *
 * <p>Los tres tipos de conector son las tres formas de llegar a una VM. El de
 * <strong>lanzamiento</strong> la arranca; el de <strong>enganche</strong> se conecta a una que ya
 * corre; el de <strong>escucha</strong> espera a que ella se conecte, que es lo que hace falta
 * cuando el cortafuegos deja pasar en un solo sentido.
 *
 * @since 1.3
 */
public interface VirtualMachineManager {

    /**
     * El default connector.
     *
     * @return el resultado
     */
    LaunchingConnector defaultConnector();

    /**
     * El launching connectors.
     *
     * @return el resultado
     */
    List<LaunchingConnector> launchingConnectors();

    /**
     * El attaching connectors.
     *
     * @return el resultado
     */
    List<AttachingConnector> attachingConnectors();

    /**
     * El listening connectors.
     *
     * @return el resultado
     */
    List<ListeningConnector> listeningConnectors();

    /**
     * Todos los connectors, heredados incluidos.
     *
     * @return el resultado
     */
    List<Connector> allConnectors();

    /**
     * El connected virtual machines.
     *
     * @return el resultado
     */
    List<VirtualMachine> connectedVirtualMachines();

    /**
     * El major interface version.
     *
     * @return el resultado
     */
    int majorInterfaceVersion();

    /**
     * El minor interface version.
     *
     * @return el resultado
     */
    int minorInterfaceVersion();

    /**
     * El create virtual machine.
     *
     * @param connection el Connection
     * @param process el Process
     * @return el resultado
     * @throws java.io.IOException si corresponde
     */
    VirtualMachine createVirtualMachine(Connection connection, Process process)
            throws java.io.IOException;

    /**
     * El create virtual machine.
     *
     * @param connection el Connection
     * @return el resultado
     * @throws java.io.IOException si corresponde
     */
    VirtualMachine createVirtualMachine(Connection connection)
            throws java.io.IOException;
}
