package javax.swing.table;

import java.awt.Component;

import javax.swing.CellEditor;
import javax.swing.JTable;

/**
 * How a table cell is edited.
 *
 * <p>It is {@link CellEditor} plus the way of getting the editing component. The division is not
 * capricious: an edit's life cycle is the same in a table, a tree or a list, and the only thing
 * that changes is what it is passed to place itself.
 *
 * <p>Unlike {@link TableCellRenderer}, here the component <strong>is</strong> real: there is only
 * one at a time --the one of the cell being edited-- so it takes the focus, listens to the
 * keyboard and lives until the editing ends.
 */
public interface TableCellEditor extends CellEditor {

    /** Configures and returns the component to edit that cell with. */
    Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column);
}
