package javax.swing.plaf;

import java.awt.Rectangle;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * El aspecto de un {@link JTree}.
 *
 * <h2>Geometria y edicion</h2>
 *
 * <p>Los cinco primeros metodos traducen entre caminos, filas y puntos. El arbol no puede hacerlo:
 * en que fila cae un camino depende de que este desplegado y de cuanto mide cada nodo, y las dos
 * cosas las decide el aspecto.
 *
 * <p>Los otros cinco son sobre la edicion en el lugar. Tambien es del aspecto porque el editor es un
 * componente que el aspecto agrega y saca del arbol.
 */
public abstract class TreeUI extends ComponentUI {

    protected TreeUI() {
    }

    /** El rectangulo de ese camino, o nulo si no se ve. */
    public abstract Rectangle getPathBounds(JTree tree, TreePath path);

    public abstract TreePath getPathForRow(JTree tree, int row);

    /** La fila de ese camino, o -1 si no se ve. */
    public abstract int getRowForPath(JTree tree, TreePath path);

    /** Cuantas filas se ven. */
    public abstract int getRowCount(JTree tree);

    /** El camino mas cercano a ese punto, aunque el punto no caiga sobre ninguno. */
    public abstract TreePath getClosestPathForLocation(JTree tree, int x, int y);

    public abstract boolean isEditing(JTree tree);

    /** Termina la edicion guardando; devuelve si habia una. */
    public abstract boolean stopEditing(JTree tree);

    public abstract void cancelEditing(JTree tree);

    public abstract void startEditingAtPath(JTree tree, TreePath path);

    public abstract TreePath getEditingPath(JTree tree);
}
