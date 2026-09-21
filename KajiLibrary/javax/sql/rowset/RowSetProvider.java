package javax.sql.rowset;

import java.sql.SQLException;
import java.util.ServiceLoader;

/**
 * Where the {@link RowSetFactory} comes from.
 *
 * <h2>The three sources, in order</h2>
 *
 * <ol>
 *   <li>the system property {@code javax.sql.rowset.RowSetFactory};
 *   <li>the declared services {@link ServiceLoader} finds;
 *   <li>the default implementation.
 * </ol>
 *
 * <p>The order is what matters: whatever is put on the command line always wins, because it is what
 * somebody decided for <strong>this</strong> run. The declared services are the choice of whoever
 * put the classpath together. The default implementation comes last, so that there is never none.
 *
 * <h2>Why this class exists</h2>
 *
 * <p>To take the name of the concrete class out of the code. Before it existed, creating a {@code
 * CachedRowSet} meant writing {@code new com.sun.rowset.CachedRowSetImpl()} — an internal name of a
 * particular implementation, repeated at every creation point.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The resolution of the three sources is real and works: registering a factory by system
 * property or as a declared service works. What is missing is the <strong>default
 * implementation</strong> ({@code com.sun.rowset.RowSetFactoryImpl}, which is not public API and is
 * several classes); if none is configured, {@link #newFactory()} fails with {@link SQLException}
 * saying which one is missing, instead of returning a factory that then makes nothing.
 *
 * @since 1.7
 */
public class RowSetProvider {

    private static final String PROPERTY = "javax.sql.rowset.RowSetFactory";
    private static final String DEFAULT_FACTORY = "com.sun.rowset.RowSetFactoryImpl";

    /** For the subclasses; this class has no state nor instance methods. */
    protected RowSetProvider() {
    }

    /**
     * The factory that corresponds according to the three sources.
     *
     * @return the factory
     * @throws SQLException if no source gave a usable factory
     */
    public static RowSetFactory newFactory() throws SQLException {
        final String fromSystem = System.getProperty(PROPERTY);
        if (fromSystem != null && fromSystem.length() > 0) {
            return newFactory(fromSystem, null);
        }

        try {
            for (final RowSetFactory f : ServiceLoader.load(RowSetFactory.class)) {
                return f;
            }
        } catch (final java.util.ServiceConfigurationError e) {
            throw sqlException("a RowSetFactory declared as a service could not be loaded", e);
        }

        return newFactory(DEFAULT_FACTORY, null);
    }

    /**
     * The factory of that class, loaded with that loader.
     *
     * @param factoryClassName the fully qualified name of the class
     * @param cl the loader to use; {@code null} for the thread's context one
     * @return the factory
     * @throws SQLException if the name is {@code null}, the class is not there, it is not a
     *     {@link RowSetFactory}, or it could not be instantiated
     */
    public static RowSetFactory newFactory(final String factoryClassName, final ClassLoader cl)
            throws SQLException {
        if (factoryClassName == null) {
            throw new SQLException("the factory class name cannot be null");
        }
        final ClassLoader loader =
                cl != null ? cl : Thread.currentThread().getContextClassLoader();
        try {
            final Class<?> c = Class.forName(factoryClassName, true, loader);
            final Object o = c.getDeclaredConstructor().newInstance();
            if (!(o instanceof RowSetFactory)) {
                throw new SQLException(factoryClassName + " is not a RowSetFactory");
            }
            return (RowSetFactory) o;
        } catch (final ClassNotFoundException e) {
            throw sqlException("factory class not found: " + factoryClassName, e);
        } catch (final ReflectiveOperationException e) {
            throw sqlException("could not instantiate the factory " + factoryClassName, e);
        }
    }

    private static SQLException sqlException(final String message, final Throwable cause) {
        final SQLException e = new SQLException(message);
        e.initCause(cause);
        return e;
    }
}
