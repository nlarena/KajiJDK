package javax.swing.tree;

import java.util.Enumeration;

/**
 * Un nodo de arbol visto desde afuera: hijos, padre y poco mas.
 *
 * <p>Es de solo lectura a proposito. Un {@code JTree} no arma el arbol, lo muestra; quien lo arma
 * usa {@code MutableTreeNode} o su propio modelo. Que la estructura de un documento de texto
 * implemente esta interfaz es lo que permite mirarla con un arbol sin convertir nada.
 *
 * <p>{@link #getAllowsChildren} y {@link #isLeaf} no son lo mismo: una carpeta vacia <em>admite</em>
 * hijos y no tiene ninguno, y esa diferencia es la que decide si se dibuja el triangulito para
 * desplegarla.
 */
public interface TreeNode {

    TreeNode getChildAt(int childIndex);

    int getChildCount();

    /** El padre, o {@code null} si es la raiz. */
    TreeNode getParent();

    /** La posicion de ese nodo entre los hijos, o {@code -1} si no es hijo de este. */
    int getIndex(TreeNode node);

    /** Si admite hijos; ver la nota de la interfaz. */
    boolean getAllowsChildren();

    boolean isLeaf();

    Enumeration<? extends TreeNode> children();
}
