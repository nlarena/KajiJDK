package javax.sql.rowset;

import java.sql.SQLException;

/**
 * Fabrica los cinco tipos de {@code RowSet} sin nombrar sus implementaciones.
 *
 * <p>Es lo que evita el {@code new com.sun.rowset.CachedRowSetImpl()} que aparecia en el codigo
 * antes de que esta interfaz existiera: nombrar la clase concreta ataba la aplicacion a una
 * implementacion, y cambiarla obligaba a tocar cada punto de creacion.
 *
 * <p>La instancia se consigue con {@link RowSetProvider#newFactory()}.
 *
 * @since 1.7
 */
public interface RowSetFactory {

    /**
     * Un conjunto desconectado con cache.
     *
     * @return el conjunto
     * @throws SQLException si no se pudo crear
     */
    CachedRowSet createCachedRowSet() throws SQLException;

    /**
     * Un conjunto con filtro.
     *
     * @return el conjunto
     * @throws SQLException si no se pudo crear
     */
    FilteredRowSet createFilteredRowSet() throws SQLException;

    /**
     * Un conjunto conectado, envoltorio de un {@code ResultSet}.
     *
     * @return el conjunto
     * @throws SQLException si no se pudo crear
     */
    JdbcRowSet createJdbcRowSet() throws SQLException;

    /**
     * Un conjunto que une otros.
     *
     * @return el conjunto
     * @throws SQLException si no se pudo crear
     */
    JoinRowSet createJoinRowSet() throws SQLException;

    /**
     * Un conjunto que se serializa a XML.
     *
     * @return el conjunto
     * @throws SQLException si no se pudo crear
     */
    WebRowSet createWebRowSet() throws SQLException;
}
