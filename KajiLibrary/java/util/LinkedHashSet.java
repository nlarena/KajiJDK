package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

// A {@link Set} that iterates in **insertion order** — the set analogue of
// {@link LinkedHashMap}, and built on one, exactly as the JDK builds it. A set is a map whose
// values carry no information, so every element becomes a key mapped to one shared placeholder
// and all the work (hashing, chaining, the order list) belongs to the map.
//
// The reason to want it: {@link HashSet} iterates in whatever order the hash table happens to
// lay the elements out, which is stable for a given run but arbitrary and changes the moment
// the table resizes. That is fine for membership tests and a menace for anything whose output
// a human or a diff will read — deduplicating a list of names, collecting the distinct fields
// of a record, emitting a set into a file. This class gives you the deduplication *and* keeps
// the order you put things in, for two pointers per element.
//
// It costs nothing in lookup: `contains` is still one hash and one short chain walk. The only
// price over HashSet is memory, and only for a `TreeSet` would you also pay O(log n).
//
// `iterator()` walks the backing map's order list through the package-private
// `firstEntry`/`afterEntry` seam — our `Set` has no view machinery, so that seam is what makes
// the ordering observable at all, and `getFirst`/`getLast` expose the two ends directly.
//
// Subset of the JDK's: addFirst/addLast, reversed() and the spliterator/stream methods are not
// modelled. size/isEmpty/contains/add/remove/clear/iterator are declared here rather than
// inherited, because the JDK's LinkedHashSet gets them from HashSet and AbstractCollection,
// neither of which ours can extend without `super` calls (unsupported by our bytecode generator).
public class LinkedHashSet<E> extends AbstractSet<E> implements Set<E>, SequencedSet<E> {

    // The value every key maps to. Its identity is irrelevant — only "there is an entry here"
    // matters — so a single instance is shared by every element of every LinkedHashSet.
    private static final Object PRESENT = new Object();

    private final LinkedHashMap<E, Object> map;

    public LinkedHashSet() {
        map = new LinkedHashMap<E, Object>();
    }

    public LinkedHashSet(int initialCapacity) {
        map = new LinkedHashMap<E, Object>(initialCapacity);
    }

    public LinkedHashSet(int initialCapacity, float loadFactor) {
        map = new LinkedHashMap<E, Object>(initialCapacity, loadFactor);
    }

    // Copying a Collection preserves *its* iteration order, which is the point: feeding a
    // List through here gives you the list deduplicated, still in list order.
    public LinkedHashSet(Collection<? extends E> c) {
        map = new LinkedHashMap<E, Object>();
        // The wildcard is widened to E by a cast so the iterator's element type is a plain
        // type variable — a capture-converted `Iterator<capture-of ? extends E>` is more than
        // our javac's inference handles.
        Collection<E> src = (Collection<E>) c;
        Iterator<E> it = src.iterator();
        while (it.hasNext()) {
            map.put(it.next(), PRESENT);
        }
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

    // A set add reports whether the element was *new*, which is exactly whether put found no
    // previous entry. Re-adding an element already present therefore does **not** move it to
    // the end: its insertion position was fixed the first time.
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    public void clear() {
        map.clear();
    }

    /**
     * Agrega `e` al **principio** del orden, moviendolo si ya estaba.
     *
     * <p>Mover es la parte que sorprende y es lo que dice el contrato: a diferencia de `add`, que
     * sobre un elemento presente no hace nada, `addFirst` lo trae al frente.
     */
    public void addFirst(E e) {
        map.putFirst(e, PRESENT);
    }

    /** Idem, al final. */
    public void addLast(E e) {
        map.putLast(e, PRESENT);
    }

    /**
     * Una **vista** del conjunto al reves, no una copia: comparte el mapa de atras, asi que un
     * cambio de un lado se ve del otro.
     */
    public SequencedSet<E> reversed() {
        return new LhmKeySet<E, Object>(map, true);
    }

    /**
     * Un conjunto dimensionado para `numElements` elementos.
     *
     * @throws IllegalArgumentException si `numElements` es negativo
     */
    public static <T> LinkedHashSet<T> newLinkedHashSet(int numElements) {
        if (numElements < 0) {
            throw new IllegalArgumentException("Negative number of elements: " + numElements);
        }
        return new LinkedHashSet<T>(numElements * 2 + 1);
    }

    // --- the ends of the order ------------------------------------------------------

    public E getFirst() {
        LhmEntry<E, Object> e = map.primeraEntrada();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e.getKey();
    }

    public E getLast() {
        LhmEntry<E, Object> e = map.ultimaEntrada();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e.getKey();
    }

    public E removeFirst() {
        E first = getFirst();
        map.remove(first);
        return first;
    }

    public E removeLast() {
        E last = getLast();
        map.remove(last);
        return last;
    }

    public Iterator<E> iterator() {
        return new LinkedHashSetItr<E>(map);
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.DISTINCT | Spliterator.SIZED | Spliterator.ORDERED);
    }
}

// Walks the backing map's order list, one entry at a time — no snapshot, no extra storage, and
// no dependence on the hash table's layout, which is precisely why the order it yields is the
// insertion order rather than the bucket order. Top-level package-private rather than nested,
// since a nested class inside a *generic* class is miscompiled (finding #13).
final class LinkedHashSetItr<E> implements Iterator<E> {

    private final LinkedHashMap<E, Object> map;
    private LhmEntry<E, Object> next;

    LinkedHashSetItr(LinkedHashMap<E, Object> map) {
        this.map = map;
        this.next = map.primeraEntrada();
    }

    public boolean hasNext() {
        return next != null;
    }

    public E next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        E key = next.getKey();
        next = map.afterEntry(next);
        return key;
    }

}
