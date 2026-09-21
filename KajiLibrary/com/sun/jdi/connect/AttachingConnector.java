package com.sun.jdi.connect;

import com.sun.jdi.VirtualMachine;
import java.io.IOException;
import java.util.Map;

/**
 * The connector that attaches to a VM that is **already running**.
 *
 * <p>It is the commonest case outside development: a server that started with
 * {@code -agentlib:jdwp=transport=dt_socket,server=y,suspend=n} and that one connects to
 * afterwards, without restarting it.
 *
 * <p>The debugged VM is the one that listens; the debugger is the one that calls. It is the
 * other way round from {@link ListeningConnector}.
 */
public interface AttachingConnector extends Connector {

    /**
     * It attaches to the VM those arguments describe.
     *
     * @param arguments the map that came out of {@link #defaultArguments()}, with the values set
     * @return the debugged VM
     * @throws IOException if the other end could not be reached
     * @throws IllegalConnectorArgumentsException if some argument is missing or does not serve
     */
    VirtualMachine attach(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException;
}
