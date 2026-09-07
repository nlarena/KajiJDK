package javax.swing.tree;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Vector;

/**
 * Un nodo de arbol que guarda un objeto y una lista de hijos.
 *
 * <h2>El nodo no es el dato</h2>
 *
 * <p>Cada nodo tiene un {@code userObject}: el dato que representa. El nodo es la estructura -- el
 * padre, los hijos --, el objeto es lo que le importa al programa. Separarlos permite armar un
 * arbol sobre datos que ya existen sin tocarlos.
 *
 * <p>{@link #toString} devuelve el {@code toString} del objeto, no del nodo. Es lo que hace que un
 * arbol de cadenas se vea bien sin escribir un dibujante.
 *
 * <h2>Permitir hijos no es tener hijos</h2>
 *
 * <p>{@link #setAllowsChildren} decide si el nodo es una hoja aunque no tenga hijos ahora. Una
 * carpeta vacia permite hijos y por eso lleva el triangulito de desplegar; un archivo no. Sin esa
 * distincion, una carpeta vacia se veria como un archivo.
 *
 * <h2>Los recorridos</h2>
 *
 * <p>Hay cuatro, y no son lo mismo. En preorden el padre viene antes que sus hijos; en posorden
 * despues; por niveles se recorre fila por fila. El que se quiere casi siempre es preorden, que es
 * el orden en que se ven las filas de un arbol desplegado.
 */
public class DefaultMutableTreeNode implements Cloneable, MutableTreeNode, Serializable {

    /** Un recorrido vacio, para los nodos que no tienen hijos. */
    public static final Enumeration<TreeNode> EMPTY_ENUMERATION = new VacioEnum();

    /** El padre, o nulo si es la raiz. */
    protected MutableTreeNode parent;

    /** Los hijos; nulo mientras no haya ninguno. */
    protected Vector<MutableTreeNode> children;

    /** El dato que este nodo representa. */
    protected transient Object userObject;

    /** Si el nodo puede tener hijos; ver la nota de la clase. */
    protected boolean allowsChildren;

    /** Un nodo sin dato, que permite hijos. */
    public DefaultMutableTreeNode() {
        this(null);
    }

    /** Un nodo con ese dato, que permite hijos. */
    public DefaultMutableTreeNode(Object userObject) {
        this(userObject, true);
    }

    /** Un nodo con ese dato; {@code allowsChildren} decide si es hoja. */
    public DefaultMutableTreeNode(Object userObject, boolean allowsChildren) {
        super();
        parent = null;
        this.allowsChildren = allowsChildren;
        this.userObject = userObject;
    }

    /**
     * Inserta un hijo en esa posicion.
     *
     * <p>Lo saca de su padre anterior primero: un nodo no puede estar en dos lugares, y dejarlo
     * romperia el recorrido hacia arriba.
     *
     * @throws IllegalArgumentException si el nodo es nulo o es un antepasado de este.
     * @throws IllegalStateException si este nodo no permite hijos.
     */
    public void insert(MutableTreeNode newChild, int childIndex) {
        if (!allowsChildren) {
            throw new IllegalStateException("node does not allow children");
        }
        if (newChild == null) {
            throw new IllegalArgumentException("new child is null");
        }
        if (isNodeAncestor(newChild)) {
            throw new IllegalArgumentException("new child is an ancestor");
        }
        MutableTreeNode oldParent = (MutableTreeNode) newChild.getParent();
        if (oldParent != null) {
            oldParent.remove(newChild);
        }
        newChild.setParent(this);
        if (children == null) {
            children = new Vector<MutableTreeNode>();
        }
        children.insertElementAt(newChild, childIndex);
    }

    /** Saca el hijo numero tal. */
    public void remove(int childIndex) {
        MutableTreeNode child = (MutableTreeNode) getChildAt(childIndex);
        children.removeElementAt(childIndex);
        child.setParent(null);
    }

    /** Cambia el padre; lo llama el padre, no quien usa el arbol. */
    public void setParent(MutableTreeNode newParent) {
        parent = newParent;
    }

    public TreeNode getParent() {
        return parent;
    }

    /**
     * El hijo numero tal.
     *
     * @throws ArrayIndexOutOfBoundsException si no existe.
     */
    public TreeNode getChildAt(int index) {
        if (children == null) {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }
        return children.elementAt(index);
    }

    public int getChildCount() {
        if (children == null) {
            return 0;
        }
        return children.size();
    }

    /**
     * En que posicion esta ese hijo, o -1.
     *
     * @throws IllegalArgumentException si el nodo es nulo.
     */
    public int getIndex(TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        if (!isNodeChild(aChild)) {
            return -1;
        }
        return children.indexOf(aChild);
    }

    public Enumeration<TreeNode> children() {
        if (children == null) {
            return EMPTY_ENUMERATION;
        }
        return new HijosEnum(children);
    }

    /** Si el nodo puede tener hijos; ver la nota de la clase. */
    public void setAllowsChildren(boolean allows) {
        if (allows != allowsChildren) {
            allowsChildren = allows;
            if (!allowsChildren) {
                removeAllChildren();
            }
        }
    }

    public boolean getAllowsChildren() {
        return allowsChildren;
    }

    /** El dato que este nodo representa. */
    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    public Object getUserObject() {
        return userObject;
    }

    /** Se saca de su padre. */
    public void removeFromParent() {
        MutableTreeNode parent = (MutableTreeNode) getParent();
        if (parent != null) {
            parent.remove(this);
        }
    }

    /**
     * Saca ese hijo.
     *
     * @throws IllegalArgumentException si no es hijo de este nodo.
     */
    public void remove(MutableTreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        if (!isNodeChild(aChild)) {
            throw new IllegalArgumentException("argument is not a child");
        }
        remove(getIndex(aChild));
    }

    public void removeAllChildren() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            remove(i);
        }
    }

    /** Agrega un hijo al final. */
    public void add(MutableTreeNode newChild) {
        if (newChild != null && newChild.getParent() == this) {
            insert(newChild, getChildCount() - 1);
        } else {
            insert(newChild, getChildCount());
        }
    }

    /** Si ese nodo esta en el camino hacia la raiz desde este. */
    public boolean isNodeAncestor(TreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }
        TreeNode ancestor = this;
        do {
            if (ancestor == anotherNode) {
                return true;
            }
            ancestor = ancestor.getParent();
        } while (ancestor != null);
        return false;
    }

    /** Si este nodo esta en el camino hacia la raiz desde ese. */
    public boolean isNodeDescendant(DefaultMutableTreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }
        return anotherNode.isNodeAncestor(this);
    }

    /** El antepasado mas cercano que los dos comparten, o nulo. */
    public TreeNode getSharedAncestor(DefaultMutableTreeNode aNode) {
        if (aNode == this) {
            return this;
        }
        if (aNode == null) {
            return null;
        }
        int level1 = getLevel();
        int level2 = aNode.getLevel();
        TreeNode node1;
        TreeNode node2;
        int diff;
        if (level2 > level1) {
            diff = level2 - level1;
            node1 = aNode;
            node2 = this;
        } else {
            diff = level1 - level2;
            node1 = this;
            node2 = aNode;
        }
        // Se sube el mas hondo hasta emparejar, y despues los dos a la par.
        while (diff > 0) {
            node1 = node1.getParent();
            diff--;
        }
        do {
            if (node1 == node2) {
                return node1;
            }
            node1 = node1.getParent();
            node2 = node2.getParent();
        } while (node1 != null);
        return null;
    }

    /** Si los dos estan en el mismo arbol. */
    public boolean isNodeRelated(DefaultMutableTreeNode aNode) {
        return (aNode != null) && (getRoot() == aNode.getRoot());
    }

    /** Cuantos niveles hay abajo de este nodo. */
    public int getDepth() {
        Object last = null;
        Enumeration<TreeNode> enum_ = breadthFirstEnumeration();
        while (enum_.hasMoreElements()) {
            last = enum_.nextElement();
        }
        if (last == null) {
            throw new Error("nodes should be null");
        }
        return ((DefaultMutableTreeNode) last).getLevel() - getLevel();
    }

    /** Cuantos niveles hay arriba de este nodo. */
    public int getLevel() {
        TreeNode ancestor = this;
        int levels = 0;
        while ((ancestor = ancestor.getParent()) != null) {
            levels++;
        }
        return levels;
    }

    /** El camino desde la raiz hasta este nodo. */
    public TreeNode[] getPath() {
        return getPathToRoot(this, 0);
    }

    /**
     * Arma el camino subiendo y llenando el arreglo al reves.
     *
     * <p>Se sube contando primero y se llena despues, porque el largo del camino no se sabe hasta
     * llegar a la raiz.
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
            retNodes = getPathToRoot(aNode.getParent(), depth);
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }

    /** Los datos de los nodos del camino, no los nodos. */
    public Object[] getUserObjectPath() {
        TreeNode[] realPath = getPath();
        Object[] retPath = new Object[realPath.length];
        for (int counter = 0; counter < realPath.length; counter++) {
            retPath[counter] = ((DefaultMutableTreeNode) realPath[counter]).getUserObject();
        }
        return retPath;
    }

    public TreeNode getRoot() {
        TreeNode ancestor = this;
        TreeNode previous;
        do {
            previous = ancestor;
            ancestor = ancestor.getParent();
        } while (ancestor != null);
        return previous;
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    /**
     * El nodo que sigue en preorden, o nulo si es el ultimo.
     *
     * <p>Recorre todo el arbol, no solo los hermanos: despues del ultimo hijo viene el hermano del
     * padre.
     */
    public DefaultMutableTreeNode getNextNode() {
        if (getChildCount() == 0) {
            DefaultMutableTreeNode nextSibling = getNextSibling();
            if (nextSibling == null) {
                DefaultMutableTreeNode aNode = (DefaultMutableTreeNode) getParent();
                do {
                    if (aNode == null) {
                        return null;
                    }
                    nextSibling = aNode.getNextSibling();
                    if (nextSibling != null) {
                        return nextSibling;
                    }
                    aNode = (DefaultMutableTreeNode) aNode.getParent();
                } while (true);
            }
            return nextSibling;
        }
        return (DefaultMutableTreeNode) getChildAt(0);
    }

    /** El anterior en preorden, o nulo. */
    public DefaultMutableTreeNode getPreviousNode() {
        DefaultMutableTreeNode previousSibling;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            return null;
        }
        previousSibling = getPreviousSibling();
        if (previousSibling != null) {
            if (previousSibling.getChildCount() == 0) {
                return previousSibling;
            }
            return previousSibling.getLastLeaf();
        }
        return myParent;
    }

    public Enumeration<TreeNode> preorderEnumeration() {
        return new PreordenEnum(this);
    }

    public Enumeration<TreeNode> postorderEnumeration() {
        return new PosordenEnum(this);
    }

    public Enumeration<TreeNode> breadthFirstEnumeration() {
        return new NivelesEnum(this);
    }

    /** Igual que {@link #preorderEnumeration}, con el nombre de siempre. */
    public Enumeration<TreeNode> depthFirstEnumeration() {
        return postorderEnumeration();
    }

    /**
     * Recorre el camino desde la raiz hasta este nodo.
     *
     * @throws IllegalArgumentException si el nodo dado no es antepasado de este.
     */
    public Enumeration<TreeNode> pathFromAncestorEnumeration(TreeNode ancestor) {
        return new CaminoEnum(this, ancestor);
    }

    public boolean isNodeChild(TreeNode aNode) {
        if (aNode == null) {
            return false;
        }
        if (getChildCount() == 0) {
            return false;
        }
        return (aNode.getParent() == this);
    }

    /**
     * El primer hijo.
     *
     * @throws NoSuchElementException si no tiene hijos.
     */
    public TreeNode getFirstChild() {
        if (getChildCount() == 0) {
            throw new NoSuchElementException("node has no children");
        }
        return getChildAt(0);
    }

    /**
     * El ultimo hijo.
     *
     * @throws NoSuchElementException si no tiene hijos.
     */
    public TreeNode getLastChild() {
        if (getChildCount() == 0) {
            throw new NoSuchElementException("node has no children");
        }
        return getChildAt(getChildCount() - 1);
    }

    /**
     * El hijo que sigue a ese.
     *
     * @throws IllegalArgumentException si no es hijo de este nodo.
     */
    public TreeNode getChildAfter(TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        int index = getIndex(aChild);
        if (index == -1) {
            throw new IllegalArgumentException("node is not a child");
        }
        if (index < getChildCount() - 1) {
            return getChildAt(index + 1);
        }
        return null;
    }

    /**
     * El hijo anterior a ese.
     *
     * @throws IllegalArgumentException si no es hijo de este nodo.
     */
    public TreeNode getChildBefore(TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        int index = getIndex(aChild);
        if (index == -1) {
            throw new IllegalArgumentException("argument is not a child");
        }
        if (index > 0) {
            return getChildAt(index - 1);
        }
        return null;
    }

    public boolean isNodeSibling(TreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }
        if (anotherNode == this) {
            return true;
        }
        TreeNode myParent = getParent();
        return (myParent != null && myParent == anotherNode.getParent());
    }

    public int getSiblingCount() {
        TreeNode myParent = getParent();
        if (myParent == null) {
            return 1;
        }
        return myParent.getChildCount();
    }

    public DefaultMutableTreeNode getNextSibling() {
        DefaultMutableTreeNode retval;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            retval = null;
        } else {
            retval = (DefaultMutableTreeNode) myParent.getChildAfter(this);
        }
        return retval;
    }

    public DefaultMutableTreeNode getPreviousSibling() {
        DefaultMutableTreeNode retval;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            retval = null;
        } else {
            retval = (DefaultMutableTreeNode) myParent.getChildBefore(this);
        }
        return retval;
    }

    /** Si no tiene hijos; distinto de no permitirlos. Ver la nota de la clase. */
    public boolean isLeaf() {
        return (getChildCount() == 0);
    }

    /** La primera hoja bajando siempre por el primer hijo. */
    public DefaultMutableTreeNode getFirstLeaf() {
        DefaultMutableTreeNode node = this;
        while (!node.isLeaf()) {
            node = (DefaultMutableTreeNode) node.getFirstChild();
        }
        return node;
    }

    public DefaultMutableTreeNode getLastLeaf() {
        DefaultMutableTreeNode node = this;
        while (!node.isLeaf()) {
            node = (DefaultMutableTreeNode) node.getLastChild();
        }
        return node;
    }

    /** La hoja que sigue en el arbol entero, no solo bajo este nodo. */
    public DefaultMutableTreeNode getNextLeaf() {
        DefaultMutableTreeNode nextSibling;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            return null;
        }
        nextSibling = getNextSibling();
        if (nextSibling != null) {
            return nextSibling.getFirstLeaf();
        }
        return myParent.getNextLeaf();
    }

    public DefaultMutableTreeNode getPreviousLeaf() {
        DefaultMutableTreeNode previousSibling;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            return null;
        }
        previousSibling = getPreviousSibling();
        if (previousSibling != null) {
            return previousSibling.getLastLeaf();
        }
        return myParent.getPreviousLeaf();
    }

    /** Cuantas hojas cuelgan de este nodo. */
    public int getLeafCount() {
        int count = 0;
        Enumeration<TreeNode> enum_ = breadthFirstEnumeration();
        while (enum_.hasMoreElements()) {
            TreeNode node = enum_.nextElement();
            if (node.isLeaf()) {
                count++;
            }
        }
        return count;
    }

    /** El {@code toString} del dato, no del nodo; ver la nota de la clase. */
    public String toString() {
        if (userObject == null) {
            return null;
        }
        return userObject.toString();
    }

    /**
     * Una copia del nodo, sin padre y sin hijos.
     *
     * <p>Copia superficial a proposito: copiar el subarbol seria caro y casi nunca es lo que se
     * quiere. Quien quiera el subarbol lo recorre.
     */
    public Object clone() {
        DefaultMutableTreeNode newNode;
        try {
            newNode = (DefaultMutableTreeNode) super.clone();
            newNode.children = null;
            newNode.parent = null;
        } catch (CloneNotSupportedException e) {
            throw new Error(e.toString());
        }
        return newNode;
    }

    /** El recorrido vacio; ver {@link #EMPTY_ENUMERATION}. */
    static final class VacioEnum implements Enumeration<TreeNode> {

        public boolean hasMoreElements() {
            return false;
        }

        public TreeNode nextElement() {
            throw new NoSuchElementException("No more elements");
        }
    }

    /** Los hijos de un nodo, en orden. */
    static final class HijosEnum implements Enumeration<TreeNode> {

        private final Vector<MutableTreeNode> hijos;
        private int i = 0;

        HijosEnum(Vector<MutableTreeNode> hijos) {
            this.hijos = hijos;
        }

        public boolean hasMoreElements() {
            return i < hijos.size();
        }

        public TreeNode nextElement() {
            if (!hasMoreElements()) {
                throw new NoSuchElementException("No more elements");
            }
            TreeNode n = hijos.elementAt(i);
            i++;
            return n;
        }
    }

    /**
     * Recorrido en preorden: el padre antes que sus hijos.
     *
     * <p>Se lleva una pila de recorridos pendientes en lugar de recursion, para que un arbol muy
     * hondo no desborde.
     */
    static final class PreordenEnum implements Enumeration<TreeNode> {

        private final Stack<Enumeration<TreeNode>> pila = new Stack<Enumeration<TreeNode>>();

        PreordenEnum(TreeNode raiz) {
            Vector<TreeNode> v = new Vector<TreeNode>(1);
            v.addElement(raiz);
            pila.push(v.elements());
        }

        public boolean hasMoreElements() {
            return (!pila.empty() && pila.peek().hasMoreElements());
        }

        public TreeNode nextElement() {
            Enumeration<TreeNode> arriba = pila.peek();
            TreeNode nodo = arriba.nextElement();
            if (!arriba.hasMoreElements()) {
                pila.pop();
            }
            Enumeration<? extends TreeNode> hijos = nodo.children();
            if (hijos.hasMoreElements()) {
                pila.push((Enumeration<TreeNode>) hijos);
            }
            return nodo;
        }
    }

    /** Recorrido en posorden: los hijos antes que el padre. */
    static final class PosordenEnum implements Enumeration<TreeNode> {

        private TreeNode raiz;
        private Enumeration<? extends TreeNode> hijos;
        private Enumeration<TreeNode> subarbol;

        PosordenEnum(TreeNode raiz) {
            this.raiz = raiz;
            hijos = raiz.children();
            subarbol = DefaultMutableTreeNode.EMPTY_ENUMERATION;
        }

        public boolean hasMoreElements() {
            return raiz != null;
        }

        public TreeNode nextElement() {
            TreeNode retval;
            if (subarbol.hasMoreElements()) {
                retval = subarbol.nextElement();
            } else if (hijos.hasMoreElements()) {
                subarbol = new PosordenEnum(hijos.nextElement());
                retval = subarbol.nextElement();
            } else {
                retval = raiz;
                raiz = null;
            }
            return retval;
        }
    }

    /** Recorrido por niveles: fila por fila. */
    static final class NivelesEnum implements Enumeration<TreeNode> {

        private final java.util.LinkedList<Enumeration<? extends TreeNode>> cola =
                new java.util.LinkedList<Enumeration<? extends TreeNode>>();

        NivelesEnum(TreeNode raiz) {
            Vector<TreeNode> v = new Vector<TreeNode>(1);
            v.addElement(raiz);
            cola.addLast(v.elements());
        }

        public boolean hasMoreElements() {
            return (!cola.isEmpty() && cola.getFirst().hasMoreElements());
        }

        public TreeNode nextElement() {
            Enumeration<? extends TreeNode> primera = cola.getFirst();
            TreeNode nodo = primera.nextElement();
            if (!primera.hasMoreElements()) {
                cola.removeFirst();
            }
            Enumeration<? extends TreeNode> hijos = nodo.children();
            if (hijos.hasMoreElements()) {
                cola.addLast(hijos);
            }
            return nodo;
        }
    }

    /** Recorre el camino desde un antepasado hasta un nodo. */
    static final class CaminoEnum implements Enumeration<TreeNode> {

        private final Stack<TreeNode> pila = new Stack<TreeNode>();

        CaminoEnum(TreeNode nodo, TreeNode antepasado) {
            if (nodo == null || antepasado == null) {
                throw new IllegalArgumentException("argument is null");
            }
            TreeNode n = nodo;
            while (n != null && n != antepasado) {
                pila.push(n);
                n = n.getParent();
            }
            if (n != antepasado) {
                throw new IllegalArgumentException("node is not an ancestor");
            }
            pila.push(antepasado);
        }

        public boolean hasMoreElements() {
            return !pila.isEmpty();
        }

        public TreeNode nextElement() {
            if (pila.isEmpty()) {
                throw new NoSuchElementException("No more elements");
            }
            return pila.pop();
        }
    }
}
