package javax.swing.event;

import java.util.EventObject;

import javax.swing.tree.TreePath;

/**
 * Una rama del arbol se abrio o se cerro.
 *
 * <p>Lleva el camino y no el nodo, por la razon de siempre en un arbol: un mismo objeto puede colgar
 * de dos lugares, y solo el camino dice cual se abrio. Ver {@link TreePath}.
 *
 * <p>Cual de las dos cosas paso lo dice el metodo del {@link TreeExpansionListener} al que llega.
 */
public class TreeExpansionEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** El camino de la rama. */
    protected TreePath path;

    public TreeExpansionEvent(Object source, TreePath path) {
        super(source);
        this.path = path;
    }

    /** El camino de la rama que se abrio o se cerro. */
    public TreePath getPath() {
        return this.path;
    }
}
