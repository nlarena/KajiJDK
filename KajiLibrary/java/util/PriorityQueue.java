package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

// A {@link Queue} whose head is always the *smallest* element rather than the oldest — a
// priority queue, implemented as a **binary min-heap** over a plain array.
//
// The heap is the reason this is worth having. The array is read as a complete binary tree:
// the children of index `i` live at `2i+1` and `2i+2`, its parent at `(i-1)/2`. No links, no
// nodes — the tree is implied by arithmetic, which is why a heap is the densest ordered
// structure there is. The single invariant is that **a parent never compares greater than its
// children**; note what it does *not* say: siblings are unordered, and the array is not
// sorted. That weaker promise is exactly what buys O(log n) insertion where a sorted array
// would cost O(n).
//
// Both mutations restore the invariant by moving one element along a single root-to-leaf path:
//
//   - `siftUp` (after appending at the end) walks the new element towards the root while it is
//     smaller than its parent.
//   - `siftDown` (after moving the last element into the vacated root) walks it towards the
//     leaves, always swapping with the *smaller* child — the smaller one, because that child
//     is the only candidate that can legally sit above the other.
//
// Each path is at most log2(n) long, so offer and poll are O(log n) and peek is O(1). The
// price is that iteration comes out in array order, not sorted order: to see the elements in
// priority order you must drain the queue, which is precisely heapsort.
//
// Ordering comes from the elements' own {@link Comparable}, or from a {@link Comparator} given
// to the constructor. Null elements are rejected: null is `poll`'s "empty" answer.
//
// Subset of the JDK's: no Spliterator, no bulk operations, no serialization.
public class PriorityQueue<E> extends AbstractQueue<E> {

    // The heap, laid out breadth-first: index 0 is the root, i.e. the minimum.
    private Object[] queue;
    private int size;
    // Null means "use the elements' natural ordering".
    private final Comparator<E> comparator;

    public PriorityQueue() {
        queue = new Object[11];
        comparator = null;
    }

    public PriorityQueue(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("initialCapacity < 1");
        }
        queue = new Object[initialCapacity];
        comparator = null;
    }

    public PriorityQueue(Comparator<? super E> comparator) {
        queue = new Object[11];
        this.comparator = (Comparator<E>) comparator;
    }

    public PriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("initialCapacity < 1");
        }
        queue = new Object[initialCapacity];
        this.comparator = (Comparator<E>) comparator;
    }

    // Building from a collection one offer at a time is O(n log n). The JDK does better with
    // Floyd's bottom-up heapify (O(n)) when it can; ours takes the simple route, since the
    // difference only shows up on a bulk load.
    public PriorityQueue(Collection<? extends E> c) {
        queue = new Object[11];
        comparator = null;
        Collection<E> src = (Collection<E>) c;
        Iterator<E> it = src.iterator();
        while (it.hasNext()) {
            offer(it.next());
        }
    }

    public Comparator<? super E> comparator() {
        return comparator;
    }

    // Compare two elements. Both are taken as `Object`: calling a method on a receiver whose
    // static type is a *type variable* is silently dropped by our javac (finding #111), so the
    // natural-ordering path binds the element to a `Comparable` local first.
    private int compare(Object a, Object b) {
        int c;
        if (comparator != null) {
            c = comparator.compare((E) a, (E) b);
        } else {
            Comparable<Object> ca = (Comparable<Object>) a;
            c = ca.compareTo(b);
        }
        return c;
    }

    private void grow() {
        if (size == queue.length) {
            Object[] bigger = new Object[queue.length * 2];
            for (int i = 0; i < size; i++) {
                bigger[i] = queue[i];
            }
            queue = bigger;
        }
    }

    // Walk `x` up from `k` until its parent no longer compares greater. The element is carried
    // in a local and written once at the end rather than swapped at every step — half the
    // array writes for the same result.
    private void siftUp(int k, Object x) {
        int i = k;
        boolean placed = false;
        while (i > 0 && !placed) {
            int parent = (i - 1) / 2;
            if (compare(x, queue[parent]) >= 0) {
                placed = true;
            } else {
                queue[i] = queue[parent];
                i = parent;
            }
        }
        queue[i] = x;
    }

    // Walk `x` down from `k`, always following the smaller child. Stopping at `size / 2` is
    // the leaf test: an index at or past it has no children.
    private void siftDown(int k, Object x) {
        int i = k;
        int half = size / 2;
        boolean placed = false;
        while (i < half && !placed) {
            int child = 2 * i + 1;
            int right = child + 1;
            if (right < size && compare(queue[right], queue[child]) < 0) {
                child = right;
            }
            if (compare(x, queue[child]) <= 0) {
                placed = true;
            } else {
                queue[i] = queue[child];
                i = child;
            }
        }
        queue[i] = x;
    }

    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        grow();
        // Append at the end, then let it rise: the array stays a complete tree at every step,
        // which is what keeps the shape (and therefore the depth) under control.
        int i = size;
        size = size + 1;
        siftUp(i, e);
        return true;
    }

    public E peek() {
        Object head = null;
        if (size > 0) {
            head = queue[0];
        }
        return (E) head;
    }

    public E poll() {
        Object head = null;
        if (size > 0) {
            head = queue[0];
            size = size - 1;
            Object last = queue[size];
            queue[size] = null;
            if (size > 0) {
                // The hole is at the root and the only element without a home is the former
                // last leaf — so drop it in and let it sink.
                siftDown(0, last);
            }
        }
        return (E) head;
    }

    public int size() {
        return size;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            queue[i] = null;
        }
        size = 0;
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

    private int indexOf(Object o) {
        int found = -1;
        for (int i = 0; i < size; i++) {
            if (found < 0 && eq(o, queue[i])) {
                found = i;
            }
        }
        return found;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    // Removing from the middle is the one O(n) operation: the heap gives no way to *find* an
    // arbitrary element, only to find the minimum, so this is a linear scan followed by a
    // local repair. That repair needs both directions — the element that fills the hole came
    // from the far end of the array, so it may belong either above or below its new position.
    public boolean remove(Object o) {
        int i = indexOf(o);
        boolean removed = i >= 0;
        if (removed) {
            size = size - 1;
            Object last = queue[size];
            queue[size] = null;
            if (i < size) {
                siftDown(i, last);
                // siftDown left it where it was, so the hole's subtree was already fine and
                // the element may instead be too small for its parent.
                if (queue[i] == last) {
                    siftUp(i, last);
                }
            }
        }
        return removed;
    }

    public Iterator<E> iterator() {
        return new PriorityQueueItr<E>(this);
    }

    // Package-private, for the iterator: the heap array in its stored order.
    Object at(int i) {
        return queue[i];
    }

    int count() {
        return size;
    }

    /**
     * A spliterator over these elements.
     *
     *  <p>Sin ORDERED, y esa ausencia es informacion: una cola de prioridad recorre en el orden del
     * monticulo, que no es el orden de prioridad. Solo `poll` respeta la prioridad.
     *
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL);
    }
}

// Walks the backing array, which is heap order and therefore *not* priority order — only the
// first element is guaranteed to be the smallest. The JDK's does the same, and documents it.
// Top-level package-private rather than nested, since a nested class inside a *generic* class
// is miscompiled (finding #13).
final class PriorityQueueItr<E> implements Iterator<E> {

    private final PriorityQueue<E> heap;
    private int cursor;

    PriorityQueueItr(PriorityQueue<E> heap) {
        this.heap = heap;
    }

    public boolean hasNext() {
        return cursor < heap.count();
    }

    public E next() {
        if (cursor >= heap.count()) {
            throw new NoSuchElementException();
        }
        E e = (E) heap.at(cursor);
        cursor = cursor + 1;
        return e;
    }

}
