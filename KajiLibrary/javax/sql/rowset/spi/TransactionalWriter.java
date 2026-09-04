package javax.sql.rowset.spi;

import java.sql.SQLException;
import java.sql.Savepoint;

import javax.sql.RowSetWriter;

/**
 * Un escritor que ademas sabe confirmar y deshacer.
 *
 * <h2>Por que no esta en {@link RowSetWriter}</h2>
 *
 * <p>Porque no todo origen tiene transacciones. Un escritor contra un archivo XML puede escribir y
 * no puede deshacer; obligarlo a declarar {@code rollback} lo forzaria a tener un metodo que miente
 * o que falla siempre.
 *
 * <p>Separando la capacidad en su propia interfaz, un {@code CachedRowSet} pregunta con
 * {@code instanceof} si el escritor que le toco puede, en vez de intentarlo y ver que pasa.
 *
 * <h2>Los puntos de resguardo</h2>
 *
 * <p>{@link #rollback(Savepoint)} deshace hasta una marca en vez de deshacer todo. Sirve cuando un
 * lote de filas se escribe junto y una sola falla: se vuelve hasta antes de esa fila y el resto del
 * lote se conserva.
 *
 * @since 1.5
 */
public interface TransactionalWriter extends RowSetWriter {

    /**
     * Confirma lo escrito.
     *
     * @throws SQLException si no se pudo confirmar
     */
    void commit() throws SQLException;

    /**
     * Deshace todo lo escrito desde la ultima confirmacion.
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
