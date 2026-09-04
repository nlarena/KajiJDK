package javax.swing.table;

import java.awt.Component;

import javax.swing.CellEditor;
import javax.swing.JTable;

/**
 * Como se edita una celda de una tabla.
 *
 * <p>Es {@link CellEditor} mas la forma de conseguir el componente de edicion. La division no es
 * caprichosa: el ciclo de vida de una edicion es el mismo en una tabla, un arbol o una lista, y solo
 * cambia que se le pasa para ubicarse.
 *
 * <p>A diferencia de {@link TableCellRenderer}, aca el componente <strong>si</strong> es de verdad:
 * hay uno solo a la vez —el de la celda que se esta editando— asi que recibe el foco, escucha el
 * teclado y vive hasta que la edicion termina.
 */
public interface TableCellEditor extends CellEditor {

    /** Configura y devuelve el componente con el que editar esa celda. */
    Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column);
}
