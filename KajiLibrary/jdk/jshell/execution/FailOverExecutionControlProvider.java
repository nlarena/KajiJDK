package jdk.jshell.execution;

import java.util.HashMap;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

/**
 * The provider that tries several in order and keeps the first one that works.
 *
 * <h2>Why it exists</h2>
 *
 * <p>Starting the remote engine may fail for reasons that have nothing to do with JShell: a firewall
 * that will not let the listening port be opened, a machine with no loopback interface configured, a
 * policy forbidding processes to be launched. None of those is the user's mistake and in all of them
 * there is a worse but serviceable way out.
 *
 * <p>Without this, JShell would not start on those machines and the message would talk about
 * sockets. With this, it starts with whichever engine it can.
 *
 * <h2>The parameters</h2>
 *
 * <p>They are numbered: {@code 0}, {@code 1}, {@code 2}... and each one names a provider. They are
 * tried in the order of their numbers. By default they are the remote one over JDI with launching,
 * the remote one over JDI listening, and the local one.
 *
 * <p>That the last is the local one is no accident: it is the only one that cannot fail because of
 * the environment, so it serves as the floor.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>It works, and it does exactly what it has to do: the first two on the list need JDI and fail,
 * and the session ends up with the local engine, which works. It is the only case in this package
 * where the missing transport does not show from outside.
 *
 * @since 9
 */
public class FailOverExecutionControlProvider implements ExecutionControlProvider {

    /** One provider. */
    public FailOverExecutionControlProvider() {
    }

    /**
     * The name it is asked for by.
     *
     * @return {@code "failover"}
     */
    @Override
    public String name() {
        return "failover";
    }

    /**
     * The providers to try, in order.
     *
     * <p>They are ten numbered slots and only the first comes filled. The other nine are empty on
     * purpose: they are where whoever configures the session puts their alternatives, and that they
     * exist with their number is what tells them how many they may put and what they are called.
     *
     * @return the map from position to provider specification
     */
    @Override
    public Map<String, String> defaultParameters() {
        final Map<String, String> out = new HashMap<String, String>();
        out.put("0", "jdi");
        for (int i = 1; i < 10; i++) {
            out.put(Integer.toString(i), "");
        }
        return out;
    }

    /**
     * Returns the engine of the first provider that does not fail.
     *
     * @param env the session's environment
     * @param parameters the providers to try, numbered
     * @return the engine
     * @throws Throwable whatever the last one failed with, if they all failed
     */
    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters)
            throws Throwable {
        final Map<String, String> ps = parameters == null || parameters.isEmpty()
                ? defaultParameters() : parameters;
        Throwable last = null;
        for (int i = 0; i < ps.size(); i++) {
            final String spec = ps.get(Integer.toString(i));
            if (spec == null || spec.isEmpty()) {
                continue;
            }
            try {
                final ExecutionControl ec = ExecutionControl.generate(env, spec);
                if (ec != null) {
                    return ec;
                }
            } catch (Throwable e) {
                // One failing is the expected case: that is what this class is for. The last reason
                // is kept, because if they all fail it is the only thing that can be reported.
                last = e;
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("no execution control provider succeeded");
    }
}
