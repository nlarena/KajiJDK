package jdk.jshell.execution;

import java.util.Collections;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

/**
 * The local engine's provider: it runs the snippets in the same process as JShell.
 *
 * <h2>When it is worth it</h2>
 *
 * <p>When the snippets are meant to see what is already loaded --for instance, when embedding JShell
 * inside an application to inspect it from within-- and when starting another process costs too
 * much.
 *
 * <h2>When it is not</h2>
 *
 * <p>When what is run may not be trusted. A local snippet shares the heap, the threads and the open
 * files with JShell: a {@code System.exit(0)} of the user's takes the session with it. That is why
 * the {@code jshell} tool's default engine is not this one but the remote one.
 *
 * @since 9
 */
public class LocalExecutionControlProvider implements ExecutionControlProvider {

    /** One provider. */
    public LocalExecutionControlProvider() {
    }

    /**
     * The name it is asked for by.
     *
     * @return {@code "local"}
     */
    @Override
    public String name() {
        return "local";
    }

    /**
     * The parameters it accepts.
     *
     * @return an empty map: this engine has nothing to configure
     */
    @Override
    public Map<String, String> defaultParameters() {
        return Collections.<String, String>emptyMap();
    }

    /**
     * Builds the engine.
     *
     * @param env the session's environment
     * @param parameters the parameters; this engine uses none
     * @return the engine
     */
    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) {
        return createExecutionControl(env, parameters);
    }

    /**
     * The same as {@link #generate}, without declaring exceptions.
     *
     * <p>It exists because this provider cannot fail while building the engine --it opens no ports
     * and starts no processes-- and whoever uses it directly should not have to wrap it in a
     * {@code try}.
     *
     * @param env the session's environment
     * @param parameters the parameters
     * @return the engine
     */
    public ExecutionControl createExecutionControl(ExecutionEnv env,
            Map<String, String> parameters) {
        return new LocalExecutionControl();
    }
}
