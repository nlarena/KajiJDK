package javax.swing.tree;

import java.awt.Component;

import javax.swing.CellEditor;
import javax.swing.JTree;

/**
 * Quien deja editar el nombre de un nodo en el lugar.
 *
 * <p>Hereda de {@link CellEditor} todo lo de empezar, terminar y cancelar una edicion; lo unico que
 * agrega es el metodo que arma el componente sabiendo de que nodo se trata.
 *
 * <p>No recibe {@code hasFocus} como el dibujante: un editor siempre tiene el foco, porque para eso
 * se abrio.
 */
public interface TreeCellEditor extends CellEditor {

    /** El componente con el que se edita ese nodo. */
    Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
            boolean expanded, boolean leaf, int row);
}
