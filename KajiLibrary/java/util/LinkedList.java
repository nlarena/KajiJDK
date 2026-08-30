package java.util;

// A doubly-linked list: every element is a node holding its neighbours, so inserting or
// removing at a known position is O(1) — no shifting, unlike {@link ArrayList}. The price is
// that reaching position *i* costs a walk, and every element pays for two extra references.
// That trade is the whole reason both exist: ArrayList for indexed access, LinkedList for
// churn at the ends.
//
// Being linked at both ends is also what lets it be a {@link Deque} for free, so it doubles
// as a queue and as a stack.
//
// `get(int)` walks from the *nearer* end, which halves the average walk — the same trick the
// JDK plays.
public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E> {

    private LinkedNode<E> first;
    private LinkedNode<E> last;
    private int size;

    public LinkedList() {
    }

    // Copia los elementos de otra coleccion, en el orden de su iterador.
    public LinkedList(Collection<? extends E> c) {
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            this.addLast(it.next());
        }
    }

    // --- linking primitives (everything else is written in terms of these three) ---

    private void linkFirst(E e) {
        LinkedNode<E> f = first;
        LinkedNode<E> node = new LinkedNode<E>(null, e, f);
        first = node;
        if (f == null) {
            last = node;
        } else {
            f.prev = node;
        }
        size++;
    }

    private void linkLast(E e) {
        LinkedNode<E> l = last;
        LinkedNode<E> node = new LinkedNode<E>(l, e, null);
        last = node;
        if (l == null) {
            first = node;
        } else {
            l.next = node;
        }
        size++;
    }

    // Insert `e` immediately before the (non-null) node `succ`.
    private void linkBefore(E e, LinkedNode<E> succ) {
        LinkedNode<E> pred = succ.prev;
        LinkedNode<E> node = new LinkedNode<E>(pred, e, succ);
        succ.prev = node;
        if (pred == null) {
            first = node;
        } else {
            pred.next = node;
        }
        size++;
    }

    // Unlink a node that is known to be in the list, returning its element.
    private E unlink(LinkedNode<E> node) {
        E element = node.item;
        LinkedNode<E> prev = node.prev;
        LinkedNode<E> next = node.next;
        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
            node.prev = null;
        }
        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
            node.next = null;
        }
        node.item = null;
        size--;
        return element;
    }

    // The node at `index`, walking from whichever end is closer.
    private LinkedNode<E> node(int index) {
        LinkedNode<E> n;
        if (index < size / 2) {
            n = first;
            for (int i = 0; i < index; i++) {
                n = n.next;
            }
        } else {
            n = last;
            for (int i = size - 1; i > index; i--) {
                n = n.prev;
            }
        }
        return n;
    }

    // Null-safe equality, as an if/else helper: a boolean-valued ternary is rejected by our
    // javac (finding #109).
    private static boolean eq(Object a, Object b) {
        boolean same;
        if (a == null) {
            same = b == null;
        } else {
            same = a.equals(b);
        }
        return same;
    }

    private void checkIndex(int index, int bound) {
        if (index < 0 || index > bound) {
            throw new IndexOutOfBoundsException();
        }
    }

    // --- Collection / List ---

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    public void add(int index, E element) {
        checkIndex(index, size);
        if (index == size) {
            linkLast(element);
        } else {
            linkBefore(element, node(index));
        }
    }

    public E get(int index) {
        checkIndex(index, size - 1);
        return node(index).item;
    }

    public E set(int index, E element) {
        checkIndex(index, size - 1);
        LinkedNode<E> n = node(index);
        E old = n.item;
        n.item = element;
        return old;
    }

    public E remove(int index) {
        checkIndex(index, size - 1);
        return unlink(node(index));
    }

    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    public int indexOf(Object o) {
        int found = -1;
        int i = 0;
        LinkedNode<E> n = first;
        while (n != null) {
            if (found < 0 && eq(o, n.item)) {
                found = i;
            }
            i++;
            n = n.next;
        }
        return found;
    }

    public int lastIndexOf(Object o) {
        int found = -1;
        int i = size - 1;
        LinkedNode<E> n = last;
        while (n != null) {
            if (found < 0 && eq(o, n.item)) {
                found = i;
            }
            i--;
            n = n.prev;
        }
        return found;
    }

    public void clear() {
        LinkedNode<E> n = first;
        while (n != null) {
            LinkedNode<E> next = n.next;
            n.item = null;
            n.prev = null;
            n.next = null;
            n = next;
        }
        first = null;
        last = null;
        size = 0;
    }

    public Iterator<E> iterator() {
        return new LinkedListItr<E>(first, true);
    }

    public Iterator<E> descendingIterator() {
        return new LinkedListItr<E>(last, false);
    }

    // --- Deque: the throwing forms ---

    public void addFirst(E e) {
        linkFirst(e);
    }

    public void addLast(E e) {
        linkLast(e);
    }

    public E removeFirst() {
        if (first == null) {
            throw new NoSuchElementException();
        }
        return unlink(first);
    }

    public E removeLast() {
        if (last == null) {
            throw new NoSuchElementException();
        }
        return unlink(last);
    }

    public E getFirst() {
        if (first == null) {
            throw new NoSuchElementException();
        }
        return first.item;
    }

    public E getLast() {
        if (last == null) {
            throw new NoSuchElementException();
        }
        return last.item;
    }

    // --- Deque: the sentinel forms ---

    public boolean offerFirst(E e) {
        linkFirst(e);
        return true;
    }

    public boolean offerLast(E e) {
        linkLast(e);
        return true;
    }

    public E pollFirst() {
        E e;
        if (first == null) {
            e = null;
        } else {
            e = unlink(first);
        }
        return e;
    }

    public E pollLast() {
        E e;
        if (last == null) {
            e = null;
        } else {
            e = unlink(last);
        }
        return e;
    }

    public E peekFirst() {
        E e;
        if (first == null) {
            e = null;
        } else {
            e = first.item;
        }
        return e;
    }

    public E peekLast() {
        E e;
        if (last == null) {
            e = null;
        } else {
            e = last.item;
        }
        return e;
    }

    public boolean removeFirstOccurrence(Object o) {
        boolean removed = false;
        LinkedNode<E> n = first;
        while (n != null && !removed) {
            LinkedNode<E> next = n.next;
            if (eq(o, n.item)) {
                unlink(n);
                removed = true;
            }
            n = next;
        }
        return removed;
    }

    public boolean removeLastOccurrence(Object o) {
        boolean removed = false;
        LinkedNode<E> n = last;
        while (n != null && !removed) {
            LinkedNode<E> prev = n.prev;
            if (eq(o, n.item)) {
                unlink(n);
                removed = true;
            }
            n = prev;
        }
        return removed;
    }

    // --- Queue view (head = first) ---

    public boolean offer(E e) {
        return offerLast(e);
    }

    public E poll() {
        return pollFirst();
    }

    public E peek() {
        return peekFirst();
    }

    public E remove() {
        return removeFirst();
    }

    public E element() {
        return getFirst();
    }

    // --- Stack view (top = first) ---

    public void push(E e) {
        addFirst(e);
    }

    public E pop() {
        return removeFirst();
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED);
    }
}

// One link: its element and both neighbours. Package-private and top-level rather than
// nested, since a nested class inside a *generic* class is miscompiled (finding #13).
final class LinkedNode<E> {

    E item;
    LinkedNode<E> prev;
    LinkedNode<E> next;

    LinkedNode(LinkedNode<E> prev, E item, LinkedNode<E> next) {
        this.prev = prev;
        this.item = item;
        this.next = next;
    }
}

// Walks the chain from a starting node, forwards or backwards — the same class serves
// `iterator()` and `descendingIterator()`.
final class LinkedListItr<E> implements Iterator<E> {

    private LinkedNode<E> cursor;
    private final boolean forward;

    LinkedListItr(LinkedNode<E> start, boolean forward) {
        this.cursor = start;
        this.forward = forward;
    }

    public boolean hasNext() {
        return cursor != null;
    }

    public E next() {
        if (cursor == null) {
            throw new NoSuchElementException();
        }
        E e = cursor.item;
        if (forward) {
            cursor = cursor.next;
        } else {
            cursor = cursor.prev;
        }
        return e;
    }

}
