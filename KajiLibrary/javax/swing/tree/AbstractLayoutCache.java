package javax.swing.tree;

import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.event.TreeModelEvent;

/**
 * La cuenta de que fila corresponde a que nodo del arbol.
 *
 * <h2>Filas y caminos son dos numeraciones distintas</h2>
 *
 * <p>Un {@link TreePath} identifica un nodo y no cambia nunca. Una <em>fila</em> es donde ese nodo
 * aparece en pantalla, y cambia cada vez que se despliega o se pliega algo mas arriba. Traducir
 * entre las dos es todo lo que hace esta clase, y es lo que permite que el modelo del arbol no sepa
 * nada de pantallas.
 *
 * <h2>Los nodos plegados no ocupan fila</h2>
 *
 * <p>Un nodo existe en el modelo aunque su padre este plegado; simplemente no tiene fila. De ahi que
 * {@link #getRowForPath} devuelva -1 para lo que no se ve, y no un error: no verse es un estado
 * normal, no una equivocacion.
 *
 * <h2>Dos implementaciones, y la diferencia es una sola</h2>
 *
 * <p>{@link FixedHeightLayoutCache} sirve cuando todas las filas miden lo mismo -- entonces la fila
 * de un pixel es una division -- y {@link VariableHeightLayoutCache} cuando no. Todo lo demas es
 * igual, y por eso esta clase existe: para que el arbol no tenga que saber cual le toco.
 *
 * <h2>Quien mide</h2>
 *
 * <p>Esta clase no sabe dibujar ni medir texto. Le pregunta a un {@link NodeDimensions}, que le pone
 * el aspecto. Sin uno, las medidas salen vacias y las cuentas de filas siguen andando: separar las
 * dos cosas es justamente lo que permite probar la traduccion sin una pantalla.
 */
public abstract class AbstractLayoutCache implements RowMapper {

    /** Quien sabe cuanto ocupa cada nodo; ver la nota de la clase. */
    protected NodeDimensions nodeDimensions;

    /** El modelo del arbol. */
    protected TreeModel treeModel;

    /** La seleccion, para que las cuentas de filas la puedan avisar. */
    protected TreeSelectionModel treeSelectionModel;

    /** Si la raiz ocupa una fila. */
    protected boolean rootVisible;

    /** El alto de cada fila, o cero si cada una mide lo suyo. */
    protected int rowHeight;

    /** Para las subclases. */
    protected AbstractLayoutCache() {
    }

    public void setNodeDimensions(NodeDimensions nd) {
        this.nodeDimensions = nd;
    }

    public NodeDimensions getNodeDimensions() {
        return nodeDimensions;
    }

    public void setModel(TreeModel newModel) {
        treeModel = newModel;
    }

    public TreeModel getModel() {
        return treeModel;
    }

    /**
     * Si la raiz ocupa una fila.
     *
     * <p>Con la raiz escondida, los hijos de la raiz son las filas de primer nivel. Es como se
     * muestra un arbol que en realidad es un bosque.
     */
    public void setRootVisible(boolean rootVisible) {
        this.rootVisible = rootVisible;
    }

    public boolean isRootVisible() {
        return rootVisible;
    }

    /**
     * El alto de cada fila.
     *
     * <p>Cero o menos significa "cada una mide lo suyo", y es lo que obliga a preguntarle a cada
     * nodo. Un numero positivo hace que la fila de un pixel sea una division.
     */
    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setSelectionModel(TreeSelectionModel newLSM) {
        if (treeSelectionModel != null) {
            treeSelectionModel.setRowMapper(null);
        }
        treeSelectionModel = newLSM;
        if (treeSelectionModel != null) {
            treeSelectionModel.setRowMapper(this);
        }
    }

    public TreeSelectionModel getSelectionModel() {
        return treeSelectionModel;
    }

    /** Lo que ocupan todas las filas juntas. */
    public int getPreferredHeight() {
        int rowCount = getRowCount();
        if (rowCount > 0) {
            Rectangle bounds = getBounds(getPathForRow(rowCount - 1), null);
            if (bounds != null) {
                return bounds.y + bounds.height;
            }
        }
        return 0;
    }

    /**
     * Lo que ocupa la fila mas ancha de las que caen en ese rectangulo.
     *
     * <p>Con un rectangulo nulo mira todas. Mirar solo las visibles es lo que hace que un arbol de
     * cien mil nodos no tenga que medirlos todos para saber cuanto scroll horizontal hace falta.
     */
    public int getPreferredWidth(Rectangle bounds) {
        int rowCount = getRowCount();
        if (rowCount > 0) {
            int maxWidth = 0;
            int i = 0;
            int last = rowCount;
            if (bounds != null) {
                TreePath first = getPathClosestTo(bounds.x, bounds.y);
                i = (first == null) ? 0 : getRowForPath(first);
                if (i < 0) {
                    i = 0;
                }
                TreePath fin = getPathClosestTo(bounds.x, bounds.y + bounds.height);
                int r = (fin == null) ? rowCount - 1 : getRowForPath(fin);
                last = (r < 0) ? rowCount : r + 1;
            }
            while (i < last) {
                Rectangle b = getBounds(getPathForRow(i), null);
                if (b != null && b.x + b.width > maxWidth) {
                    maxWidth = b.x + b.width;
                }
                i = i + 1;
            }
            return maxWidth;
        }
        return 0;
    }

    /** Si ese camino esta desplegado. */
    public abstract boolean isExpanded(TreePath path);

    /** Donde va ese nodo, o nulo si no se ve. */
    public abstract Rectangle getBounds(TreePath path, Rectangle placeIn);

    /** El nodo de esa fila, o nulo si esa fila no existe. */
    public abstract TreePath getPathForRow(int row);

    /** La fila de ese nodo, o -1 si no se ve; ver la nota de la clase. */
    public abstract int getRowForPath(TreePath path);

    /** El nodo mas cercano a ese punto. */
    public abstract TreePath getPathClosestTo(int x, int y);

    /** Los nodos visibles desde ese, hacia abajo. */
    public abstract Enumeration<TreePath> getVisiblePathsFrom(TreePath path);

    /** Cuantas filas ocupan los descendientes visibles de ese nodo. */
    public abstract int getVisibleChildCount(TreePath path);

    /** Despliega o pliega ese camino. */
    public abstract void setExpandedState(TreePath path, boolean isExpanded);

    /** Si ese camino esta desplegado y todos sus padres tambien. */
    public abstract boolean getExpandedState(TreePath path);

    /** Cuantas filas hay. */
    public abstract int getRowCount();

    /** Olvida todas las medidas guardadas. */
    public abstract void invalidateSizes();

    /** Olvida la medida de ese nodo. */
    public abstract void invalidatePathBounds(TreePath path);

    /** Cambiaron esos nodos. */
    public abstract void treeNodesChanged(TreeModelEvent e);

    /** Se agregaron esos nodos. */
    public abstract void treeNodesInserted(TreeModelEvent e);

    /** Se sacaron esos nodos. */
    public abstract void treeNodesRemoved(TreeModelEvent e);

    /** Cambio la estructura debajo de ese nodo. */
    public abstract void treeStructureChanged(TreeModelEvent e);

    /**
     * Las filas de esos caminos.
     *
     * <p>Es lo que le da un {@link RowMapper} al modelo de seleccion. Un arreglo vacio si no hay
     * filas, no nulo: el que llama va a recorrerlo.
     */
    public int[] getRowsForPaths(TreePath[] paths) {
        if (paths == null) {
            return null;
        }
        int numPaths = paths.length;
        int[] rows = new int[numPaths];
        for (int counter = 0; counter < numPaths; counter++) {
            rows[counter] = getRowForPath(paths[counter]);
        }
        return rows;
    }

    /** Le pregunta al medidor; rectangulo vacio si no hay ninguno puesto. */
    protected Rectangle getNodeDimensions(Object value, int row, int depth,
            boolean expanded, Rectangle placeIn) {
        NodeDimensions nd = getNodeDimensions();
        if (nd != null) {
            return nd.getNodeDimensions(value, row, depth, expanded, placeIn);
        }
        return null;
    }

    /** Si todas las filas miden lo mismo; ver {@link #setRowHeight}. */
    protected boolean isFixedRowHeight() {
        return (rowHeight > 0);
    }

    /**
     * Quien sabe cuanto ocupa un nodo dibujado.
     *
     * <p>Lo pone el aspecto, porque medir depende de la tipografia y de los iconos, que son del
     * aspecto y no del arbol.
     */
    public abstract static class NodeDimensions {

        /** Para las subclases. */
        protected NodeDimensions() {
        }

        /**
         * El rectangulo que ocupa ese nodo.
         *
         * <p>Se le pasa un rectangulo para llenar y evitar crear uno por nodo; con nulo devuelve
         * uno nuevo.
         */
        public abstract Rectangle getNodeDimensions(Object value, int row, int depth,
                boolean expanded, Rectangle bounds);
    }
}
