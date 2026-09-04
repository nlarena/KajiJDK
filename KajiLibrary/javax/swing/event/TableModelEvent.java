package javax.swing.event;

import java.util.EventObject;

import javax.swing.table.TableModel;

/**
 * Los datos de una tabla cambiaron.
 *
 * <h2>Un evento que se lee por sus constantes</h2>
 *
 * <p>La combinacion de rango, columna y tipo cubre desde "cambio una celda" hasta "cambio todo", y
 * las dos constantes especiales son las que hacen practico el caso general:
 * {@link #HEADER_ROW} como primera fila significa que cambio la <strong>estructura</strong> —hay
 * otras columnas, no otros datos— y {@link #ALL_COLUMNS} que el cambio abarca la fila entera.
 *
 * <p>La distincion importa porque un cambio de estructura obliga a la tabla a rehacer sus columnas,
 * y uno de datos solo a repintar. Confundirlos es la diferencia entre una tabla que parpadea y una
 * que muestra columnas viejas.
 */
public class TableModelEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** Se insertaron filas. */
    public static final int INSERT = 1;

    /** Cambiaron valores. */
    public static final int UPDATE = 0;

    /** Se borraron filas. */
    public static final int DELETE = -1;

    /** Como primera fila: cambio la estructura de columnas. */
    public static final int HEADER_ROW = -1;

    /** Como columna: el cambio abarca todas. */
    public static final int ALL_COLUMNS = -1;

    protected int type;
    protected int firstRow;
    protected int lastRow;
    protected int column;

    /** Cambio todo. */
    public TableModelEvent(TableModel source) {
        this(source, 0, Integer.MAX_VALUE, ALL_COLUMNS, UPDATE);
    }

    /** Cambio una fila entera. */
    public TableModelEvent(TableModel source, int row) {
        this(source, row, row, ALL_COLUMNS, UPDATE);
    }

    /** Cambio un rango de filas. */
    public TableModelEvent(TableModel source, int firstRow, int lastRow) {
        this(source, firstRow, lastRow, ALL_COLUMNS, UPDATE);
    }

    /** Cambio una columna de un rango de filas. */
    public TableModelEvent(TableModel source, int firstRow, int lastRow, int column) {
        this(source, firstRow, lastRow, column, UPDATE);
    }

    /** Con todo explicito. */
    public TableModelEvent(TableModel source, int firstRow, int lastRow, int column, int type) {
        super(source);
        this.firstRow = firstRow;
        this.lastRow = lastRow;
        this.column = column;
        this.type = type;
    }

    /** La primera fila afectada; {@link #HEADER_ROW} si cambio la estructura. */
    public int getFirstRow() {
        return this.firstRow;
    }

    /** La ultima fila afectada, inclusive. */
    public int getLastRow() {
        return this.lastRow;
    }

    /** La columna afectada, o {@link #ALL_COLUMNS}. */
    public int getColumn() {
        return this.column;
    }

    /** {@link #INSERT}, {@link #UPDATE} o {@link #DELETE}. */
    public int getType() {
        return this.type;
    }
}
