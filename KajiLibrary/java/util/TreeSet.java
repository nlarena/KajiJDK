package java.util;

import java.lang.Cloneable;
import java.io.Serializable;

// A sorted set, resting on a {@link TreeMap} — exactly as the JDK builds it. A set is a map whose
// values say nothing, so each element is stored as a key pointing at a single shared sentinel object;
// all the work (ordering, balancing, O(log n) search) is the tree's, and this class is the thin
// projection that hides the values.
//
// That is the whole design: `add` is `put`, `contains` is `containsKey`, `remove` is `remove`, and
// walking is walking the tree in order.
//
// What it rests on is not a `TreeMap` but **any `NavigableMap`**, and that is what makes the views
// come free: `headSet(x)` is this same set over `map.headMap(x)`, and `descendingSet()` over
// `map.descendingMap()`. The fifteen navigation methods are written once and work the same over the
// whole set or over a reversed slice of a slice.
//
// The same mechanism gives a map's key views: `TreeMap.navigableKeySet()` returns a TreeSet over the
// map, with `noAdd` set. That is the only difference between a set and a map's key view: the view
// **cannot** add, because it would not know what value to put.
public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Serializable, Cloneable {

    // The single value every key points at. Its identity does not matter — all that counts is that
    // "there is an entry here" — so one instance is enough for every element of every TreeSet.
    private static final Object PRESENT = new Object();

    private final NavigableMap<E, Object> map;

    // The same map seen as walkable. It is kept separately because `NavigableMap` does not promise to
    // know how to be walked node by node; the two that arrive here — TreeMap and TmView — do.
    private final TmWalk<E, Object> walk;

    // Set when this set is a map's key view: then `add` refuses.
    private final boolean noAdd;

    public TreeSet() {
        this(new TreeMap<E, Object>(), false);
    }

    /**
     * With a comparator of its own.
     *
     * <p>The parameter is `Comparator<? super E>` and not `Comparator<E>`, which is what the JDK
     * declares: a comparator of `Object` knows how to compare `Integer`, so it has to be allowed in.
     * With the narrow form, `new TreeSet<Integer>(Collections.reverseOrder())` did not compile.
     */
    public TreeSet(Comparator<? super E> comparator) {
        this(new TreeMap<E, Object>(comparator), false);
    }

    // It copies another collection's elements, sorting them by their natural order.
    public TreeSet(Collection<? extends E> c) {
        this(new TreeMap<E, Object>(), false);
        this.addAll(c);
    }

    // It copies a set that **already comes sorted**, and keeps its comparator — like
    // `TreeMap(SortedMap)`, and for the same reason: without the comparator the copy would be
    // reordered.
    public TreeSet(SortedSet<E> s) {
        this(new TreeMap<E, Object>((Comparator<E>) s.comparator()), false);
        this.addAll(s);
    }

    TreeSet(NavigableMap<E, Object> map, boolean noAdd) {
        this.map = map;
        this.walk = (TmWalk<E, Object>) map;
        this.noAdd = noAdd;
    }

    // It wraps a view of the map behind keeping the add restriction: a slice of a key view still
    // cannot add.
    private TreeSet<E> over(NavigableMap<E, Object> view) {
        return new TreeSet<E>(view, this.noAdd);
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public boolean contains(Object o) {
        return this.map.containsKey(o);
    }

    // A set's `add` reports whether the element was **new**, which is exactly whether the `put` found
    // nothing before.
    public boolean add(E e) {
        if (this.noAdd) {
            throw new UnsupportedOperationException();
        }
        return this.map.put(e, PRESENT) == null;
    }

    public boolean remove(Object o) {
        return this.map.remove(o) != null;
    }

    public void clear() {
        this.map.clear();
    }

    public Comparator<? super E> comparator() {
        return this.map.comparator();
    }

    public Iterator<E> iterator() {
        return new TmKeyItr<E, Object>(this.walk, this.map);
    }

    public Iterator<E> descendingIterator() {
        return this.descendingSet().iterator();
    }

    // --- SortedSet ---

    public E first() {
        return this.map.firstKey();
    }

    public E last() {
        return this.map.lastKey();
    }

    // --- SequencedCollection ---
    //
    // The ends are read and taken out, but **not put**: in a sorted set the position is decided by the
    // order, not by whoever inserts. It is the same refusal as `TreeMap.putFirst`.

    public E getFirst() {
        return this.first();
    }

    public E getLast() {
        return this.last();
    }

    public E removeFirst() {
        E e = this.first();
        this.remove(e);
        return e;
    }

    public E removeLast() {
        E e = this.last();
        this.remove(e);
        return e;
    }

    public void addFirst(E e) {
        throw new UnsupportedOperationException();
    }

    public void addLast(E e) {
        throw new UnsupportedOperationException();
    }

    // It is narrowed to NavigableSet, which is what `NavigableSet.reversed()` promises now that it
    // carries a default of its own.
    public NavigableSet<E> reversed() {
        return this.descendingSet();
    }

    // --- NavigableSet: the neighbours ---
    //
    // The four are the map's navigation looking only at the keys. `lower` is the largest element
    // strictly smaller; `floor`, the largest <=; and `ceiling`/`higher` the symmetric ones upwards.
    // They return null when there is none, which is the only reasonable answer — unlike `first`/
    // `last`, which promise an element and therefore throw.

    public E lower(E e) {
        return this.map.lowerKey(e);
    }

    public E floor(E e) {
        return this.map.floorKey(e);
    }

    public E ceiling(E e) {
        return this.map.ceilingKey(e);
    }

    public E higher(E e) {
        return this.map.higherKey(e);
    }

    public E pollFirst() {
        Map.Entry<E, Object> e = this.map.pollFirstEntry();
        if (e == null) {
            return null;
        }
        return e.getKey();
    }

    public E pollLast() {
        Map.Entry<E, Object> e = this.map.pollLastEntry();
        if (e == null) {
            return null;
        }
        return e.getKey();
    }

    // --- NavigableSet: the views ---

    public NavigableSet<E> descendingSet() {
        return this.over(this.map.descendingMap());
    }

    public NavigableSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive) {
        return this.over(this.map.subMap(from, fromInclusive, to, toInclusive));
    }

    public NavigableSet<E> headSet(E to, boolean inclusive) {
        return this.over(this.map.headMap(to, inclusive));
    }

    public NavigableSet<E> tailSet(E from, boolean inclusive) {
        return this.over(this.map.tailMap(from, inclusive));
    }

    // SortedSet's three forms: the floor is in, the ceiling is not.
    public SortedSet<E> subSet(E from, E to) {
        return this.subSet(from, true, to, false);
    }

    public SortedSet<E> headSet(E to) {
        return this.headSet(to, false);
    }

    public SortedSet<E> tailSet(E from) {
        return this.tailSet(from, true);
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
