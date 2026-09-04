package javax.swing.table;

import javax.swing.event.TableModelListener;

/**
 * Los datos de una tabla: cuantas filas, cuantas columnas, y que hay en cada celda.
 *
 * <p>La tabla no guarda datos — los pide. Eso permite que una tabla de un millon de filas exista sin
 * que un millon de celdas esten en memoria: el modelo las calcula o las trae cuando se las piden.
 *
 * <p>{@link #getColumnClass} existe para que la tabla sepa <em>como</em> dibujar cada columna sin
 * mirar los valores: una de booleanos se dibuja con casillas, una de fechas con el formato local. Si
 * dependiera del contenido, una columna con la primera celda vacia se dibujaria distinto que las
 * demas.
 */
public interface TableModel {

    /** Cuantas filas hay. */
    int getRowCount();

    /** Cuantas columnas hay. */
    int getColumnCount();

    /** El nombre de una columna, para el encabezado. */
    String getColumnName(int columnIndex);

    /** El tipo de los valores de una columna; ver la nota de la interfaz. */
    Class<?> getColumnClass(int columnIndex);

    /** Si esa celda se puede editar. */
    boolean isCellEditable(int rowIndex, int columnIndex);

    /** El valor de esa celda. */
    Object getValueAt(int rowIndex, int columnIndex);

    /** Cambia el valor de esa celda. */
    void setValueAt(Object aValue, int rowIndex, int columnIndex);

    /** Agrega un oyente. */
    void addTableModelListener(TableModelListener l);

    /** Saca un oyente. */
    void removeTableModelListener(TableModelListener l);
}
