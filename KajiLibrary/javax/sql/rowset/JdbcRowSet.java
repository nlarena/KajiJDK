package javax.sql.rowset;

import java.sql.SQLException;
import java.sql.Savepoint;

import javax.sql.RowSet;

/**
 * Un {@code RowSet} <strong>conectado</strong>: un envoltorio delgado sobre un {@code ResultSet}.
 *
 * <h2>Que agrega si ya existe {@code ResultSet}</h2>
 *
 * <p>Dos cosas. Es un componente al estilo JavaBeans —tiene propiedades que se fijan y despues se
 * ejecuta— y emite eventos, asi que una interfaz grafica se puede enganchar a el. Y es desplazable y
 * actualizable aunque el controlador subyacente no lo sea de por si.
 *
 * <p>Lo que <strong>no</strong> agrega es desconexion: mantiene la conexion abierta todo el tiempo,
 * como un {@code ResultSet}. Para soltarla esta {@link CachedRowSet}.
 *
 * <h2>Cuando conviene este y no un {@link CachedRowSet}</h2>
 *
 * <p>Cuando los datos tienen que estar frescos y el conjunto es grande. Al estar conectado, lo que
 * se lee es lo que hay ahora, y no hay conflictos que resolver porque no hay ventana entre leer y
 * escribir. El precio es la conexion retenida.
 *
 * @since 1.5
 */
public interface JdbcRowSet extends RowSet, Joinable {

    /**
     * Si las filas borradas se siguen viendo al recorrer.
     *
     * @return si se muestran
     * @throws SQLException si no se pudo consultar
     */
    boolean getShowDeleted() throws SQLException;

    /**
     * Muestra o esconde las filas borradas.
     *
     * @param b si mostrarlas
     * @throws SQLException si no se pudo cambiar
     */
    void setShowDeleted(boolean b) throws SQLException;

    /**
     * Los avisos acumulados.
     *
     * @return el primero de la cadena, o {@code null}
     * @throws SQLException si no se pudieron obtener
     */
    RowSetWarning getRowSetWarnings() throws SQLException;

    /**
     * Confirma la transaccion.
     *
     * @throws SQLException si no se pudo confirmar
     */
    void commit() throws SQLException;

    /**
     * Si la conexion confirma sola cada sentencia.
     *
     * @return si esta en confirmacion automatica
     * @throws SQLException si no se pudo consultar
     */
    boolean getAutoCommit() throws SQLException;

    /**
     * Prende o apaga la confirmacion automatica.
     *
     * <p>Apagarla es lo que hace posible {@link #rollback}: con la confirmacion automatica prendida
     * cada sentencia ya quedo escrita y no hay nada que deshacer.
     *
     * @param autoCommit si confirmar sola
     * @throws SQLException si no se pudo cambiar
     */
    void setAutoCommit(boolean autoCommit) throws SQLException;

    /**
     * Deshace la transaccion.
     *
     * @throws SQLException si no se pudo deshacer
     */
    void rollback() throws SQLException;

    /**
     * Deshace hasta el punto de resguardo dado.
     *
     * @param s el punto de resguardo
     * @throws SQLException si no se pudo deshacer
     */
    void rollback(Savepoint s) throws SQLException;
}
