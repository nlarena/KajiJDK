package jdk.jshell.execution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

/**
 * The provider of the remote engine over JDI.
 *
 * <h2>The four parameters</h2>
 *
 * <p>{@link #PARAM_REMOTE_AGENT} is the main class of the process that executes; it can be changed
 * to put an agent of one's own. {@link #PARAM_TIMEOUT} is how long to wait for it to turn up.
 * {@link #PARAM_HOST_NAME} is which interface to bind to, and empty means the loopback one.
 * {@link #PARAM_LAUNCH} chooses between JDI launching the process and JDI waiting for it.
 *
 * <p>That the host name comes empty by default is a security decision: a debug port open to the
 * network is total control of the process for anybody who reaches it.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The parameters are real and {@link #defaultParameters} answers the same as the JDK.
 * {@link #generate} cannot: starting the other machine is JDI's transport, which this VM does not
 * have. Whoever wants an engine that works has {@link LocalExecutionControlProvider}, and
 * {@link FailOverExecutionControlProvider} reaches it by itself.
 *
 * @since 9
 */
public class JdiExecutionControlProvider implements ExecutionControlProvider {

    /** The main class of the process that executes. */
    public static final String PARAM_REMOTE_AGENT = "remoteAgent";

    /** How long to wait for the other machine to turn up, in milliseconds. */
    public static final String PARAM_TIMEOUT = "timeout";

    /** Which interface to bind to; empty is the loopback one. */
    public static final String PARAM_HOST_NAME = "hostname";

    /** Whether JDI has to launch the process instead of waiting for it. */
    public static final String PARAM_LAUNCH = "launch";

    private final JdiDefaultExecutionControl.JdiStarter starter;

    /** A provider that starts the other machine the usual way. */
    public JdiExecutionControlProvider() {
        this(null);
    }

    /**
     * A provider that leaves the starting to that starter.
     *
     * @param starter how the other machine turns up, or {@code null} for the usual way
     */
    public JdiExecutionControlProvider(JdiDefaultExecutionControl.JdiStarter starter) {
        this.starter = starter;
    }

    /**
     * The name it is asked for by.
     *
     * @return {@code "jdi"}
     */
    @Override
    public String name() {
        return "jdi";
    }

    /**
     * The parameters and their default values.
     *
     * @return the map of parameters
     */
    @Override
    public Map<String, String> defaultParameters() {
        final Map<String, String> out = new HashMap<String, String>();
        out.put(PARAM_REMOTE_AGENT, "jdk.jshell.execution.RemoteExecutionControl");
        out.put(PARAM_TIMEOUT, "5000");
        out.put(PARAM_HOST_NAME, "");
        out.put(PARAM_LAUNCH, "false");
        return out;
    }

    /**
     * Starts the other machine and returns the engine to talk to it with.
     *
     * @param env the session's environment
     * @param parameters the parameters
     * @return the engine
     * @throws IOException if it could not be started
     */
    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters)
            throws IOException {
        final Map<String, String> ps = parameters == null || parameters.isEmpty()
                ? defaultParameters() : parameters;
        final int timeout = Integer.parseInt(
                ps.get(PARAM_TIMEOUT) == null ? "5000" : ps.get(PARAM_TIMEOUT));
        final String host = ps.get(PARAM_HOST_NAME);
        final boolean launch = Boolean.parseBoolean(ps.get(PARAM_LAUNCH));
        final String agent = ps.get(PARAM_REMOTE_AGENT);
        if (starter != null) {
            // With a starter of one's own, the starting is its business: it may launch the other
            // machine however it likes. What this library cannot give it is JDI's connection.
            starter.start(env, ps, 0);
        }
        // With no starter of one's own it has to happen here, and here it cannot.
        new JdiInitiator(0, env == null ? null : env.extraRemoteVMOptions(), agent, launch,
                host == null || host.isEmpty() ? null : host, timeout,
                new HashMap<String, String>());
        throw new IOException("this VM has no JDI transport: there is no way to connect to another "
                + "virtual machine");
    }
}
