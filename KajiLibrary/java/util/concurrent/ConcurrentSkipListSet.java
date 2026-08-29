package java.util.concurrent;

import java.util.Spliterator;
import java.util.Spliterators;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SequencedSet;
import java.util.SortedSet;


/**
 * A sorted set, safe for concurrent use, backed by a {@link ConcurrentSkipListMap}.
 *
 * <p>There is no skip list in this file. A set is a map whose values carry no information, so every
 * element is stored as a KEY mapped to one shared placeholder, and all the work — the ordering, the
 * express lanes, the O(log n) search, the atomicity — belongs to the map. What is left here is the
 * projection that hides the values, which is also how the JDK builds it and how {@link
 * java.util.TreeSet} is built over {@code TreeMap}.
 *
 * <p>That the range views come out right is a consequence of the same decision: {@code subSet} is a
 * set over {@code subMap}, so the bounds are enforced in exactly one place instead of being
 * re-derived here. The map's own key-set views had that bug once — they wrapped the base map rather
 * than the bounded view, and reported every key in the map — which is the argument for not writing
 * a second implementation of the same reasoning.
 *
 * @implNote A KajiLibrary subset. The JDK also exposes {@code clone}, the spliterator, and a bulk
 *           {@code removeAll}; those are omitted rather than declared and left throwing, so a caller
 *           who needs them gets a compile error instead of a surprise. {@code equals} and {@code
 *           hashCode} come from {@link AbstractSet} — the JDK overrides them for speed, not for a
 *           different answer.
 */
public class ConcurrentSkipListSet<E> extends AbstractSet<E> implements NavigableSet<E> {

    // The value every element maps to. Its identity is irrelevant -- only "there is an entry here"
    // matters -- so one instance is shared by every element of every set.
    private static final Object PRESENT = new Object();

    // Declared as the interface and not as ConcurrentSkipListMap, because a range view of the map
    // is a SkipSubMap and not a ConcurrentSkipListMap: the views this class hands back have to be
    // able to hold either.
    private final ConcurrentNavigableMap<E, Object> m;

    /** Creates an empty set ordered by the natural ordering of its elements. */
    public ConcurrentSkipListSet() {
        this.m = new ConcurrentSkipListMap<E, Object>();
    }

    /**
     * Creates an empty set ordered by the given comparator.
     *
     * @param comparator the ordering, or {@code null} for the natural ordering of the elements
     */
    public ConcurrentSkipListSet(Comparator<? super E> comparator) {
        this.m = new ConcurrentSkipListMap<E, Object>(comparator);
    }

    /**
     * Creates a set holding the elements of {@code c}, in their natural ordering.
     *
     * <p>Duplicates in {@code c} collapse, as they do in any set.
     */
    public ConcurrentSkipListSet(Collection<? extends E> c) {
        ConcurrentSkipListMap<E, Object> map = new ConcurrentSkipListMap<E, Object>();
        this.m = map;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            E element = it.next();
            map.put(element, ConcurrentSkipListSet.PRESENT);
        }
    }

    /**
     * Creates a set holding the elements of {@code s}, keeping the ORDERING of {@code s} and not
     * the natural one — a sorted set arrives with a comparator, and dropping it would silently
     * re-sort the elements.
     */
    public ConcurrentSkipListSet(SortedSet<E> s) {
        Comparator<? super E> ordering = s.comparator();
        ConcurrentSkipListMap<E, Object> map = new ConcurrentSkipListMap<E, Object>(ordering);
        this.m = map;
        Iterator<E> it = s.iterator();
        while (it.hasNext()) {
            E element = it.next();
            map.put(element, ConcurrentSkipListSet.PRESENT);
        }
    }

    // Wraps an existing map without copying it, which is what makes the range views LIVE: the set
    // returned by subSet and the set it came from are two projections of one map.
    ConcurrentSkipListSet(ConcurrentNavigableMap<E, Object> map) {
        this.m = map;
    }

    // ---- the set operations, each one a map operation with the value thrown away ----

    @Override
    public int size() {
        return this.m.size();
    }

    @Override
    public boolean isEmpty() {
        return this.m.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.m.containsKey(o);
    }

    /** Adds {@code e}, reporting whether it was NEW — which is whether the map had no entry yet. */
    @Override
    public boolean add(E e) {
        return this.m.putIfAbsent(e, ConcurrentSkipListSet.PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return this.m.remove(o, ConcurrentSkipListSet.PRESENT);
    }

    @Override
    public void clear() {
        this.m.clear();
    }

    @Override
    public Iterator<E> iterator() {
        NavigableSet<E> keys = this.m.navigableKeySet();
        return keys.iterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        NavigableSet<E> keys = this.m.descendingKeySet();
        return keys.iterator();
    }

    // ---- ordering and the ends ----

    @Override
    public Comparator<? super E> comparator() {
        return this.m.comparator();
    }

    @Override
    public E first() {
        return this.m.firstKey();
    }

    @Override
    public E last() {
        return this.m.lastKey();
    }

    /**
     * Refused: where an element sits in a sorted set is decided by the ordering, so there is no
     * such thing as putting one at the front.
     *
     * <p>Spelled out rather than inherited from {@link java.util.SequencedCollection} — the default
     * refuses too, but a set that reads as "supports the sequenced operations" and then throws is
     * worth saying out loud.
     */
    @Override
    public void addFirst(E e) {
        throw new UnsupportedOperationException();
    }

    /** Refused, for the reason {@link #addFirst} gives. */
    @Override
    public void addLast(E e) {
        throw new UnsupportedOperationException();
    }

    // ---- the navigation, which is the whole point of a sorted set ----

    @Override
    public E lower(E e) {
        return this.m.lowerKey(e);
    }

    @Override
    public E floor(E e) {
        return this.m.floorKey(e);
    }

    @Override
    public E ceiling(E e) {
        return this.m.ceilingKey(e);
    }

    @Override
    public E higher(E e) {
        return this.m.higherKey(e);
    }

    @Override
    public E pollFirst() {
        Map.Entry<E, Object> gone = this.m.pollFirstEntry();
        if (gone == null) {
            return null;
        }
        return gone.getKey();
    }

    @Override
    public E pollLast() {
        Map.Entry<E, Object> gone = this.m.pollLastEntry();
        if (gone == null) {
            return null;
        }
        return gone.getKey();
    }

    // ---- views, each one a set over the matching view of the map ----

    // Every view method below goes through this one, because every one of them trips finding #123:
    // ConcurrentNavigableMap narrows subMap/headMap/tailMap/descendingMap to return itself, but the
    // call resolves to NavigableMap's declaration instead of the override, so what arrives is typed
    // NavigableMap. The cast puts back the type the interface already guarantees -- our own map is
    // the only implementation, and every view it hands out is a ConcurrentNavigableMap.
    @SuppressWarnings("unchecked")
    private ConcurrentSkipListSet<E> setOver(NavigableMap<E, Object> view) {
        return new ConcurrentSkipListSet<E>((ConcurrentNavigableMap<E, Object>) view);
    }

    @Override
    public NavigableSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive) {
        NavigableMap<E, Object> view = this.m.subMap(from, fromInclusive, to, toInclusive);
        return this.setOver(view);
    }

    @Override
    public NavigableSet<E> headSet(E to, boolean inclusive) {
        NavigableMap<E, Object> view = this.m.headMap(to, inclusive);
        return this.setOver(view);
    }

    @Override
    public NavigableSet<E> tailSet(E from, boolean inclusive) {
        NavigableMap<E, Object> view = this.m.tailMap(from, inclusive);
        return this.setOver(view);
    }

    /** The SortedSet-shaped forms: {@code from} inclusive, {@code to} exclusive. */
    @Override
    public SortedSet<E> subSet(E from, E to) {
        NavigableMap<E, Object> view = this.m.subMap(from, true, to, false);
        return this.setOver(view);
    }

    @Override
    public SortedSet<E> headSet(E to) {
        NavigableMap<E, Object> view = this.m.headMap(to, false);
        return this.setOver(view);
    }

    @Override
    public SortedSet<E> tailSet(E from) {
        NavigableMap<E, Object> view = this.m.tailMap(from, true);
        return this.setOver(view);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        NavigableMap<E, Object> view = this.m.descendingMap();
        return this.setOver(view);
    }

    @Override
    public SequencedSet<E> reversed() {
        NavigableMap<E, Object> view = this.m.descendingMap();
        return this.setOver(view);
    }


    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED |
                        Spliterator.CONCURRENT | Spliterator.NONNULL);
    }
}
