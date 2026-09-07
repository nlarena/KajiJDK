package javax.swing.tree;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 * Un modelo de arbol sobre nodos {@link TreeNode}.
 *
 * <h2>Dos formas de decir que algo es hoja</h2>
 *
 * <p>{@link #setAsksAllowsChildren} elige entre preguntar si el nodo <em>permite</em> hijos o si
 * <em>tiene</em>. La diferencia se ve en una carpeta vacia: preguntando por permitir, se dibuja con
 * el triangulito de desplegar; preguntando por tener, se ve como un archivo.
 *
 * <p>Por omision pregunta si tiene, que es lo que quiere un arbol de datos donde no hay contenedores
 * vacios. Un arbol de archivos quiere lo otro.
 *
 * <h2>Cambiar el arbol no alcanza</h2>
 *
 * <p>Se puede cambiar un {@link DefaultMutableTreeNode} directamente, pero entonces el modelo no se
 * entera y no avisa. Los metodos {@code insertNodeInto}, {@code removeNodeFromParent} y
 * {@code nodeChanged} hacen las dos cosas: cambian y avisan.
 *
 * <p>Los {@code nodesWere...} son para el caso al reves: el arbol ya cambio por afuera y solo falta
 * avisar. Sirven cuando el cambio fue grande y conviene hacerlo de una y avisar una sola vez.
 */
public class DefaultTreeModel implements Serializable, TreeModel {

    /** La raiz del arbol. */
    protected TreeNode root;

    /** Quienes escuchan. */
    protected EventListenerList listenerList = new EventListenerList();

    /** Si ser hoja se decide por permitir hijos; ver la nota de la clase. */
    protected boolean asksAllowsChildren;

    /** Un modelo sobre ese arbol, que decide hoja por tener hijos. */
    public DefaultTreeModel(TreeNode root) {
        this(root, false);
    }

    /** Un modelo sobre ese arbol, eligiendo como se decide que es hoja. */
    public DefaultTreeModel(TreeNode root, boolean asksAllowsChildren) {
        this.root = root;
        this.asksAllowsChildren = asksAllowsChildren;
    }

    public void setAsksAllowsChildren(boolean newValue) {
        asksAllowsChildren = newValue;
    }

    public boolean asksAllowsChildren() {
        return asksAllowsChildren;
    }

    /** Cambia la raiz; el arbol entero se rearma. */
    public void setRoot(TreeNode root) {
        Object oldRoot = this.root;
        this.root = root;
        if (root == null && oldRoot != null) {
            fireTreeStructureChanged(this, null);
        } else {
            nodeStructureChanged(root);
        }
    }

    public Object getRoot() {
        return root;
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) {
            return -1;
        }
        return ((TreeNode) parent).getIndex((TreeNode) child);
    }

    public Object getChild(Object parent, int index) {
        return ((TreeNode) parent).getChildAt(index);
    }

    public int getChildCount(Object parent) {
        return ((TreeNode) parent).getChildCount();
    }

    /** Si es hoja; la regla la elige {@link #setAsksAllowsChildren}. */
    public boolean isLeaf(Object node) {
        if (asksAllowsChildren) {
            return !((TreeNode) node).getAllowsChildren();
        }
        return ((TreeNode) node).isLeaf();
    }

    /** Avisa que todo el arbol cambio. */
    public void reload() {
        reload(root);
    }

    /**
     * El usuario edito un nodo.
     *
     * <p>Guarda el valor en el nodo y avisa. Que el modelo lo haga y no el editor es lo que permite
     * que un modelo sobre datos ajenos traduzca antes de guardar.
     */
    public void valueForPathChanged(TreePath path, Object newValue) {
        MutableTreeNode aNode = (MutableTreeNode) path.getLastPathComponent();
        aNode.setUserObject(newValue);
        nodeChanged(aNode);
    }

    /** Inserta un nodo y avisa. */
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        parent.insert(newChild, index);
        int[] newIndexs = new int[1];
        newIndexs[0] = index;
        nodesWereInserted(parent, newIndexs);
    }

    /**
     * Saca un nodo y avisa.
     *
     * @throws IllegalArgumentException si el nodo no tiene padre.
     */
    public void removeNodeFromParent(MutableTreeNode node) {
        MutableTreeNode parent = (MutableTreeNode) node.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("node does not have a parent.");
        }
        int[] childIndex = new int[1];
        Object[] removedArray = new Object[1];
        childIndex[0] = parent.getIndex(node);
        parent.remove(childIndex[0]);
        removedArray[0] = node;
        nodesWereRemoved(parent, childIndex, removedArray);
    }

    /** Avisa que cambio lo que muestra ese nodo, no su estructura. */
    public void nodeChanged(TreeNode node) {
        if (listenerList != null && node != null) {
            TreeNode parent = node.getParent();
            if (parent != null) {
                int anIndex = parent.getIndex(node);
                if (anIndex != -1) {
                    int[] cIndexs = new int[1];
                    cIndexs[0] = anIndex;
                    nodesChanged(parent, cIndexs);
                }
            } else if (node == getRoot()) {
                nodesChanged(node, null);
            }
        }
    }

    /**
     * Avisa que el subarbol de ese nodo cambio entero.
     *
     * <p>Es el aviso mas caro: la vista tira todo lo que sabia de ese subarbol y lo vuelve a armar,
     * incluido que estaba desplegado. Conviene solo cuando el cambio es grande.
     */
    public void reload(TreeNode node) {
        if (node != null) {
            fireTreeStructureChanged(this, getPathToRoot(node), null, null);
        }
    }

    /** Avisa que se insertaron esos hijos, que ya estan en el arbol. */
    public void nodesWereInserted(TreeNode node, int[] childIndices) {
        if (listenerList != null && node != null && childIndices != null
                && childIndices.length > 0) {
            int cCount = childIndices.length;
            Object[] newChildren = new Object[cCount];
            for (int counter = 0; counter < cCount; counter++) {
                newChildren[counter] = node.getChildAt(childIndices[counter]);
            }
            fireTreeNodesInserted(this, getPathToRoot(node), childIndices, newChildren);
        }
    }

    /**
     * Avisa que se sacaron esos hijos.
     *
     * <p>Hay que pasar los nodos sacados porque ya no estan en el arbol: quien escuche no los podria
     * conseguir de otro lado, y los necesita para limpiar lo que tuviera guardado de ellos.
     */
    public void nodesWereRemoved(TreeNode node, int[] childIndices, Object[] removedChildren) {
        if (node != null && childIndices != null) {
            fireTreeNodesRemoved(this, getPathToRoot(node), childIndices, removedChildren);
        }
    }

    /** Avisa que esos hijos cambiaron lo que muestran. */
    public void nodesChanged(TreeNode node, int[] childIndices) {
        if (node != null) {
            if (childIndices != null) {
                int cCount = childIndices.length;
                if (cCount > 0) {
                    Object[] cChildren = new Object[cCount];
                    for (int counter = 0; counter < cCount; counter++) {
                        cChildren[counter] = node.getChildAt(childIndices[counter]);
                    }
                    fireTreeNodesChanged(this, getPathToRoot(node), childIndices, cChildren);
                }
            } else if (node == getRoot()) {
                fireTreeNodesChanged(this, getPathToRoot(node), null, null);
            }
        }
    }

    public void nodeStructureChanged(TreeNode node) {
        if (node != null) {
            fireTreeStructureChanged(this, getPathToRoot(node), null, null);
        }
    }

    /** El camino desde la raiz hasta ese nodo. */
    public TreeNode[] getPathToRoot(TreeNode aNode) {
        return getPathToRoot(aNode, 0);
    }

    /**
     * Arma el camino subiendo; ver {@link DefaultMutableTreeNode#getPathToRoot}.
     *
     * <p>Si el nodo no llega a la raiz de este modelo, el camino sale igual pero incompleto: el
     * modelo no puede saber si el arbol se rearmo debajo de el.
     */
    protected TreeNode[] getPathToRoot(TreeNode aNode, int depth) {
        TreeNode[] retNodes;
        if (aNode == null) {
            if (depth == 0) {
                return null;
            }
            retNodes = new TreeNode[depth];
        } else {
            depth++;
            if (aNode == root) {
                retNodes = new TreeNode[depth];
            } else {
                retNodes = getPathToRoot(aNode.getParent(), depth);
            }
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }

    public void addTreeModelListener(TreeModelListener l) {
        listenerList.add(TreeModelListener.class, l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listenerList.remove(TreeModelListener.class, l);
    }

    public TreeModelListener[] getTreeModelListeners() {
        return listenerList.getListeners(TreeModelListener.class);
    }

    protected void fireTreeNodesChanged(Object source, Object[] path, int[] childIndices,
            Object[] children) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path, childIndices, children);
                }
                ((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
            }
        }
    }

    protected void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices,
            Object[] children) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path, childIndices, children);
                }
                ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
            }
        }
    }

    protected void fireTreeNodesRemoved(Object source, Object[] path, int[] childIndices,
            Object[] children) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path, childIndices, children);
                }
                ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
            }
        }
    }

    protected void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices,
            Object[] children) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path, childIndices, children);
                }
                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }

    /** El aviso de estructura con un camino ya armado. */
    private void fireTreeStructureChanged(Object source, TreePath path) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path);
                }
                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
