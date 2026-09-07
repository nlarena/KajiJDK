package javax.swing.tree;

import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.event.TreeModelEvent;

/**
 * La cuenta de filas para un arbol donde todas miden lo mismo.
 *
 * <h2>Por que hay dos caches</h2>
 *
 * <p>Con todas las filas del mismo alto, saber en que fila cae un pixel es una division y saber
 * donde empieza una fila es una multiplicacion. Eso vale para un arbol de cualquier tamano y sin
 * medir un solo nodo. La otra cache -- {@link VariableHeightLayoutCache} -- tiene que preguntarle a
 * cada nodo cuanto mide y sumar.
 *
 * <p>El resto -- que nodos se ven, en que orden, cual es la fila de cual -- es igual en las dos.
 */
public class FixedHeightLayoutCache extends AbstractLayoutCache {

    private final NucleoDeCache nucleo = new NucleoDeCache();

    /** Una cache vacia. */
    public FixedHeightLayoutCache() {
        super();
        setRowHeight(1);
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
        if (rowHeight <= 0) {
            throw new IllegalArgumentException(
                    "FixedHeightLayoutCache only supports row heights greater than 0");
        }
        if (getRowHeight() != rowHeight) {
            super.setRowHeight(rowHeight);
        }
    }

    public int getRowCount() {
        return nucleo.cuantas();
    }

    /** No hace nada: con altura fija no hay medida guardada que tirar. */
    public void invalidatePathBounds(TreePath path) {
    }

    /** No hace nada; ver {@link #invalidatePathBounds}. */
    public void invalidateSizes() {
    }

    public boolean isExpanded(TreePath path) {
        return nucleo.estaMarcado(path);
    }

    /**
     * Donde va ese nodo.
     *
     * <p>El alto y la posicion vertical salen de la fila; el ancho y la posicion horizontal, del
     * medidor. Sin medidor no hay ancho que dar y devuelve nulo.
     */
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        int row = getRowForPath(path);
        if (row < 0) {
            return null;
        }
        Object nodo = path.getLastPathComponent();
        Rectangle r = getNodeDimensions(nodo, row, path.getPathCount() - 1,
                nucleo.estaMarcado(path), placeIn);
        if (r == null) {
            return null;
        }
        r.y = row * getRowHeight();
        r.height = getRowHeight();
        return r;
    }

    public TreePath getPathForRow(int row) {
        return nucleo.caminoDeFila(row);
    }

    public int getRowForPath(TreePath path) {
        return nucleo.filaDeCamino(path);
    }

    /**
     * El nodo mas cercano a ese punto.
     *
     * <p>La coordenada horizontal no se mira: una fila ocupa todo el ancho, aunque su dibujo no.
     * Un clic a la derecha del texto sigue siendo un clic en esa fila.
     */
    public TreePath getPathClosestTo(int x, int y) {
        int n = getRowCount();
        if (n == 0) {
            return null;
        }
        int row = y / getRowHeight();
        if (row < 0) {
            row = 0;
        } else if (row >= n) {
            row = n - 1;
        }
        return getPathForRow(row);
    }

    public int getVisibleChildCount(TreePath path) {
        return nucleo.hijosVisibles(path);
    }

    public Enumeration<TreePath> getVisiblePathsFrom(TreePath path) {
        return nucleo.desde(path);
    }

    public void setExpandedState(TreePath path, boolean isExpanded) {
        nucleo.setDesplegado(path, isExpanded);
    }

    public boolean getExpandedState(TreePath path) {
        return nucleo.desplegadoDeVerdad(path);
    }

    /** Un nodo que cambia no cambia ninguna fila: con altura fija, nada se mueve. */
    public void treeNodesChanged(TreeModelEvent e) {
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
