package javax.sql.rowset.spi;

import java.sql.SQLException;

import javax.sql.RowSet;

/**
 * El conjunto de filas que <strong>no</strong> se pudieron escribir, para decidir una por una.
 *
 * <h2>Que es un conflicto</h2>
 *
 * <p>El {@code RowSet} se lleno en un momento y se escribe en otro. Si entre esos dos momentos
 * alguien cambio la misma fila en el origen, escribir encima perderia su cambio sin que nadie se
 * entere. El proveedor detecta eso y no escribe: en lugar de decidir por su cuenta, junta las filas
 * en conflicto y las entrega aca.
 *
 * <h2>Por que es un {@link RowSet} y ademas tiene sus propios accesores</h2>
 *
 * <p>Porque hay <strong>tres</strong> valores por celda en juego: el que estaba cuando se cargo, el
 * que el usuario escribio, y el que hay ahora en el origen. Un {@code RowSet} solo puede contener
 * uno.
 *
 * <p>La division es: los metodos heredados de {@code RowSet} dan el valor que el usuario quiso
 * poner, {@link #getConflictValue} da el que hay en el origen ahora mismo, y
 * {@link #setResolvedValue} es donde se escribe el que finalmente va a quedar. Resolver un
 * conflicto es mirar los dos primeros y elegir el tercero.
 *
 * <h2>El recorrido</h2>
 *
 * <p>{@link #nextConflict} y {@link #previousConflict} recorren solo las filas conflictivas,
 * salteando las que se escribieron bien. {@link #getStatus} dice de que tipo es el conflicto de la
 * fila actual — al actualizar, al borrar o al insertar—, que no se resuelven igual: una fila que ya
 * no existe en el origen no se puede actualizar de ninguna manera.
 *
 * @since 1.5
 */
public interface SyncResolver extends RowSet {

    /** La fila que se quiso actualizar cambio en el origen. */
    int UPDATE_ROW_CONFLICT = 0;

    /** La fila que se quiso borrar cambio en el origen. */
    int DELETE_ROW_CONFLICT = 1;

    /** La fila que se quiso insertar choca con una que ya esta. */
    int INSERT_ROW_CONFLICT = 2;

    /** Esta fila no tuvo conflicto. */
    int NO_ROW_CONFLICT = 3;

    /**
     * De que tipo es el conflicto de la fila actual.
     *
     * @return una de las cuatro constantes
     */
    int getStatus();

    /**
     * El valor que hay <strong>en el origen</strong> para esa columna.
     *
     * @param index la columna, desde 1
     * @return el valor del origen
     * @throws SQLException si el indice no es valido o no hay fila actual
     */
    Object getConflictValue(int index) throws SQLException;

    /**
     * El valor que hay <strong>en el origen</strong> para esa columna.
     *
     * @param columnName el nombre de la columna
     * @return el valor del origen
     * @throws SQLException si el nombre no existe o no hay fila actual
     */
    Object getConflictValue(String columnName) throws SQLException;

    /**
     * Fija el valor con el que se resuelve el conflicto de esa columna.
     *
     * @param index la columna, desde 1
     * @param obj el valor que va a quedar
     * @throws SQLException si el indice no es valido o no hay fila actual
     */
    void setResolvedValue(int index, Object obj) throws SQLException;

    /**
     * Fija el valor con el que se resuelve el conflicto de esa columna.
     *
     * @param columnName el nombre de la columna
     * @param obj el valor que va a quedar
     * @throws SQLException si el nombre no existe o no hay fila actual
     */
    void setResolvedValue(String columnName, Object obj) throws SQLException;

    /**
     * Avanza a la proxima fila en conflicto.
     *
     * @return {@code true} si habia otra
     * @throws SQLException si no se pudo avanzar
     */
    boolean nextConflict() throws SQLException;

    /**
     * Retrocede a la fila en conflicto anterior.
     *
     * @return {@code true} si habia otra
     * @throws SQLException si no se pudo retroceder
     */
    boolean previousConflict() throws SQLException;
}
