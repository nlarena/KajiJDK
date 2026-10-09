package javax.swing.text;

import java.util.Stack;

/**
 * It walks an element tree depth first, the parent first and the children afterwards.
 *
 * <p>It keeps the path from the root on a stack, not pointers into the tree: that is why
 * {@link #depth} comes for free and why the walk can be cloned and continued along two paths.
 *
 * <p>It is not change-proof: if the document is edited while walking, what follows is undefined.
 * Whoever walks and edits at the same time has to redo the walk.
 */
public class ElementIterator implements Cloneable {

    private Element root;
    private Stack<StackItem> elementStack = null;

    /** An element and which of its children we are on. */
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

    /** It walks that document's default root element. */
    public ElementIterator(Document document) {
        root = document.getDefaultRootElement();
    }

    /** It walks starting from that element. */
    public ElementIterator(Element root) {
        this.root = root;
    }

    /** A copy that continues from the same place. */
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

    /** It goes back to the beginning and returns the root. */
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

    /**
     * How many levels there are below the root down to the current element; zero if it has not
     * started.
     */
    public int depth() {
        if (elementStack == null) {
            return 0;
        }
        return elementStack.size();
    }

    /** Where it is standing, or {@code null} if it has not started or has already finished. */
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
     * The next one, depth first.
     *
     * <p>It goes down to the first child if there is one; if not, it moves on to the sibling; if no
     * sibling is left, it goes up until it finds one. It is the order in which a document is read.
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

        // The children ran out: go up until somebody with siblings is found.
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
     * The previous one in the walk, or {@code null} if we are on the first.
     *
     * <p>Previous in the same order as {@link #next}: the sibling on the left, going down to its
     * last descendant, or the parent if there is no sibling.
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
            Element sibling = elem.getElement(index - 1);
            return lastDescendant(sibling);
        }
        if (index == 0) {
            return elem;
        }
        if (stackSize > 1) {
            return elementStack.elementAt(stackSize - 2).getElement();
        }
        return null;
    }

    /** The last element of that subtree, going always down the last child. */
    private Element lastDescendant(Element e) {
        Element current = e;
        while (current.getElementCount() > 0) {
            current = current.getElement(current.getElementCount() - 1);
        }
        return current;
    }
}
