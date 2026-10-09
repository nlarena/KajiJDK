package com.sun.jdi.connect;

import com.sun.jdi.VirtualMachine;
import java.io.IOException;
import java.util.Map;

/**
 * The connector that **starts** the debugged VM and connects to it.
 *
 * <p>It is the one an IDE uses when one hits "debug": it launches the process with the JDWP
 * options already set and waits for the connection to be established. The VM starts
 * **suspended** -- otherwise, the program might end before the debugger sets the first
 * breakpoint.
 *
 * <p>It is the only one of the three that may fail with {@link VMStartException}: it is the
 * only one that has a process of its own that may have started badly.
 */
public interface LaunchingConnector extends Connector {

    /**
     * It starts the VM those arguments describe and connects.
     *
     * @param arguments the map that came out of {@link #defaultArguments()}, with the values set
     * @return the debugged VM, suspended
     * @throws IOException if the other end could not be reached
     * @throws IllegalConnectorArgumentsException if some argument is missing or does not serve
     * @throws VMStartException if the VM started but the connection did not; the exception brings
     *     the process, and its streams have to be read and it has to be ended
     */
    VirtualMachine launch(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException, VMStartException;
}
