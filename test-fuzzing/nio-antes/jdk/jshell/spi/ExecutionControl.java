package jdk.jshell.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * The engine that runs what is typed into `jshell`.
 *
 * <p>It is the border between the half that **compiles** and the half that **runs**, and that border
 * exists because the two are usually in different processes: `jshell` compiles each snippet to a
 * `.class` in its own VM and sends it to another one to execute. The reason is that code typed into
 * a console hangs, breaks and calls `System.exit`, and none of those three may take the console down
 * with it.
 *
 * <p>Hence the shape of the interface, which otherwise reads oddly:
 *
 * <ul>
 *   <li>{@link #load} and {@link #redefine} send **bytes**, not objects: the same classes are not
 *       there on the other side.
 *   <li>{@link #invoke} and {@link #varValue} return **strings**: the real value lives in the other
 *       VM, and the only thing that can cross is its representation.
 *   <li>{@link #stop} exists apart from `close`: cutting off a hung snippet is not closing the
 *       engine.
 * </ul>
 *
 * <p>The nested exceptions split along the same axis: {@link RunException} is "the user's code
 * failed" --it is shown and the session goes on-- and {@link EngineTerminationException} is "the
 * engine died" --it has to be rebuilt.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>The interface and its ten nested types are complete; so are the two static {@code generate}
 * methods, and really so: they look the provider up with {@link ServiceLoader} and pass it the
 * parameters. What there is not is any installed provider, so today they throw
 * {@link IllegalArgumentException} for not finding one --which is exactly what the JDK does when an
 * engine that is not there is named.
 *
 * @since 9
 */
public interface ExecutionControl extends AutoCloseable {

    /**
     * Loads those classes into the engine.
     *
     * @param cbcs the classes, each with its name and its bytes
     * @throws ClassInstallException if any could not be loaded; the exception says which ones were
     * @throws NotImplementedException if the engine cannot load classes
     * @throws EngineTerminationException if the engine died
     */
    void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException;

    /**
     * Replaces the body of classes that are already loaded.
     *
     * <p>It is what allows a method to be redefined without restarting the session. Not every engine
     * can: one that cannot throws {@link NotImplementedException} and `jshell` recompiles.
     *
     * @throws ClassInstallException if any could not be redefined
     * @throws NotImplementedException if the engine cannot redefine
     * @throws EngineTerminationException if the engine died
     */
    void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException;

    /**
     * Calls a static method with no arguments and returns its result as text.
     *
     * @param className the class
     * @param methodName the method, static and with no arguments
     * @return the result, already turned into text in the executing VM
     * @throws RunException if the user's code threw, was stopped or did not resolve
     * @throws EngineTerminationException if the engine died
     * @throws InternalException if the machinery failed, not the user's code
     */
    String invoke(String className, String methodName)
            throws RunException, EngineTerminationException, InternalException;

    /**
     * A variable's value, as text.
     *
     * @param className the class that declares it
     * @param varName the variable's name
     * @throws RunException if reading it threw
     * @throws EngineTerminationException if the engine died
     * @throws InternalException if the machinery failed
     */
    String varValue(String className, String varName)
            throws RunException, EngineTerminationException, InternalException;

    /**
     * Adds a path to the engine's classpath.
     *
     * @throws EngineTerminationException if the engine died
     * @throws InternalException if the path could not be added
     */
    void addToClasspath(String path) throws EngineTerminationException, InternalException;

    /**
     * Stops whatever is executing.
     *
     * <p>It is called **from another thread**: the one that asked for the execution is blocked
     * waiting for it. The engine stays alive afterwards.
     *
     * @throws EngineTerminationException if the engine died
     * @throws InternalException if it could not be stopped
     */
    void stop() throws EngineTerminationException, InternalException;

    /**
     * An operation of this engine's own, which the interface does not cover.
     *
     * <p>It is the SPI's escape hatch: an engine with extra capabilities exposes them here, and the
     * client that knows them names them by their string.
     *
     * @throws RunException if the user's code threw
     * @throws EngineTerminationException if the engine died
     * @throws InternalException if the engine does not know that command
     */
    Object extensionCommand(String command, Object arg)
            throws RunException, EngineTerminationException, InternalException;

    /**
     * Closes the engine.
     *
     * <p>Redeclared without {@code throws} --{@link AutoCloseable#close()} declares it-- so that a
     * try-with-resources over an `ExecutionControl` does not force anything to be caught.
     */
    @Override
    void close();

    /**
     * The engine that specification names.
     *
     * <p>The specification is {@code name} or {@code name:key(value),key(value)}. The name is looked
     * up among the installed {@link ExecutionControlProvider}s; the parameters are passed on top of
     * its {@link ExecutionControlProvider#defaultParameters()}.
     *
     * @param env the environment the engine uses to talk to the user
     * @param spec the specification
     * @return the engine
     * @throws IllegalArgumentException if the specification is malformed or names an engine that is
     *     not installed
     * @throws Throwable whatever the provider throws while building it
     */
    static ExecutionControl generate(ExecutionEnv env, String spec) throws Throwable {
        if (env == null) {
            throw new NullPointerException("env");
        }
        if (spec == null) {
            throw new NullPointerException("spec");
        }
        int colon = spec.indexOf(':');
        String name = colon < 0 ? spec : spec.substring(0, colon);
        Map<String, String> parameters = colon < 0
                ? new HashMap<String, String>()
                : parseParameters(spec.substring(colon + 1));
        return generate(env, name.trim(), parameters);
    }

    /**
     * The engine of that name, with those parameters.
     *
     * @param env the environment the engine uses to talk to the user
     * @param name the provider's name
     * @param parameters the parameters, or `null` for the provider's own
     * @return the engine
     * @throws IllegalArgumentException if there is no provider with that name
     * @throws Throwable whatever the provider throws while building it
     */
    static ExecutionControl generate(ExecutionEnv env, String name, Map<String, String> parameters)
            throws Throwable {
        if (env == null) {
            throw new NullPointerException("env");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        Iterator<ExecutionControlProvider> it =
                ServiceLoader.load(ExecutionControlProvider.class).iterator();
        while (it.hasNext()) {
            ExecutionControlProvider p = it.next();
            if (name.equals(p.name())) {
                return p.generate(env, parameters == null ? p.defaultParameters() : parameters);
            }
        }
        throw new IllegalArgumentException("no ExecutionControlProvider named: " + name);
    }

    /** Splits {@code key(value),key(value)}. For the two `generate` methods' internal use. */
    private static Map<String, String> parseParameters(String text) {
        Map<String, String> out = new HashMap<String, String>();
        int i = 0;
        int n = text.length();
        while (i < n) {
            int open = text.indexOf('(', i);
            if (open < 0) {
                throw new IllegalArgumentException("expected '(' in: " + text);
            }
            int close = text.indexOf(')', open);
            if (close < 0) {
                throw new IllegalArgumentException("expected ')' in: " + text);
            }
            String key = text.substring(i, open).trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("empty parameter name in: " + text);
            }
            out.put(key, text.substring(open + 1, close));
            i = close + 1;
            if (i < n) {
                if (text.charAt(i) != ',') {
                    throw new IllegalArgumentException("expected ',' in: " + text);
                }
                i++;
            }
        }
        return out;
    }

    /**
     * A compiled class, ready to send to the engine.
     *
     * <p>{@link Serializable} because it has to cross to the other VM; and for that same reason it
     * copies the bytes on the way in and on the way out, so that nobody modifies what has already
     * been sent.
     */
    final class ClassBytecodes implements Serializable {

        private static final long serialVersionUID = 54506481972415973L;

        private final String name;
        private final byte[] bytecodes;

        /**
         * A class with that name and those bytes.
         *
         * @param name the binary name
         * @param bytecodes the `.class` contents; it is copied
         */
        public ClassBytecodes(String name, byte[] bytecodes) {
            this.name = name;
            this.bytecodes = bytecodes.clone();
        }

        /** A copy of the `.class` bytes. */
        public byte[] bytecodes() {
            return this.bytecodes.clone();
        }

        /** The class's binary name. */
        public String name() {
            return this.name;
        }
    }

    /** The root of everything that can go wrong in an engine. */
    abstract class ExecutionControlException extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * With that detail.
         *
         * @param message the detail
         */
        public ExecutionControlException(String message) {
            super(message);
        }
    }

    /**
     * Not all the classes could be installed.
     *
     * <p>It carries **which ones did** get in: loading is not atomic, and `jshell` needs to know what
     * was left half done so as not to send it again.
     */
    class ClassInstallException extends ExecutionControlException {

        private static final long serialVersionUID = 1L;

        private final boolean[] installed;

        /**
         * With that detail and that success map.
         *
         * @param message the detail
         * @param installed one position per class, in the order they were sent
         */
        public ClassInstallException(String message, boolean[] installed) {
            super(message);
            this.installed = installed == null ? null : installed.clone();
        }

        /** Which classes got in, in the order they were sent. */
        public boolean[] installed() {
            return this.installed == null ? null : this.installed.clone();
        }
    }

    /**
     * The engine died: nothing more can be asked of it.
     *
     * <p>It is the only one in the family that does **not** let the session go on. The others speak
     * of the user's code or of a missing service; this one speaks of the engine.
     */
    class EngineTerminationException extends ExecutionControlException {

        private static final long serialVersionUID = 1L;

        /**
         * With that detail.
         *
         * @param message the detail
         */
        public EngineTerminationException(String message) {
            super(message);
        }
    }

    /** The engine's machinery failed, not the user's code. */
    class InternalException extends ExecutionControlException {

        private static final long serialVersionUID = 1L;

        /**
         * With that detail.
         *
         * @param message the detail
         */
        public InternalException(String message) {
            super(message);
        }
    }

    /**
     * This engine does not implement that operation.
     *
     * <p>It inherits from {@link InternalException} and not from the root: to the caller it is a
     * machinery failure, and whoever does not tell the cases apart handles it the same.
     */
    class NotImplementedException extends InternalException {

        private static final long serialVersionUID = 1L;

        /**
         * With that detail.
         *
         * @param message the detail
         */
        public NotImplementedException(String message) {
            super(message);
        }
    }

    /**
     * The user's code did not finish running properly.
     *
     * <p>Abstract and with a private constructor: the list of subclasses is closed --it threw, it did
     * not resolve or it was stopped-- and an engine cannot invent a fourth. It is a sealed type
     * written before the language had `sealed`.
     */
    abstract class RunException extends ExecutionControlException {

        private static final long serialVersionUID = 1L;

        private RunException(String message) {
            super(message);
        }
    }

    /**
     * The user's code threw an exception.
     *
     * <p>It carries the exception class's **name** and not the exception: the class lives in the
     * other VM and may not exist in `jshell`'s.
     */
    class UserException extends RunException {

        private static final long serialVersionUID = 1L;

        private final String causeExceptionClass;

        /**
         * With that detail, that class and that stack.
         *
         * @param message the original exception's message
         * @param causeExceptionClass its class's name
         * @param stackElements its stack, as seen in the other VM
         */
        public UserException(String message, String causeExceptionClass,
                StackTraceElement[] stackElements) {
            super(message);
            this.causeExceptionClass = causeExceptionClass;
            setStackTrace(stackElements);
        }

        /** The name of the class of the exception the user's code threw. */
        public String causeExceptionClass() {
            return this.causeExceptionClass;
        }
    }

    /**
     * The snippet used something that is not defined yet.
     *
     * <p>It is the case that makes a console usable: in `jshell` you can write a method calling
     * another one that does not exist yet. The snippet compiles, and only on executing it does this
     * come out, with the identifier of what is missing.
     */
    class ResolutionException extends RunException {

        private static final long serialVersionUID = 1L;

        private final int id;

        /**
         * With that identifier and that stack.
         *
         * @param id the identifier of what is missing
         * @param stackElements the stack, as seen in the other VM
         */
        public ResolutionException(int id, StackTraceElement[] stackElements) {
            super("resolution exception: " + id);
            this.id = id;
            setStackTrace(stackElements);
        }

        /** The identifier of what is missing. */
        public int id() {
            return this.id;
        }
    }

    /** Execution was stopped with {@link ExecutionControl#stop()}. */
    class StoppedException extends RunException {

        private static final long serialVersionUID = 1L;

        /** No detail: there is nothing more to say than "it was stopped". */
        public StoppedException() {
            super("stopped by user");
        }
    }
}
