package javax.swing.event;

import java.util.EventObject;

import javax.swing.tree.TreePath;

/**
 * El arbol cambio.
 *
 * <h2>El camino es al PADRE, no a lo que cambio</h2>
 *
 * <p>Es la trampa de esta clase. {@link #getTreePath} devuelve el camino al nodo <em>cuyos hijos</em>
 * cambiaron, y {@link #getChildIndices} dice cuales. Leerlo como "el nodo que cambio" da el resultado
 * equivocado en todos los casos menos uno.
 *
 * <p>La excepcion es {@code treeStructureChanged}, donde el camino apunta a la raiz del subarbol que
 * se rehizo entero y los indices son {@code null}: no hay hijos que enumerar porque cambiaron todos.
 */
public class TreeModelEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** El camino al padre. */
    protected TreePath path;

    /** Que hijos cambiaron, en orden creciente. */
    protected int[] childIndices;

    /** Los hijos que cambiaron. */
    protected Object[] children;

    /** Con el camino como arreglo de nodos. */
    public TreeModelEvent(Object source, Object[] path, int[] childIndices, Object[] children) {
        this(source, path == null ? null : new TreePath(path), childIndices, children);
    }

    /** Con el camino como {@link TreePath}. */
    public TreeModelEvent(Object source, TreePath path, int[] childIndices, Object[] children) {
        super(source);
        this.path = path;
        this.childIndices = childIndices;
        this.children = children;
    }

    /** Cambio la estructura debajo de ese camino, dado como arreglo. */
    public TreeModelEvent(Object source, Object[] path) {
        this(source, path == null ? null : new TreePath(path));
    }

    /** Cambio la estructura debajo de ese camino. */
    public TreeModelEvent(Object source, TreePath path) {
        super(source);
        this.path = path;
        this.childIndices = new int[0];
    }

    /** El camino al padre; ver la nota de la clase. */
    public TreePath getTreePath() {
        return this.path;
    }

    /** El mismo camino, como arreglo de nodos. */
    public Object[] getPath() {
        if (this.path != null) {
            return this.path.getPath();
        }
        return null;
    }

    /** Los hijos que cambiaron, en un arreglo nuevo. */
    public Object[] getChildren() {
        if (this.children == null) {
            return null;
        }
        Object[] copia = new Object[this.children.length];
        for (int i = 0; i < this.children.length; i++) {
            copia[i] = this.children[i];
        }
        return copia;
    }

    /** Los indices de los hijos que cambiaron, en un arreglo nuevo. */
    public int[] getChildIndices() {
        if (this.childIndices == null) {
            return null;
        }
        int[] copia = new int[this.childIndices.length];
        for (int i = 0; i < this.childIndices.length; i++) {
            copia[i] = this.childIndices[i];
        }
        return copia;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName() + " " + String.valueOf(hashCode()));
        if (this.path != null) {
            sb.append(" path " + this.path);
        }
        if (this.childIndices != null) {
            sb.append(" indices [");
            for (int i = 0; i < this.childIndices.length; i++) {
                sb.append(" " + String.valueOf(this.childIndices[i]));
            }
            sb.append(" ]");
        }
        return sb.toString();
    }
}
