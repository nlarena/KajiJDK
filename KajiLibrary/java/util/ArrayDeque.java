package java.util;

// A {@link Deque} over a **circular array**: the elements occupy a contiguous run that wraps
// around the end of the array, tracked by the index of the head. That is what makes both ends
// O(1) — adding at the front just moves the head index backwards instead of shifting anything,
// which is precisely what an ArrayList cannot do.
//
// Against {@link LinkedList} (the other Deque here) the trade is the usual one: this stores
// only the elements, with no per-element node objects and no pointer chasing, but it has to
// grow by copying when it fills. As a stack or a queue it is the one to reach for.
//
// Null elements are rejected, as in the JDK: null is the "empty" answer of peek/poll, so
// storing one would make an absent element indistinguishable from a present null.
public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E> {

    // The backing array; the live elements are the `size` slots starting at `head`, wrapping.
    private Object[] elements;
    private int head;
    private int size;

    public ArrayDeque() {
        elements = new Object[16];
    }

    // Copia los elementos de otra coleccion, en el orden de su iterador: el primero queda al
    // frente.
    public ArrayDeque(Collection<? extends E> c) {
        this(c.size() < 1 ? 1 : c.size() + 1);
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            this.addLast(it.next());
        }
    }

    // Una copia superficial: mismo contenido, arreglo propio.
    public ArrayDeque<E> clone() {
        return new ArrayDeque<E>(this);
    }

    public ArrayDeque(int numElements) {
        int cap = numElements;
        if (cap < 1) {
            cap = 1;
        }
        elements = new Object[cap];
    }

    // Logical index i (0 = head) to physical slot.
    private int slot(int i) {
        int s = head + i;
        if (s >= elements.length) {
            s = s - elements.length;
        }
        return s;
    }

    // The element at logical index `i`. Package-private: the iterator reads through it, so it
    // can walk the deque live instead of copying it.
    Object at(int i) {
        return elements[slot(i)];
    }

    int count() {
        return size;
    }

    // Double the array when it fills, re-laying the elements from index 0 so the run is
    // contiguous again.
    private void grow() {
        if (size == elements.length) {
            Object[] bigger = new Object[elements.length * 2];
            for (int i = 0; i < size; i++) {
                bigger[i] = elements[slot(i)];
            }
            elements = bigger;
            head = 0;
        }
    }

    private static boolean eq(Object a, Object b) {
        boolean same;
        if (a == null) {
            same = b == null;
        } else {
            same = a.equals(b);
        }
        return same;
    }

    // Remove the element at logical index `i` by closing the gap from whichever side is
    // shorter — the array equivalent of unlinking a node.
    private void removeAt(int i) {
        if (i < size / 2) {
            for (int j = i; j > 0; j--) {
                elements[slot(j)] = elements[slot(j - 1)];
            }
            elements[head] = null;
            head = slot(1);
        } else {
            for (int j = i; j < size - 1; j++) {
                elements[slot(j)] = elements[slot(j + 1)];
            }
            elements[slot(size - 1)] = null;
        }
        size--;
    }

    // --- insertion ---

    public void addFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        grow();
        head = head - 1;
        if (head < 0) {
            head = elements.length - 1;
        }
        elements[head] = e;
        size++;
    }

    public void addLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        grow();
        elements[slot(size)] = e;
        size++;
    }

    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    public boolean add(E e) {
        addLast(e);
        return true;
    }

    public boolean offer(E e) {
        addLast(e);
        return true;
    }

    public void push(E e) {
        addFirst(e);
    }

    // --- removal ---

    public E pollFirst() {
        E e;
        if (size == 0) {
            e = null;
        } else {
            e = (E) elements[head];
            elements[head] = null;
            head = slot(1);
            size--;
        }
        return e;
    }

    public E pollLast() {
        E e;
        if (size == 0) {
            e = null;
        } else {
            int last = slot(size - 1);
            e = (E) elements[last];
            elements[last] = null;
            size--;
        }
        return e;
    }

    public E removeFirst() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return pollFirst();
    }

    public E removeLast() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return pollLast();
    }

    public E poll() {
        return pollFirst();
    }

    public E remove() {
        return removeFirst();
    }

    public E pop() {
        return removeFirst();
    }

    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    public boolean removeFirstOccurrence(Object o) {
        boolean removed = false;
        for (int i = 0; i < size; i++) {
            if (!removed && eq(o, at(i))) {
                removeAt(i);
                removed = true;
            }
        }
        return removed;
    }

    public boolean removeLastOccurrence(Object o) {
        boolean removed = false;
        for (int i = size - 1; i >= 0; i--) {
            if (!removed && eq(o, at(i))) {
                removeAt(i);
                removed = true;
            }
        }
        return removed;
    }

    // --- inspection ---

    public E peekFirst() {
        E e;
        if (size == 0) {
            e = null;
        } else {
            e = (E) elements[head];
        }
        return e;
    }

    public E peekLast() {
        E e;
        if (size == 0) {
            e = null;
        } else {
            e = (E) elements[slot(size - 1)];
        }
        return e;
    }

    public E getFirst() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return peekFirst();
    }

    public E getLast() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return peekLast();
    }

    public E peek() {
        return peekFirst();
    }

    public E element() {
        return getFirst();
    }

    public boolean contains(Object o) {
        boolean found = false;
        for (int i = 0; i < size; i++) {
            if (eq(o, at(i))) {
                found = true;
            }
        }
        return found;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            elements[slot(i)] = null;
        }
        head = 0;
        size = 0;
    }

    public Iterator<E> iterator() {
        return new ArrayDequeItr<E>(this, true);
    }

    public Iterator<E> descendingIterator() {
        return new ArrayDequeItr<E>(this, false);
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
                        Spliterator.NONNULL);
    }
}

// Walks the deque by logical index, head-to-tail or tail-to-head. Top-level package-private
// rather than nested, since a nested class inside a *generic* class is miscompiled (#13).
final class ArrayDequeItr<E> implements Iterator<E> {

    private final ArrayDeque<E> deque;
    private final boolean forward;
    private int cursor;

    ArrayDequeItr(ArrayDeque<E> deque, boolean forward) {
        this.deque = deque;
        this.forward = forward;
        if (forward) {
            this.cursor = 0;
        } else {
            this.cursor = deque.count() - 1;
        }
    }

    public boolean hasNext() {
        boolean more;
        if (forward) {
            more = cursor < deque.count();
        } else {
            more = cursor >= 0;
        }
        return more;
    }

    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        E e = (E) deque.at(cursor);
        if (forward) {
            cursor++;
        } else {
            cursor--;
        }
        return e;
    }

}
