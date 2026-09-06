package jdk.jshell.execution;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.SPIResolutionException;

/**
 * The engine that runs the snippets <strong>on this very virtual machine</strong>, by reflection.
 *
 * <h2>What it does</h2>
 *
 * <p>JShell compiles each snippet into a class with a method, sends it to {@link #load}, and then
 * asks for {@link #invoke}. This engine installs the classes with a {@link LoaderDelegate}, finds
 * the method and calls it. There is no separate process and no protocol: it is a call.
 *
 * <h2>Why the result is a {@code String}</h2>
 *
 * <p>Because the engine may be on the other side of a socket, and there returning the object would
 * mean serializing it --and everything hanging off it. Its representation is sent and that is that.
 * That the local version does the same is not waste: it is what guarantees a snippet looks the same
 * running locally or remotely.
 *
 * <h2>The user's exceptions</h2>
 *
 * <p>An exception from the user's code is not a failure of this engine, so it cannot travel up as it
 * is: it is turned into an {@link ExecutionControl.UserException}, which carries the original
 * class's name and the stack trace. The conversion goes through
 * {@link #throwConvertedInvocationException}, which is separate precisely so that a remote engine
 * can do it differently.
 *
 * <p>The odd case is {@link SPIResolutionException}: it is not a mistake in the program but the way
 * JShell says the snippet used something that is not defined yet. That is why it is recognized and
 * turned into an {@link ExecutionControl.ResolutionException}, which JShell understands.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>It works. Defining a class from its bytecode and calling it by reflection is all it needs, and
 * both work. The only thing it cannot do is {@link #stop}: cutting short what is running requires
 * the execution to go through another thread, and that is what {@link LocalExecutionControl} adds.
 *
 * @since 9
 */
public class DirectExecutionControl implements ExecutionControl {

    private final LoaderDelegate loaderDelegate;

    /**
     * An engine with that loader.
     *
     * @param loaderDelegate whoever installs the classes
     */
    public DirectExecutionControl(LoaderDelegate loaderDelegate) {
        this.loaderDelegate = loaderDelegate;
    }

    /** An engine with the default loader. */
    public DirectExecutionControl() {
        this(new DefaultLoaderDelegate());
    }

    /**
     * Installs those classes.
     *
     * @param cbcs the name and the bytecode of each one
     * @throws ClassInstallException if any of them could not be installed
     * @throws NotImplementedException if the loader does not know how to install
     * @throws EngineTerminationException if the engine is gone
     */
    @Override
    public void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        loaderDelegate.load(cbcs);
        classesRedefined(cbcs);
    }

    /**
     * Replaces the code of classes that were already there.
     *
     * @param cbcs the classes and their new bytecode
     * @throws ClassInstallException if they could not be replaced
     * @throws NotImplementedException if this engine does not know how to redefine
     * @throws EngineTerminationException if the engine is gone
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        throw new NotImplementedException("redefine not supported");
    }

    /**
     * Notice that those classes changed content.
     *
     * @param cbcs the redefined classes
     * @throws NotImplementedException if this engine does not support it
     * @throws EngineTerminationException if the engine is gone
     */
    protected void classesRedefined(ClassBytecodes[] cbcs)
            throws NotImplementedException, EngineTerminationException {
        loaderDelegate.classesRedefined(cbcs);
    }

    /**
     * Calls that static method with no arguments and returns its result.
     *
     * @param className the class
     * @param methodName the method
     * @return the representation of the result
     * @throws RunException if the user's code failed
     * @throws InternalException if the engine failed
     * @throws EngineTerminationException if the engine is gone
     */
    @Override
    public String invoke(String className, String methodName)
            throws RunException, InternalException, EngineTerminationException {
        final Method m;
        try {
            m = findClass(className).getDeclaredMethod(methodName, new Class<?>[0]);
            m.setAccessible(true);
        } catch (Throwable e) {
            throw new InternalException("invoke: " + e.getMessage());
        }
        clientCodeEnter();
        try {
            return invoke(m);
        } catch (InvocationTargetException e) {
            return throwConvertedInvocationException(e.getCause());
        } catch (Throwable e) {
            return throwConvertedOtherException(e);
        } finally {
            clientCodeLeave();
        }
    }

    /**
     * A static variable's value, as text.
     *
     * @param className the class
     * @param varName the variable
     * @return the representation of the value
     * @throws RunException if the user's code failed
     * @throws EngineTerminationException if the engine is gone
     * @throws InternalException if the engine failed
     */
    @Override
    public String varValue(String className, String varName)
            throws RunException, EngineTerminationException, InternalException {
        final Object v;
        try {
            final java.lang.reflect.Field f = findClass(className).getDeclaredField(varName);
            f.setAccessible(true);
            v = f.get(null);
        } catch (Throwable e) {
            throw new InternalException("varValue: " + e.getMessage());
        }
        return valueString(v);
    }

    /**
     * Adds an entry to the class search path.
     *
     * @param path the entry
     * @throws EngineTerminationException if the engine is gone
     * @throws InternalException if it could not be added
     */
    @Override
    public void addToClasspath(String path) throws EngineTerminationException, InternalException {
        loaderDelegate.addToClasspath(path);
    }

    /**
     * Cuts short whatever is running.
     *
     * @throws EngineTerminationException if the engine is gone
     * @throws InternalException if it could not be cut short
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        throw new NotImplementedException("stop: not supported");
    }

    /**
     * An operation that is not in the interface, for engines with capabilities of their own.
     *
     * @param command the operation's name
     * @param arg its argument
     * @return whatever it returns
     * @throws RunException if the user's code failed
     * @throws EngineTerminationException if the engine is gone
     * @throws InternalException if the engine failed
     */
    @Override
    public Object extensionCommand(String command, Object arg)
            throws RunException, EngineTerminationException, InternalException {
        throw new NotImplementedException("extensionCommand: " + command);
    }

    /** Closes the engine. */
    @Override
    public void close() {
    }

    /**
     * Looks an installed class up.
     *
     * @param className the name
     * @return the class
     * @throws ClassNotFoundException if it is not there
     */
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        return loaderDelegate.findClass(className);
    }

    /**
     * The call proper.
     *
     * <p>It is separate so that subclasses can wrap it: {@link LocalExecutionControl} runs it on
     * another thread, which is what makes cutting it short possible.
     *
     * @param doitMethod the method
     * @return the representation of the result
     * @throws Exception whatever failed
     */
    protected String invoke(Method doitMethod) throws Exception {
        return valueString(doitMethod.invoke(null, new Object[0]));
    }

    /**
     * How a value looks on JShell's side.
     *
     * <p>Strings go in quotes and the rest through their {@code toString}. Without the quotes,
     * {@code null} and the string {@code "null"} would look the same, and they are different things.
     *
     * @param value the value
     * @return its representation
     */
    protected static String valueString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof Character) {
            return "'" + value + "'";
        }
        return value.toString();
    }

    /**
     * Converts an exception the user's code threw.
     *
     * <p>It never returns: it always throws. It returns {@code String} so that it can be written as
     * {@code return throwConverted...(e)} where a value is needed, which is one way of letting the
     * compiler know that path does not carry on.
     *
     * @param ex what the user threw
     * @return never
     * @throws RunException the converted version
     * @throws InternalException if the conversion itself failed
     */
    protected String throwConvertedInvocationException(Throwable ex)
            throws RunException, InternalException {
        if (ex instanceof SPIResolutionException) {
            final SPIResolutionException e = (SPIResolutionException) ex;
            throw new ResolutionException(e.id(), ex.getStackTrace());
        }
        throw new UserException(String.valueOf(ex.getMessage()), ex.getClass().getName(),
                ex.getStackTrace());
    }

    /**
     * Converts an exception that did not come from the user's code.
     *
     * @param ex what failed
     * @return never
     * @throws RunException if it should be treated as the user's after all
     * @throws InternalException the usual case
     */
    protected String throwConvertedOtherException(Throwable ex)
            throws RunException, InternalException {
        throw new InternalException(ex.toString() + Arrays.toString(ex.getStackTrace()));
    }

    /**
     * Notice that the user's code is about to be entered.
     *
     * @throws InternalException if the engine is not in a fit state
     */
    protected void clientCodeEnter() throws InternalException {
    }

    /**
     * Notice that the user's code has been left.
     *
     * @throws InternalException if the engine is not in a fit state
     */
    protected void clientCodeLeave() throws InternalException {
    }
}
