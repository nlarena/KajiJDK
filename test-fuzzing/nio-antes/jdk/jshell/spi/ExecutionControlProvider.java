package jdk.jshell.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link ExecutionControl}'s factory: what gets installed and what gets named.
 *
 * <p>It is what is declared as a service provider in a module or a JAR, and what
 * {@link ExecutionControl#generate(ExecutionEnv, String)} looks up by name. The split between the
 * factory and the engine is not ceremony: `jshell` has to be able to **list** the available engines
 * and show their parameters without starting any of them.
 *
 * <p>Hence {@link #defaultParameters()} belonging to the provider and not to the engine: they are
 * the parameters the engine would accept, and they have to be known before it is created.
 *
 * @since 9
 */
public interface ExecutionControlProvider {

    /**
     * The name it is chosen by.
     *
     * <p>It has to be a Java identifier: it is what goes before the colon in an engine
     * specification.
     */
    String name();

    /**
     * The parameters it accepts, with their default values.
     *
     * <p>By default, none. The map returned can be modified and then passed to {@link #generate}: it
     * is a copy, not the provider's state.
     */
    default Map<String, String> defaultParameters() {
        return new HashMap<String, String>();
    }

    /**
     * Creates the engine.
     *
     * @param env what `jshell` lends the engine
     * @param parameters the parameters; missing keys take their default value
     * @return the engine
     * @throws Throwable whatever fails while creating it
     */
    ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) throws Throwable;
}
