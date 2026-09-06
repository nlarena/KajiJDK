package jdk.jshell.execution;

import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.ListeningConnector;

/**
 * Whoever starts the other virtual machine and connects to it.
 *
 * <h2>The two ways of meeting</h2>
 *
 * <p><strong>Launching</strong>: JDI starts the process and connects. It is the simplest and it
 * needs to be able to launch processes.
 *
 * <p><strong>Listening</strong>: a port is opened, the process is launched separately with orders to
 * connect there, and one waits. It is more work and it is what serves when the process has to start
 * in a particular way --another Java version, another user, a container.
 *
 * <p>This initiator uses the second, which is the one that gives control over how it is launched.
 *
 * <h2>The timeout</h2>
 *
 * <p>It exists because the other process may never arrive, and waiting forever for something that
 * failed to start would be worse than giving up.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>None of this can work: {@link Bootstrap#virtualMachineManager} needs an implementation of JDI,
 * which is native code plus the debugging protocol, and this VM does not have it. The
 * {@code com.sun.jdi} API is complete and the calls here are the right ones; what is missing is
 * underneath.
 *
 * @since 9
 */
public class JdiInitiator {

    private final int port;
    private final List<String> remoteVMOptions;
    private final String remoteAgent;
    private final boolean isLaunch;
    private final String host;
    private final int timeout;
    private final Map<String, String> connectorOptions;

    private VirtualMachine vm;
    private Process process;

    /**
     * An initiator with that configuration.
     *
     * @param port the port they will meet over
     * @param remoteVMOptions the options to start the other machine with
     * @param remoteAgent the agent's main class
     * @param isLaunch whether JDI has to launch the process, instead of waiting for it
     * @param host the machine to connect to, or {@code null} for the local one
     * @param timeout how long to wait, in milliseconds
     * @param connectorOptions the connector's additional options
     */
    public JdiInitiator(int port, List<String> remoteVMOptions, String remoteAgent,
            boolean isLaunch, String host, int timeout, Map<String, String> connectorOptions) {
        this.port = port;
        this.remoteVMOptions = remoteVMOptions;
        this.remoteAgent = remoteAgent;
        this.isLaunch = isLaunch;
        this.host = host;
        this.timeout = timeout;
        this.connectorOptions = connectorOptions;
        start();
    }

    /**
     * The virtual machine on the other side.
     *
     * @return the machine
     */
    public VirtualMachine vm() {
        return vm;
    }

    /**
     * The process that was launched, if one was.
     *
     * @return the process, or {@code null}
     */
    public Process process() {
        return process;
    }

    /**
     * Opens the listening port, launches the process and waits for it to connect.
     *
     * <p>The order matters and is not interchangeable: listening starts first and the launch comes
     * after. The other way round, the process could try to connect before there was anybody
     * listening and die in the attempt.
     *
     * @param connectorName the listening connector's name
     * @param remotePort the port
     * @param remoteVMOptions the other machine's options
     * @param processStarted whom to tell when the process has started
     */
    protected void runListenProcess(String connectorName, int remotePort,
            List<String> remoteVMOptions, ProcessStarted processStarted) {
        final ListeningConnector connector = listener(connectorName);
        final Map<String, Connector.Argument> args = connector.defaultArguments();
        final Connector.Argument p = args.get("port");
        if (p != null) {
            p.setValue(Integer.toString(remotePort));
        }
        try {
            connector.startListening(args);
            final ProcessBuilder pb = new ProcessBuilder(command(remoteVMOptions, remotePort));
            process = pb.start();
            processStarted.processStarted(process);
            vm = connector.accept(args);
        } catch (Throwable e) {
            throw new IllegalStateException("could not start the other machine", e);
        }
    }

    /** Starts in whichever way was configured. */
    private void start() {
        if (isLaunch) {
            throw new IllegalStateException("launch: this VM has no JDI transport");
        }
        runListenProcess("com.sun.jdi.SocketListen", port, remoteVMOptions,
                new ProcessStarted() {
                    public void processStarted(Process p) throws Throwable {
                    }
                });
    }

    /** The listening connector with that name. */
    private ListeningConnector listener(String name) {
        for (final ListeningConnector c
                : Bootstrap.virtualMachineManager().listeningConnectors()) {
            if (c.name().equals(name)) {
                return c;
            }
        }
        throw new IllegalStateException("no listening connector: " + name);
    }

    /** The command line the other machine is launched with. */
    private List<String> command(List<String> options, int remotePort) {
        final List<String> out = new java.util.ArrayList<String>();
        out.add(System.getProperty("java.home") + "/bin/java");
        out.add("-agentlib:jdwp=transport=dt_socket,address="
                + (host == null ? "" : host + ":") + remotePort + ",suspend=y");
        if (options != null) {
            out.addAll(options);
        }
        if (connectorOptions != null) {
            for (final Map.Entry<String, String> e : connectorOptions.entrySet()) {
                out.add("-D" + e.getKey() + "=" + e.getValue());
            }
        }
        out.add(remoteAgent);
        out.add(Integer.toString(timeout));
        return out;
    }

    /**
     * Whom to tell when the process has started.
     *
     * <p>It exists so that whoever launches can hook up their input and output streams before the
     * process writes anything. Waiting for JDI's connection to be ready would mean whatever the
     * process printed meanwhile was already lost.
     *
     * @since 9
     */
    public interface ProcessStarted {

        /**
         * The process has started.
         *
         * @param process the process
         * @throws Throwable if the listener could not do its part
         */
        void processStarted(Process process) throws Throwable;
    }
}
