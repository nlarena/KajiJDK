package javax.swing.tree;

import java.awt.Component;

import javax.swing.CellEditor;
import javax.swing.JTree;

/**
 * Whoever allows editing a node's name in place.
 *
 * <p>It inherits from {@link CellEditor} everything about starting, ending and cancelling an
 * edit; the only thing it adds is the method that builds the component knowing which node it is
 * about.
 *
 * <p>It does not receive {@code hasFocus} like the renderer: an editor always has the focus,
 * because that is what it was opened for.
 */
public interface TreeCellEditor extends CellEditor {

    /** The component to edit that node with. */
    Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
            boolean expanded, boolean leaf, int row);
}
