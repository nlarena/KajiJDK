package jdk.jshell.execution;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.InternalException;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

/**
 * The loader {@link DirectExecutionControl} uses when it is not given another.
 *
 * <h2>How it installs</h2>
 *
 * <p>It keeps the bytes in a map and defines the class the first time somebody looks it up. Defining
 * it there and then would be simpler and would be wrong: JShell sends a snippet's classes together
 * and in any order, and a class inheriting from another of the same batch would fail if it happened
 * to go first. By defining on lookup, the loader's natural resolution asks for whichever are needed.
 *
 * <h2>Redefining</h2>
 *
 * <p>Replacing the code of an already loaded class is something only the VM's instrumentation can
 * do. Here the old class is forgotten and the new one is defined from scratch, which is what can be
 * done from a loader: the previous {@code Class} goes on existing for whoever holds it, but what is
 * looked up from now on is the new one.
 *
 * <p>That difference shows in one case only and it has to be said: an object created with the
 * previous version keeps its previous behaviour.
 */
final class DefaultLoaderDelegate implements LoaderDelegate {

    private final Loader loader;
    private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

    DefaultLoaderDelegate() {
        this.loader = new Loader();
        Thread.currentThread().setContextClassLoader(loader);
    }

    /** The real loader: it keeps bytes and defines only when asked for them. */
    private static final class Loader extends URLClassLoader {

        private final Map<String, byte[]> bytes = new HashMap<String, byte[]>();

        Loader() {
            super(new URL[0]);
        }

        synchronized void note(String name, byte[] b) {
            bytes.put(name, b);
        }

        synchronized boolean has(String name) {
            return bytes.containsKey(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            final byte[] b;
            synchronized (this) {
                b = bytes.get(name);
            }
            if (b == null) {
                return super.findClass(name);
            }
            return defineClass(name, b, 0, b.length);
        }

        void add(URL u) {
            addURL(u);
        }
    }

    @Override
    public void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        final boolean[] installed = new boolean[cbcs.length];
        try {
            for (int i = 0; i < cbcs.length; i++) {
                loader.note(cbcs[i].name(), cbcs[i].bytecodes());
                installed[i] = true;
            }
            // Only once they are all noted are they resolved, so that one inheriting from another of
            // the same batch finds its parent whatever order they arrived in.
            for (int i = 0; i < cbcs.length; i++) {
                classes.put(cbcs[i].name(), loader.loadClass(cbcs[i].name()));
            }
        } catch (Throwable e) {
            throw new ClassInstallException("load: " + e.getMessage(), installed);
        }
    }

    @Override
    public void classesRedefined(ClassBytecodes[] cbcs) {
        for (final ClassBytecodes cbc : cbcs) {
            loader.note(cbc.name(), cbc.bytecodes());
        }
    }

    @Override
    public void addToClasspath(String path) throws EngineTerminationException, InternalException {
        try {
            for (final String p : path.split(File.pathSeparator)) {
                if (!p.isEmpty()) {
                    loader.add(new File(p).toURI().toURL());
                }
            }
        } catch (MalformedURLException e) {
            throw new InternalException("addToClasspath: " + e.getMessage());
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        final Class<?> c = classes.get(name);
        if (c != null) {
            return c;
        }
        if (loader.has(name)) {
            return loader.loadClass(name);
        }
        throw new ClassNotFoundException(name + " not found");
    }
}
