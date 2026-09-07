package javax.swing.text;

import java.util.Stack;

/**
 * Recorre un arbol de elementos en profundidad, primero el padre y despues los hijos.
 *
 * <p>Guarda el camino desde la raiz en una pila, no punteros al arbol: por eso {@link #depth}
 * sale gratis y por eso el recorrido se puede clonar y seguir por dos lados.
 *
 * <p>No es a prueba de cambios: si el documento se edita mientras se recorre, lo que sigue no
 * esta definido. Quien recorre y edita a la vez tiene que rehacer el recorrido.
 */
public class ElementIterator implements Cloneable {

    private Element root;
    private Stack<StackItem> elementStack = null;

    /** Un elemento y por cual de sus hijos vamos. */
    private static class StackItem implements Cloneable {

        Element item;
        int childIndex;

        private StackItem(Element elem) {
            item = elem;
            childIndex = -1;
        }

        private void incrementIndex() {
            childIndex = childIndex + 1;
        }

        private Element getElement() {
            return item;
        }

        private int getIndex() {
            return childIndex;
        }

        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    /** Recorre el elemento raiz por omision de ese documento. */
    public ElementIterator(Document document) {
        root = document.getDefaultRootElement();
    }

    /** Recorre a partir de ese elemento. */
    public ElementIterator(Element root) {
        this.root = root;
    }

    /** Una copia que sigue desde el mismo lugar. */
    public synchronized Object clone() {
        try {
            ElementIterator it = new ElementIterator(root);
            if (elementStack != null) {
                it.elementStack = new Stack<StackItem>();
                for (int i = 0; i < elementStack.size(); i++) {
                    StackItem item = elementStack.elementAt(i);
                    StackItem clonee = (StackItem) item.clone();
                    it.elementStack.push(clonee);
                }
            }
            return it;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /** Vuelve al principio y devuelve la raiz. */
    public Element first() {
        if (root == null) {
            return null;
        }
        elementStack = new Stack<StackItem>();
        if (root.getElementCount() != 0) {
            elementStack.push(new StackItem(root));
        }
        return root;
    }

    /** Cuantos niveles hay debajo de la raiz hasta el elemento actual; cero si no arranco. */
    public int depth() {
        if (elementStack == null) {
            return 0;
        }
        return elementStack.size();
    }

    /** Donde esta parado, o {@code null} si no arranco o ya termino. */
    public Element current() {
        if (elementStack == null) {
            return first();
        }
        if (!elementStack.empty()) {
            StackItem item = elementStack.peek();
            Element elem = item.getElement();
            int index = item.getIndex();
            if (index == -1) {
                return elem;
            }
            return elem.getElement(index);
        }
        return null;
    }

    /**
     * El siguiente en profundidad.
     *
     * <p>Baja al primer hijo si lo hay; si no, avanza al hermano; si no queda hermano, sube hasta
     * encontrar uno. Es el orden en que se lee un documento.
     */
    public Element next() {
        if (elementStack == null) {
            return first();
        }
        if (elementStack.empty()) {
            return null;
        }

        StackItem item = elementStack.peek();
        Element elem = item.getElement();
        int index = item.getIndex();

        if (index + 1 < elem.getElementCount()) {
            Element child = elem.getElement(index + 1);
            if (child.isLeaf()) {
                item.incrementIndex();
                return child;
            }
            item.incrementIndex();
            elementStack.push(new StackItem(child));
            return child;
        }

        // Se acabaron los hijos: subir hasta encontrar a alguien con hermanos.
        elementStack.pop();
        while (!elementStack.empty()) {
            StackItem top = elementStack.peek();
            Element topElem = top.getElement();
            int topIndex = top.getIndex();
            if (topIndex + 1 < topElem.getElementCount()) {
                Element child = topElem.getElement(topIndex + 1);
                top.incrementIndex();
                if (!child.isLeaf()) {
                    elementStack.push(new StackItem(child));
                }
                return child;
            }
            elementStack.pop();
        }
        return null;
    }

    /**
     * El anterior en el recorrido, o {@code null} si estamos en el primero.
     *
     * <p>Anterior en el mismo orden que {@link #next}: el hermano de la izquierda, bajando hasta
     * su ultimo descendiente, o el padre si no hay hermano.
     */
    public Element previous() {
        int stackSize;
        if (elementStack == null || (stackSize = elementStack.size()) == 0) {
            return null;
        }

        StackItem item = elementStack.peek();
        Element elem = item.getElement();
        int index = item.getIndex();

        if (index > 0) {
            Element hermano = elem.getElement(index - 1);
            return ultimoDescendiente(hermano);
        }
        if (index == 0) {
            return elem;
        }
        if (stackSize > 1) {
            return elementStack.elementAt(stackSize - 2).getElement();
        }
        return null;
    }

    /** El ultimo elemento de ese subarbol, bajando siempre por el ultimo hijo. */
    private Element ultimoDescendiente(Element e) {
        Element actual = e;
        while (actual.getElementCount() > 0) {
            actual = actual.getElement(actual.getElementCount() - 1);
        }
        return actual;
    }
}
