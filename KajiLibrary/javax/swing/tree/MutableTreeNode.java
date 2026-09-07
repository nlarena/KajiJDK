package javax.swing.tree;

/**
 * Un nodo de arbol al que se le pueden agregar y sacar hijos.
 *
 * <p>{@link TreeNode} solo deja mirar. Esta agrega lo que hace falta para construir el arbol y para
 * moverlo, y es la que espera {@link DefaultTreeModel} cuando se le pide cambiar algo.
 *
 * <p>{@link #removeFromParent} y {@link #setParent} van juntos: mover un nodo es sacarlo de un
 * padre y ponerlo en otro, y hacerlo en un solo paso dejaria al arbol con un nodo en dos lugares.
 */
public interface MutableTreeNode extends TreeNode {

    /** Inserta ese hijo en esa posicion. */
    void insert(MutableTreeNode child, int index);

    void remove(int index);

    void remove(MutableTreeNode node);

    /** El objeto que este nodo representa. */
    void setUserObject(Object object);

    void removeFromParent();

    /** Lo llama el padre; ver la nota de la interfaz. */
    void setParent(MutableTreeNode newParent);
}
