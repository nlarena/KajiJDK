package com.sun.jdi;

import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.spi.Connection;
import java.util.List;

/**
 * The entry point to JDI: where the connectors come from.
 *
 * <p>The three kinds of connector are the three ways of reaching a VM. The
 * <strong>launching</strong> one starts it; the <strong>attaching</strong> one connects to one
 * that is already running; the <strong>listening</strong> one waits for it to connect, which is
 * what is needed when the firewall only lets through in one direction.
 *
 * @since 1.3
 */
public interface VirtualMachineManager {

    /**
     * The default connector.
     *
     * @return the result
     */
    LaunchingConnector defaultConnector();

    /**
     * The launching connectors.
     *
     * @return the result
     */
    List<LaunchingConnector> launchingConnectors();

    /**
     * The attaching connectors.
     *
     * @return the result
     */
    List<AttachingConnector> attachingConnectors();

    /**
     * The listening connectors.
     *
     * @return the result
     */
    List<ListeningConnector> listeningConnectors();

    /**
     * Every connectors, the inherited ones included.
     *
     * @return the result
     */
    List<Connector> allConnectors();

    /**
     * The connected virtual machines.
     *
     * @return the result
     */
    List<VirtualMachine> connectedVirtualMachines();

    /**
     * The major interface version.
     *
     * @return the result
     */
    int majorInterfaceVersion();

    /**
     * The minor interface version.
     *
     * @return the result
     */
    int minorInterfaceVersion();

    /**
     * The create virtual machine.
     *
     * @param connection the Connection
     * @param process the Process
     * @return the result
     * @throws java.io.IOException if it applies
     */
    VirtualMachine createVirtualMachine(Connection connection, Process process)
            throws java.io.IOException;

    /**
     * The create virtual machine.
     *
     * @param connection the Connection
     * @return the result
     * @throws java.io.IOException if it applies
     */
    VirtualMachine createVirtualMachine(Connection connection)
            throws java.io.IOException;
}
