package jdk.jshell.execution;

import java.lang.reflect.Method;

import jdk.jshell.spi.ExecutionControl;

/**
 * Like {@link DirectExecutionControl}, but the user's code runs on another thread.
 *
 * <h2>What the thread is for</h2>
 *
 * <p>For being able to cut it short. A snippet with an infinite loop run on JShell's thread hangs
 * JShell; run apart, {@link #stop} interrupts it and the session stays alive. That is the whole
 * difference from the class it inherits from, and it is the reason this is the default engine.
 *
 * <h2>How it cuts short</h2>
 *
 * <p>With {@link Thread#interrupt}, which is the only thing that can be done without risking the
 * process's consistency. A snippet that ignores the interruption does not stop, and rightly so:
 * killing it would leave whatever it was doing half done, in a process where JShell also lives.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>It works, with the same limit as in the JDK: it cuts short what lets itself be cut short.
 *
 * @since 9
 */
public class LocalExecutionControl extends DirectExecutionControl {

    /** The thread running the user's code right now, or {@code null}. */
    private Thread running;

    /** Whether a stop was asked for and has not been served yet. */
    private boolean stopping;

    /**
     * An engine with that loader.
     *
     * @param loaderDelegate whoever installs the classes
     */
    public LocalExecutionControl(LoaderDelegate loaderDelegate) {
        super(loaderDelegate);
    }

    /** An engine with the default loader. */
    public LocalExecutionControl() {
        super();
    }

    /**
     * An engine that installs the classes into that loader.
     *
     * @param loader the loader the user's classes are defined in
     */
    public LocalExecutionControl(ClassLoader loader) {
        super(new GivenLoader(loader));
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
        super.load(cbcs);
    }

    /**
     * Calls the method on a thread of its own and waits for the result.
     *
     * @param doitMethod the method
     * @return the representation of the result
     * @throws Exception whatever failed, or {@link ExecutionControl.StoppedException}
     */
    @Override
    protected String invoke(Method doitMethod) throws Exception {
        final Object[] result = new Object[1];
        final Throwable[] failure = new Throwable[1];
        final Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    result[0] = doitMethod.invoke(null, new Object[0]);
                } catch (Throwable e) {
                    failure[0] = e;
                }
            }
        }, "JShell user code");
        synchronized (this) {
            if (stopping) {
                stopping = false;
                throw new StoppedException();
            }
            running = t;
        }
        t.start();
        try {
            t.join();
        } finally {
            synchronized (this) {
                running = null;
            }
        }
        synchronized (this) {
            if (stopping) {
                stopping = false;
                throw new StoppedException();
            }
        }
        if (failure[0] instanceof Exception) {
            throw (Exception) failure[0];
        }
        if (failure[0] != null) {
            throw new RuntimeException(failure[0]);
        }
        return valueString(result[0]);
    }

    /**
     * Interrupts whichever thread is running the user's code.
     *
     * <p>If there is none, it leaves the mark set: the request may arrive exactly between JShell
     * deciding to execute and the thread starting, and losing it there would be worse than serving
     * it late.
     *
     * @throws EngineTerminationException if the engine is gone
     * @throws InternalException if it could not be cut short
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        final Thread t;
        synchronized (this) {
            stopping = true;
            t = running;
        }
        if (t != null) {
            t.interrupt();
        }
    }

    /** Notice that the user's code is about to be entered. */
    @Override
    protected void clientCodeEnter() {
    }

    /** Notice that the user's code has been left. */
    @Override
    protected void clientCodeLeave() {
    }

    /**
     * The loader used when this engine is handed a ready-made one.
     *
     * <p>It defines nothing: the user's classes are taken to be installed on the other side already.
     * It is the case of whoever embeds JShell inside their own application and wants the snippets to
     * see their classes.
     */
    private static final class GivenLoader implements LoaderDelegate {

        private final ClassLoader loader;

        GivenLoader(ClassLoader loader) {
            this.loader = loader;
        }

        @Override
        public void load(ClassBytecodes[] cbcs)
                throws ClassInstallException, NotImplementedException, EngineTerminationException {
            throw new NotImplementedException("load: the given loader does not define classes");
        }

        @Override
        public void classesRedefined(ClassBytecodes[] cbcs) {
        }

        @Override
        public void addToClasspath(String path)
                throws EngineTerminationException, InternalException {
            throw new InternalException("addToClasspath: the given loader cannot be extended");
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            return loader.loadClass(name);
        }
    }
}
