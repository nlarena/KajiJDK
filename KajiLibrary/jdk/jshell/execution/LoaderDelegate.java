package jdk.jshell.execution;

import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.InternalException;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

/**
 * Whoever takes care of getting the classes into the virtual machine that runs them.
 *
 * <h2>Why it is a separate interface</h2>
 *
 * <p>An execution engine does two quite different things: installing the code and running it. The
 * second is always the same --find the method and call it; the first changes completely depending on
 * where the code is, whether it has to be isolated from the rest, and whether it can be replaced
 * while running.
 *
 * <p>Separating them is what allows a single {@link DirectExecutionControl} whose loader can be
 * swapped: one that defines into a {@link ClassLoader} of its own, another that reuses the program's,
 * another that sends the bytes to the far end.
 *
 * <h2>{@link #classesRedefined} throws nothing</h2>
 *
 * <p>It is a notice, not an operation: it tells the loader that some classes it installed have just
 * changed content. Whoever redefined them has already done the work and is not waiting for
 * permission, so there is nothing the loader could answer.
 *
 * @since 9
 */
public interface LoaderDelegate {

    /**
     * Installs those classes.
     *
     * @param cbcs the name and the bytecode of each one
     * @throws ClassInstallException if any of them could not be installed
     * @throws NotImplementedException if this loader does not know how to install
     * @throws EngineTerminationException if the engine is gone
     */
    void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException;

    /**
     * Notice that those classes changed content.
     *
     * @param cbcs the redefined classes
     */
    void classesRedefined(ClassBytecodes[] cbcs);

    /**
     * Adds an entry to the class search path.
     *
     * @param path the entry
     * @throws EngineTerminationException if the engine is gone
     * @throws InternalException if it could not be added
     */
    void addToClasspath(String path) throws EngineTerminationException, InternalException;

    /**
     * Looks a class up by name.
     *
     * @param name the full name
     * @return the class
     * @throws ClassNotFoundException if it is not there
     */
    Class<?> findClass(String name) throws ClassNotFoundException;
}
