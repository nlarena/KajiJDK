package javax.swing.tree;

import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.event.TreeModelEvent;

/**
 * La cuenta de filas para un arbol donde cada fila mide lo suyo.
 *
 * <h2>Que la distingue de la otra</h2>
 *
 * <p>{@link FixedHeightLayoutCache} saca la posicion de una fila con una multiplicacion. Esta tiene
 * que preguntarle a cada nodo cuanto mide y sumar desde arriba, lo que la hace mas cara y la unica
 * que sirve cuando las filas tienen iconos de distinto tamano o texto de varias lineas.
 *
 * <p>La otra mitad -- que nodos se ven y en que orden -- es identica; ver la nota de
 * {@link AbstractLayoutCache}.
 *
 * <h2>Sin medidor no hay alturas</h2>
 *
 * <p>Si nadie puso un {@link AbstractLayoutCache.NodeDimensions} y no hay altura fija, no hay de
 * donde sacar cuanto mide una fila: {@link #getBounds} devuelve un rectangulo vacio -- y no nulo,
 * que es lo que hace la otra cache -- y las cuentas de filas siguen andando. Se puede traducir entre
 * filas y caminos sin haber dibujado nada.
 */
public class VariableHeightLayoutCache extends AbstractLayoutCache {

    private final NucleoDeCache nucleo = new NucleoDeCache();

    /** Una cache vacia. */
    public VariableHeightLayoutCache() {
        super();
    }

    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
        nucleo.setModelo(newModel);
        if (newModel != null && newModel.getRoot() != null) {
            setExpandedState(new TreePath(newModel.getRoot()), true);
        }
    }

    public void setRootVisible(boolean rootVisible) {
        if (isRootVisible() != rootVisible) {
            super.setRootVisible(rootVisible);
            nucleo.setRaizVisible(rootVisible);
        }
    }

    public void setRowHeight(int rowHeight) {
        if (rowHeight != getRowHeight()) {
            super.setRowHeight(rowHeight);
            invalidateSizes();
        }
    }

    public void setNodeDimensions(NodeDimensions nd) {
        super.setNodeDimensions(nd);
        invalidateSizes();
    }

    public void setExpandedState(TreePath path, boolean isExpanded) {
        nucleo.setDesplegado(path, isExpanded);
    }

    public boolean getExpandedState(TreePath path) {
        return nucleo.desplegadoDeVerdad(path);
    }

    /**
     * Donde va ese nodo.
     *
     * <p>La posicion vertical se acumula sumando lo que mide cada fila anterior. Con altura fija
     * puesta se usa esa y no se pregunta.
     */
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        int row = getRowForPath(path);
        if (row < 0) {
            return null;
        }
        Rectangle r = medir(row, placeIn);
        if (r == null) {
            // Sin medidor no hay ancho, pero si hay fila: se devuelve un rectangulo vacio y no
            // nulo. Esta medido, y es distinto de lo que hace la cache de altura fija, que si
            // devuelve nulo. La asimetria es del JDK.
            r = (placeIn != null) ? placeIn : new Rectangle();
            r.x = 0;
            r.width = 0;
            r.height = 0;
        }
        r.y = arriba(row);
        if (isFixedRowHeight()) {
            r.height = getRowHeight();
        }
        return r;
    }

    /** Lo que ocupa esa fila, preguntandole al medidor. */
    private Rectangle medir(int row, Rectangle placeIn) {
        TreePath path = nucleo.caminoDeFila(row);
        if (path == null) {
            return null;
        }
        return getNodeDimensions(path.getLastPathComponent(), row, path.getPathCount() - 1,
                nucleo.estaMarcado(path), placeIn);
    }

    /** Donde empieza esa fila: la suma de lo que miden las anteriores. */
    private int arriba(int row) {
        if (isFixedRowHeight()) {
            return row * getRowHeight();
        }
        int y = 0;
        for (int i = 0; i < row; i++) {
            Rectangle r = medir(i, null);
            if (r != null) {
                y = y + r.height;
            }
        }
        return y;
    }

    public TreePath getPathForRow(int row) {
        return nucleo.caminoDeFila(row);
    }

    public int getRowForPath(TreePath path) {
        return nucleo.filaDeCamino(path);
    }

    public int getRowCount() {
        return nucleo.cuantas();
    }

    /** No guarda medidas: cada consulta vuelve a preguntar. */
    public void invalidatePathBounds(TreePath path) {
    }

    /** Idem; ver {@link #invalidatePathBounds}. */
    public void invalidateSizes() {
    }

    public int getPreferredHeight() {
        int n = getRowCount();
        if (n == 0) {
            return 0;
        }
        if (isFixedRowHeight()) {
            return n * getRowHeight();
        }
        int y = 0;
        for (int i = 0; i < n; i++) {
            Rectangle r = medir(i, null);
            if (r != null) {
                y = y + r.height;
            }
        }
        return y;
    }

    public int getPreferredWidth(Rectangle bounds) {
        return super.getPreferredWidth(bounds);
    }

    /**
     * El nodo mas cercano a ese punto.
     *
     * <p>Como en la otra cache, la coordenada horizontal no se mira.
     */
    public TreePath getPathClosestTo(int x, int y) {
        int n = getRowCount();
        if (n == 0) {
            return null;
        }
        if (y < 0) {
            return getPathForRow(0);
        }
        if (isFixedRowHeight()) {
            int row = y / getRowHeight();
            if (row >= n) {
                row = n - 1;
            }
            return getPathForRow(row);
        }
        int acumulado = 0;
        for (int i = 0; i < n; i++) {
            Rectangle r = medir(i, null);
            int alto = (r == null) ? 0 : r.height;
            if (y < acumulado + alto) {
                return getPathForRow(i);
            }
            acumulado = acumulado + alto;
        }
        return getPathForRow(n - 1);
    }

    public Enumeration<TreePath> getVisiblePathsFrom(TreePath path) {
        return nucleo.desde(path);
    }

    public int getVisibleChildCount(TreePath path) {
        return nucleo.hijosVisibles(path);
    }

    public boolean isExpanded(TreePath path) {
        return nucleo.estaMarcado(path);
    }

    /** Un nodo que cambia puede cambiar de alto, asi que todo lo de abajo se corre. */
    public void treeNodesChanged(TreeModelEvent e) {
        nucleo.invalidar();
    }

    public void treeNodesInserted(TreeModelEvent e) {
        nucleo.invalidar();
    }

    public void treeNodesRemoved(TreeModelEvent e) {
        nucleo.invalidar();
    }

    public void treeStructureChanged(TreeModelEvent e) {
        nucleo.invalidar();
    }
}
