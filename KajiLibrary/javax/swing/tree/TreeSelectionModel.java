package javax.swing.tree;

import java.beans.PropertyChangeListener;

import javax.swing.event.TreeSelectionListener;

/**
 * Que nodos de un arbol estan elegidos.
 *
 * <h2>Se guardan caminos, no filas</h2>
 *
 * <p>Un camino sigue siendo el mismo si se despliega o se pliega algo mas arriba; una fila no. Por
 * eso la seleccion son {@link TreePath}, y las filas se calculan cuando hacen falta preguntandole
 * al {@link RowMapper}. Ver la nota de esa interfaz.
 *
 * <h2>Tres modos, y el del medio es el raro</h2>
 *
 * <p>Uno solo, varios contiguos, o cualquiera. El contiguo pide que las filas elegidas sean
 * seguidas, y eso depende de lo que este desplegado: plegar un nodo del medio puede volver contigua
 * una seleccion que no lo era. Es la razon de que exista {@link #resetRowSelection}, que la vista
 * llama cuando cambia lo desplegado.
 */
public interface TreeSelectionModel {

    /** Un solo nodo. */
    int SINGLE_TREE_SELECTION = 1;

    /** Varios, pero en filas seguidas. */
    int CONTIGUOUS_TREE_SELECTION = 2;

    /** Cualquier conjunto. */
    int DISCONTIGUOUS_TREE_SELECTION = 4;

    void setSelectionMode(int mode);

    int getSelectionMode();

    /** Deja elegido solo ese camino. */
    void setSelectionPath(TreePath path);

    void setSelectionPaths(TreePath[] paths);

    void addSelectionPath(TreePath path);

    void addSelectionPaths(TreePath[] paths);

    void removeSelectionPath(TreePath path);

    void removeSelectionPaths(TreePath[] paths);

    /** El primero de los elegidos, o nulo. */
    TreePath getSelectionPath();

    TreePath[] getSelectionPaths();

    int getSelectionCount();

    boolean isPathSelected(TreePath path);

    boolean isSelectionEmpty();

    void clearSelection();

    /** Quien traduce caminos a filas; ver la nota de la interfaz. */
    void setRowMapper(RowMapper newMapper);

    RowMapper getRowMapper();

    int[] getSelectionRows();

    int getMinSelectionRow();

    int getMaxSelectionRow();

    boolean isRowSelected(int row);

    /** Vuelve a calcular las filas; la llama la vista al cambiar lo desplegado. */
    void resetRowSelection();

    int getLeadSelectionRow();

    TreePath getLeadSelectionPath();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    void addTreeSelectionListener(TreeSelectionListener x);

    void removeTreeSelectionListener(TreeSelectionListener x);
}
