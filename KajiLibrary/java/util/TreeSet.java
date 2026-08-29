package java.util;

// A set that keeps its elements in sorted order, backed by a {@link TreeMap} — exactly how
// the JDK builds it. A set is a map whose values carry no information, so every element is
// stored as a key mapped to one shared placeholder object; all the work (ordering, balancing,
// O(log n) lookup) is the tree's, and this class is the thin projection that hides the values.
//
// That is the whole design: `add` is `put`, `contains` is `containsKey`, `remove` is `remove`,
// and iteration walks the tree in order.
//
// Subset of the JDK's: the NavigableSet/SortedSet navigation (subSet/headSet/tailSet/
// ceiling/floor/…) is not modelled, since our `Set` is a plain marker over `Collection`.
public class TreeSet<E> implements Set<E> {

    // The single value every key maps to. Its identity is irrelevant — only "there is an
    // entry here" matters — so one instance is shared by every element of every TreeSet.
    private static final Object PRESENT = new Object();

    private final TreeMap<E, Object> map;

    public TreeSet() {
        this.map = new TreeMap<E, Object>();
    }

    public TreeSet(Comparator<E> comparator) {
        this.map = new TreeMap<E, Object>(comparator);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    // A set add reports whether the element was *new*, which is exactly whether put found
    // no previous entry.
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    public void clear() {
        map.clear();
    }

    public Comparator<E> comparator() {
        return map.comparator();
    }

    // The smallest element, by the set's ordering.
    public E first() {
        return map.firstKey();
    }

    public E last() {
        return map.lastKey();
    }

    public Iterator<E> iterator() {
        return new TreeSetItr<E>(map, true);
    }

    public Iterator<E> descendingIterator() {
        return new TreeSetItr<E>(map, false);
    }

    /**
     * A spliterator over these elements, in sort order.
     *
     * @see SortedSet#spliterator()
     */
    public Spliterator<E> spliterator() {
        final Comparator<? super E> order = this.comparator();
        return new Spliterators.IteratorSpliterator<E>(this,
                Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED
                        | Spliterator.SIZED) {
            public Comparator<? super E> getComparator() {
                return order;
            }
        };
    }
}

// Walks the backing tree in order (or in reverse), one node at a time through the map's
// successor/predecessor links — no snapshot, no extra storage. Top-level package-private
// rather than nested, since a nested class inside a *generic* class is miscompiled (#13).
final class TreeSetItr<E> implements Iterator<E> {

    private final TreeMap<E, Object> map;
    private final boolean ascending;
    private TmNode<E, Object> next;

    TreeSetItr(TreeMap<E, Object> map, boolean ascending) {
        this.map = map;
        this.ascending = ascending;
        if (ascending) {
            this.next = map.firstNode();
        } else {
            this.next = map.lastNode();
        }
    }

    public boolean hasNext() {
        return next != null;
    }

    public E next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        E key = next.key;
        if (ascending) {
            next = map.successor(next);
        } else {
            next = map.predecessor(next);
        }
        return key;
    }

}
