package java.util;

// A LIFO stack, as Java 1.0 shipped it: a {@link Vector} with the top pinned to the *end* of
// the array, so push and pop are the vector's O(1) append and remove-last.
//
// It is also the textbook example of inheritance used where composition was meant. Stack
// *extends* Vector, so it inherits the entire List API — which means `s.add(0, x)` and
// `s.remove(3)` are legal on a stack and quietly break the discipline the type was supposed
// to enforce. A stack is not a kind of vector; it *has* one. The Java 6 replacement,
// {@link ArrayDeque}, gets this right: push/pop/peek and nothing that reaches into the middle.
//
// Kept because `Stack` is the name every introduction reaches for, and because the mistake is
// worth being able to point at.
public class Stack<E> extends Vector<E> {

    public Stack() {
    }

    // Push is append. The return of the pushed item is a 1.0 convenience, not something the
    // Deque API bothered to keep.
    public E push(E item) {
        addElement(item);
        return item;
    }

    // Pop and peek take the vector's own monitor, so a push racing a pop cannot observe the
    // stack half-updated. Single-exit, since a `return` from inside a synchronized block
    // leaks the monitor with our javac (finding #105).
    public synchronized E pop() {
        Object top;
        synchronized (this) {
            int n = elementCount;
            if (n == 0) {
                throw new EmptyStackException();
            }
            top = elementData[n - 1];
            elementData[n - 1] = null;
            elementCount = n - 1;
        }
        return (E) top;
    }

    public synchronized E peek() {
        Object top;
        synchronized (this) {
            int n = elementCount;
            if (n == 0) {
                throw new EmptyStackException();
            }
            top = elementData[n - 1];
        }
        return (E) top;
    }

    public boolean empty() {
        boolean none;
        synchronized (this) {
            none = elementCount == 0;
        }
        return none;
    }

    // The 1-based distance from the top, or -1. One-based because 1 means "the top", which
    // reads better than 0 for a stack — and is a trap for anyone who assumes it is an index.
    public synchronized int search(Object o) {
        int distance;
        synchronized (this) {
            distance = -1;
            for (int i = elementCount - 1; i >= 0; i--) {
                if (distance < 0) {
                    Object e = elementData[i];
                    boolean hit;
                    if (o == null) {
                        hit = e == null;
                    } else {
                        hit = o.equals(e);
                    }
                    if (hit) {
                        distance = elementCount - i;
                    }
                }
            }
        }
        return distance;
    }
}
