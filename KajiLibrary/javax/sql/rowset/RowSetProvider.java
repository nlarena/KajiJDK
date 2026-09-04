package javax.sql.rowset;

import java.sql.SQLException;
import java.util.ServiceLoader;

/**
 * De donde sale la {@link RowSetFactory}.
 *
 * <h2>Las tres fuentes, en orden</h2>
 *
 * <ol>
 *   <li>la propiedad de sistema {@code javax.sql.rowset.RowSetFactory};
 *   <li>los servicios declarados que {@link ServiceLoader} encuentre;
 *   <li>la implementacion por omision.
 * </ol>
 *
 * <p>El orden es el que importa: lo que se pone por linea de comandos gana siempre, porque es lo que
 * alguien decidio para <strong>esta</strong> ejecucion. Los servicios declarados son la eleccion de
 * quien armo el classpath. La implementacion por omision es lo ultimo, para que nunca falte una.
 *
 * <h2>Por que existe esta clase</h2>
 *
 * <p>Para sacar del codigo el nombre de la clase concreta. Antes de que existiera, crear un
 * {@code CachedRowSet} significaba escribir {@code new com.sun.rowset.CachedRowSetImpl()} — un
 * nombre interno de una implementacion particular, repetido en cada punto de creacion.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>La resolucion de las tres fuentes es real y funciona: registrar una fabrica por propiedad de
 * sistema o como servicio declarado anda. Lo que no hay es la <strong>implementacion por
 * omision</strong> ({@code com.sun.rowset.RowSetFactoryImpl}, que no es API publica y son varias
 * clases); si no se configura ninguna, {@link #newFactory()} falla con {@link SQLException}
 * diciendo cual falta, en vez de devolver una fabrica que despues no fabrique nada.
 *
 * @since 1.7
 */
public class RowSetProvider {

    private static final String PROPIEDAD = "javax.sql.rowset.RowSetFactory";
    private static final String POR_OMISION = "com.sun.rowset.RowSetFactoryImpl";

    /** Para las subclases; esta clase no tiene estado ni metodos de instancia. */
    protected RowSetProvider() {
    }

    /**
     * La fabrica que corresponda segun las tres fuentes.
     *
     * @return la fabrica
     * @throws SQLException si ninguna fuente dio una fabrica utilizable
     */
    public static RowSetFactory newFactory() throws SQLException {
        final String delSistema = System.getProperty(PROPIEDAD);
        if (delSistema != null && delSistema.length() > 0) {
            return newFactory(delSistema, null);
        }

        try {
            for (final RowSetFactory f : ServiceLoader.load(RowSetFactory.class)) {
                return f;
            }
        } catch (final java.util.ServiceConfigurationError e) {
            throw excepcion("un RowSetFactory declarado como servicio no se pudo cargar", e);
        }

        return newFactory(POR_OMISION, null);
    }

    /**
     * La fabrica de esa clase, cargada con ese cargador.
     *
     * @param factoryClassName el nombre completo de la clase
     * @param cl el cargador a usar; {@code null} para el del contexto del hilo
     * @return la fabrica
     * @throws SQLException si el nombre es {@code null}, la clase no esta, no es una
     *     {@link RowSetFactory}, o no se pudo instanciar
     */
    public static RowSetFactory newFactory(final String factoryClassName, final ClassLoader cl)
            throws SQLException {
        if (factoryClassName == null) {
            throw new SQLException("el nombre de la clase de fabrica no puede ser null");
        }
        final ClassLoader cargador =
                cl != null ? cl : Thread.currentThread().getContextClassLoader();
        try {
            final Class<?> c = Class.forName(factoryClassName, true, cargador);
            final Object o = c.getDeclaredConstructor().newInstance();
            if (!(o instanceof RowSetFactory)) {
                throw new SQLException(factoryClassName + " no es un RowSetFactory");
            }
            return (RowSetFactory) o;
        } catch (final ClassNotFoundException e) {
            throw excepcion("no se encontro la clase de fabrica " + factoryClassName, e);
        } catch (final ReflectiveOperationException e) {
            throw excepcion("no se pudo instanciar la fabrica " + factoryClassName, e);
        }
    }

    private static SQLException excepcion(final String mensaje, final Throwable causa) {
        final SQLException e = new SQLException(mensaje);
        e.initCause(causa);
        return e;
    }
}
