package javax.swing.tree;

import javax.swing.event.TreeModelListener;

/**
 * Los datos de un arbol.
 *
 * <h2>Los nodos son {@code Object}</h2>
 *
 * <p>El modelo no pide ninguna interfaz para sus nodos: son objetos cualquiera, y el modelo es
 * quien sabe pedirles hijos. Es lo que permite mostrar un arbol de archivos, uno de XML o uno de
 * una base de datos sin envolver cada nodo en una clase de Swing.
 *
 * <p>{@link DefaultTreeModel} es el caso facil, donde los nodos si son {@link TreeNode}.
 *
 * <h2>Por que hay que avisar</h2>
 *
 * <p>Un arbol muestra solo lo desplegado y recuerda que fila es cada nodo. Un cambio que no se
 * avise deja esa cuenta vieja, y a partir de ahi el arbol muestra un nodo y elige otro.
 */
public interface TreeModel {

    /** La raiz; nulo significa arbol vacio. */
    Object getRoot();

    /** El hijo numero tal de ese nodo. */
    Object getChild(Object parent, int index);

    int getChildCount(Object parent);

    /**
     * Si ese nodo no puede tener hijos.
     *
     * <p>Es distinto de no tener ninguno: una carpeta vacia no es una hoja, y por eso se dibuja
     * con el triangulito de desplegar.
     */
    boolean isLeaf(Object node);

    /** El usuario edito el nodo de ese camino y quedo con ese valor. */
    void valueForPathChanged(TreePath path, Object newValue);

    /** En que posicion esta ese hijo, o -1. */
    int getIndexOfChild(Object parent, Object child);

    void addTreeModelListener(TreeModelListener l);

    void removeTreeModelListener(TreeModelListener l);
}
