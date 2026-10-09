package com.sun.jdi.connect;

import com.sun.jdi.VirtualMachine;
import java.io.IOException;
import java.util.Map;

/**
 * The connector that **waits** for the debugged VM to connect to the debugger.
 *
 * <p>The roles are the other way round from {@link AttachingConnector}: here the debugger
 * listens and the VM calls, which is what happens when the program starts with {@code server=n}
 * and an address. It serves when the VM is behind something that does not allow connecting to
 * it, or when it starts by itself -- at system start-up, in a container -- and the debugger does
 * not control the moment.
 *
 * <p>That is why the cycle is three steps and not one: {@link #startListening} returns the
 * address that has to be passed to the VM, {@link #accept} waits for the connection, and
 * {@link #stopListening} closes.
 */
public interface ListeningConnector extends Connector {

    /**
     * Whether this connector may accept several connections over the same listen.
     *
     * <p>With `false` one has to go back to {@link #startListening} for each VM; and as the
     * address may change, it cannot be handed out in advance.
     */
    boolean supportsMultipleConnections();

    /**
     * It starts listening.
     *
     * @param arguments the map that came out of {@link #defaultArguments()}, with the values set
     * @return the address the VM has to connect to, in the transport's format
     * @throws IOException if the listen could not be opened
     * @throws IllegalConnectorArgumentsException if some argument is missing or does not serve
     */
    String startListening(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException;

    /**
     * It stops listening.
     *
     * <p>The arguments have to be the same as {@link #startListening}'s: it is how which of the
     * listens is closed is identified.
     *
     * @throws IOException if it could not be closed
     * @throws IllegalConnectorArgumentsException if some argument is missing or does not serve
     */
    void stopListening(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException;

    /**
     * It waits for a VM to connect.
     *
     * <p>It blocks. If the arguments bring a term and it runs out,
     * {@link TransportTimeoutException} comes out, which is an {@link IOException}.
     *
     * @param arguments the same as {@link #startListening}'s
     * @return the debugged VM
     * @throws IOException if the wait failed or the term ran out
     * @throws IllegalConnectorArgumentsException if some argument is missing or does not serve
     */
    VirtualMachine accept(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException;
}
