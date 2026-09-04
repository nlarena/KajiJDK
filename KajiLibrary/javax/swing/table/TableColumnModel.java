package javax.swing.table;

import java.util.Enumeration;

import javax.swing.ListSelectionModel;
import javax.swing.event.TableColumnModelListener;

/**
 * Que columnas tiene una tabla, en que orden y cuales estan seleccionadas.
 *
 * <h2>Separado del modelo de datos, y por eso la tabla se puede reordenar</h2>
 *
 * <p>{@link TableModel} dice que datos hay; esto dice como se presentan. Arrastrar una columna a otro
 * lugar es {@link #moveColumn} sobre este modelo y el de datos ni se entera — que es exactamente lo
 * que hace falta, porque el orden de las columnas es una preferencia de la vista.
 *
 * <p>De ahi que cada {@link TableColumn} lleve su propio indice de modelo: la posicion en esta lista
 * y la columna de la que saca los datos son dos numeros distintos.
 *
 * <h2>El margen, que parece un detalle y no lo es</h2>
 *
 * <p>{@link #getColumnMargin} es el espacio entre columnas, y entra en
 * {@link #getTotalColumnWidth} y en {@link #getColumnIndexAtX}. Olvidarlo hace que el ancho total no
 * cierre y que al hacer clic cerca de un borde se seleccione la columna de al lado.
 */
public interface TableColumnModel {

    /** Agrega una columna al final. */
    void addColumn(TableColumn aColumn);

    /** Saca una columna. */
    void removeColumn(TableColumn column);

    /** Mueve una columna de lugar en la vista. */
    void moveColumn(int columnIndex, int newIndex);

    /** Cambia el espacio entre columnas. */
    void setColumnMargin(int newMargin);

    /** Cuantas columnas hay. */
    int getColumnCount();

    /** Las columnas, en orden de vista. */
    Enumeration<TableColumn> getColumns();

    /**
     * Donde esta la columna con ese identificador.
     *
     * @throws IllegalArgumentException si no hay ninguna, o si el identificador es {@code null}
     */
    int getColumnIndex(Object columnIdentifier);

    /** La columna que esta en esa posicion de la vista. */
    TableColumn getColumn(int columnIndex);

    /** El espacio entre columnas. */
    int getColumnMargin();

    /** Que columna cae en esa coordenada horizontal, o {@code -1}. */
    int getColumnIndexAtX(int xPosition);

    /** El ancho de todas las columnas, contando los margenes. */
    int getTotalColumnWidth();

    /** Cambia si se pueden seleccionar columnas. */
    void setColumnSelectionAllowed(boolean flag);

    /** Si se pueden seleccionar columnas. */
    boolean getColumnSelectionAllowed();

    /** Las columnas seleccionadas. */
    int[] getSelectedColumns();

    /** Cuantas columnas estan seleccionadas. */
    int getSelectedColumnCount();

    /** Cambia el modelo de seleccion de columnas. */
    void setSelectionModel(ListSelectionModel newModel);

    /** El modelo de seleccion de columnas. */
    ListSelectionModel getSelectionModel();

    /** Agrega un oyente. */
    void addColumnModelListener(TableColumnModelListener x);

    /** Saca un oyente. */
    void removeColumnModelListener(TableColumnModelListener x);
}
