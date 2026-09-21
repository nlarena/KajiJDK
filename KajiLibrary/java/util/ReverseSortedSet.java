package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Map;
import java.util.Set;

// The generic reversed views: a `SortedSet`'s, a `SortedMap`'s and a `Deque`'s. They are what backs
// the `reversed()` those three interfaces declare as a `default`, for any implementation -- one
// written by somebody outside included.
//
// **The problem, and why copying does not settle it.** `reversed()` promises a *view*: what is added
// on one side has to be seen from the other. Copying the elements into an array and reversing it is
// three lines and produces a snapshot; the first modification leaves it stale without warning. So
// these classes keep no elements: they keep the set behind and a way of walking it backwards.
//
// **How any SortedSet is walked backwards.** There is no backward iterator in the interface, but
// there are `last()` and `headSet(e)`: the one before `e` is `headSet(e).last()`. Chaining that walks
// the whole thing, at O(log n) per step over a tree. It is the same thing the JDK does, and it is the
// reason these views are useful and not an ornament: they materialise nothing.
//
// The comparison is reversed too, with `Collections.reverseOrder`, so that `first`/`last`,
// `headSet`/`tailSet` and the iteration order are all consistent with each other.

/** A `SortedSet` seen backwards. */
final class ReverseSortedSet<E> extends AbstractSet<E> implements SortedSet<E> {

    private final SortedSet<E> base;

    ReverseSortedSet(SortedSet<E> base) {
        this.base = base;
    }

    public Comparator<? super E> comparator() {
        Comparator<? super E> c = this.base.comparator();
        if (c == null) {
            // The one behind's natural order: reversed, that is `reverseOrder` with no
            // comparator.
            return (Comparator<? super E>) Collections.reverseOrder();
        }
        return Collections.reverseOrder(c);
    }

    public int size() {
        return this.base.size();
    }

    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    public boolean contains(Object o) {
        return this.base.contains(o);
    }

    public boolean add(E e) {
        return this.base.add(e);
    }

    public boolean remove(Object o) {
        return this.base.remove(o);
    }

    public void clear() {
        this.base.clear();
    }

    public Iterator<E> iterator() {
        return new ReverseSortedItr<E>(this.base);
    }

    /** Reversing the reversed is the one behind, not a third wrapper. */
    public SortedSet<E> reversed() {
        return this.base;
    }

    public E first() {
        return this.base.last();
    }

    public E last() {
        return this.base.first();
    }

    // The three slices turn round along with the order: what in the view is "up to `to`" is in the
    // one behind "from `to`", and **exclusive/inclusive swap** -- which is why the view's `headSet`
    // uses the one behind's `tailSet` and then takes `to` itself out.
    public SortedSet<E> headSet(E to) {
        SortedSet<E> queue = this.base.tailSet(to);
        return new ReverseSortedSet<E>(new TailWithoutFirst<E>(queue, to));
    }

    public SortedSet<E> tailSet(E from) {
        return new ReverseSortedSet<E>(this.base.headSet(from));
    }

    public SortedSet<E> subSet(E from, E to) {
        SortedSet<E> stretch = this.base.subSet(to, from);
        return new ReverseSortedSet<E>(new TailWithoutFirst<E>(stretch, to));
    }
}

// The backward traversal: it starts at `last()` and goes on taking `headSet(current).last()`.
final class ReverseSortedItr<E> implements Iterator<E> {

    private final SortedSet<E> base;
    private E upcoming;
    private boolean hasAny;
    private boolean started = false;

    ReverseSortedItr(SortedSet<E> base) {
        this.base = base;
    }

    private void startThread() {
        if (!this.started) {
            this.started = true;
            this.hasAny = !this.base.isEmpty();
            if (this.hasAny) {
                this.upcoming = this.base.last();
            }
        }
    }

    public boolean hasNext() {
        this.startThread();
        return this.hasAny;
    }

    public E next() {
        this.startThread();
        if (!this.hasAny) {
            throw new NoSuchElementException();
        }
        E current = this.upcoming;
        SortedSet<E> before = this.base.headSet(current);
        if (before.isEmpty()) {
            this.hasAny = false;
        } else {
            this.upcoming = before.last();
        }
        return current;
    }
}

// A `SortedSet` without its first element. It exists for a single detail of the slices above: on
// turning the order round, a bound that was exclusive becomes inclusive, and this class takes out
// exactly the one left over. Without it, `reversed().headSet(x)` would include `x`, which is the
// classic error of reversing ranges and the hardest to see in a test that only looks at sizes.
final class TailWithoutFirst<E> extends AbstractSet<E> implements SortedSet<E> {

    private final SortedSet<E> base;
    private final E excluido;

    TailWithoutFirst(SortedSet<E> base, E excluido) {
        this.base = base;
        this.excluido = excluido;
    }

    public Comparator<? super E> comparator() {
        return this.base.comparator();
    }

    public int size() {
        int n = this.base.size();
        return this.base.contains(this.excluido) ? n - 1 : n;
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public boolean contains(Object o) {
        if (o != null && o.equals(this.excluido)) {
            return false;
        }
        return this.base.contains(o);
    }

    public Iterator<E> iterator() {
        Iterator<E> it = this.base.iterator();
        // The excluded one, if it is there, is the first: `base` is the tail from it.
        if (this.base.contains(this.excluido) && it.hasNext()) {
            it.next();
        }
        return it;
    }

    public E first() {
        Iterator<E> it = this.iterator();
        if (!it.hasNext()) {
            throw new NoSuchElementException();
        }
        return it.next();
    }

    public E last() {
        E u = this.base.last();
        if (u != null && u.equals(this.excluido)) {
            throw new NoSuchElementException();
        }
        return u;
    }

    public SortedSet<E> headSet(E to) {
        return new TailWithoutFirst<E>(this.base.headSet(to), this.excluido);
    }

    public SortedSet<E> tailSet(E from) {
        return new TailWithoutFirst<E>(this.base.tailSet(from), this.excluido);
    }

    public SortedSet<E> subSet(E from, E to) {
        return new TailWithoutFirst<E>(this.base.subSet(from, to), this.excluido);
    }
}

/** A `SortedMap` seen backwards. */
final class ReverseSortedMap<K, V> extends AbstractMap<K, V> implements SortedMap<K, V> {

    private final SortedMap<K, V> base;

    ReverseSortedMap(SortedMap<K, V> base) {
        this.base = base;
    }

    public Comparator<? super K> comparator() {
        Comparator<? super K> c = this.base.comparator();
        if (c == null) {
            return (Comparator<? super K>) Collections.reverseOrder();
        }
        return Collections.reverseOrder(c);
    }

    public int size() {
        return this.base.size();
    }

    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.base.containsKey(key);
    }

    public V get(Object key) {
        return this.base.get(key);
    }

    public V put(K key, V value) {
        return this.base.put(key, value);
    }

    public V remove(Object key) {
        return this.base.remove(key);
    }

    public void clear() {
        this.base.clear();
    }

    public SortedMap<K, V> reversed() {
        return this.base;
    }

    public K firstKey() {
        return this.base.lastKey();
    }

    public K lastKey() {
        return this.base.firstKey();
    }

    public SortedMap<K, V> headMap(K to) {
        return new ReverseSortedMap<K, V>(this.base.tailMap(to));
    }

    public SortedMap<K, V> tailMap(K from) {
        return new ReverseSortedMap<K, V>(this.base.headMap(from));
    }

    public SortedMap<K, V> subMap(K from, K to) {
        return new ReverseSortedMap<K, V>(this.base.subMap(to, from));
    }

    // `AbstractMap` leaves `entrySet()` abstract and builds `toString`, `equals` and the two derived
    // collections on it, so without this the class compiled and blew up at the first traversal. The
    // view is the one below: the same entries of the map behind, walked backwards.
    public Set<Map.Entry<K, V>> entrySet() {
        return new ReverseSortedEntries<K, V>(this.base);
    }
}

// The entries of a reversed `SortedMap`.
//
// It keeps **no entries**, for the same reason as the rest of the file: it keeps the map and a way of
// walking its keys backwards. And the entries it returns are **live** --they read and write against
// the map-- because an entry from `entrySet()` has to be able to `setValue`; a copy with the pair
// inside would compile all the same and lose the writes in silence, which is the kind of error this
// file exists in order not to commit.
final class ReverseSortedEntries<K, V> extends AbstractSet<Map.Entry<K, V>> {

    private final SortedMap<K, V> base;

    ReverseSortedEntries(SortedMap<K, V> base) {
        this.base = base;
    }

    public int size() {
        return this.base.size();
    }

    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    public void clear() {
        this.base.clear();
    }

    // Membership and removal are delegated to the set of the map behind: they are the same entries,
    // and the only thing that changes between the two views is the traversal order.
    public boolean contains(Object o) {
        return this.base.entrySet().contains(o);
    }

    public boolean remove(Object o) {
        return this.base.entrySet().remove(o);
    }

    public Iterator<Map.Entry<K, V>> iterator() {
        return new ReverseSortedEntriesItr<K, V>(this.base);
    }
}

// The backward traversal, with the same technique as `ReverseSortedItr` but over the map's keys: the
// one before `k` is `headMap(k).lastKey()`.
final class ReverseSortedEntriesItr<K, V> implements Iterator<Map.Entry<K, V>> {

    private final SortedMap<K, V> base;
    private K upcoming;
    private boolean hasAny;
    private boolean started = false;

    ReverseSortedEntriesItr(SortedMap<K, V> base) {
        this.base = base;
    }

    private void startThread() {
        if (!this.started) {
            this.started = true;
            this.hasAny = !this.base.isEmpty();
            if (this.hasAny) {
                this.upcoming = this.base.lastKey();
            }
        }
    }

    public boolean hasNext() {
        this.startThread();
        return this.hasAny;
    }

    public Map.Entry<K, V> next() {
        this.startThread();
        if (!this.hasAny) {
            throw new NoSuchElementException();
        }
        K current = this.upcoming;
        SortedMap<K, V> before = this.base.headMap(current);
        if (before.isEmpty()) {
            this.hasAny = false;
        } else {
            this.upcoming = before.lastKey();
        }
        return new ReverseSortedEntry<K, V>(this.base, current);
    }
}

// A live entry: the key is fixed and the value is read from and written to the map.
final class ReverseSortedEntry<K, V> implements Map.Entry<K, V> {

    private final SortedMap<K, V> base;
    private final K entryKey;

    ReverseSortedEntry(SortedMap<K, V> base, K entryKey) {
        this.base = base;
        this.entryKey = entryKey;
    }

    public K getKey() {
        return this.entryKey;
    }

    public V getValue() {
        return this.base.get(this.entryKey);
    }

    public V setValue(V value) {
        return this.base.put(this.entryKey, value);
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
        K k = this.getKey();
        V v = this.getValue();
        boolean sameKey = k == null ? other.getKey() == null : k.equals(other.getKey());
        boolean sameValue = v == null ? other.getValue() == null : v.equals(other.getValue());
        return sameKey && sameValue;
    }

    // `Map.Entry`'s contract's: the XOR of the two, with null counting as zero.
    public int hashCode() {
        K k = this.getKey();
        V v = this.getValue();
        return (k == null ? 0 : k.hashCode()) ^ (v == null ? 0 : v.hashCode());
    }

    public String toString() {
        return this.getKey() + "=" + this.getValue();
    }
}

/** A `Deque` seen backwards: the two ends swapped. */
final class ReverseDeque<E> extends AbstractCollection<E> implements Deque<E> {

    private final Deque<E> base;

    ReverseDeque(Deque<E> base) {
        this.base = base;
    }

    public int size() {
        return this.base.size();
    }

    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    public void clear() {
        this.base.clear();
    }

    public boolean contains(Object o) {
        return this.base.contains(o);
    }

    public Iterator<E> iterator() {
        return this.base.descendingIterator();
    }

    public Iterator<E> descendingIterator() {
        return this.base.iterator();
    }

    public Deque<E> reversed() {
        return this.base;
    }

    // All the rest is the same method with the end changed.
    public void addFirst(E e) {
        this.base.addLast(e);
    }

    public void addLast(E e) {
        this.base.addFirst(e);
    }

    public boolean offerFirst(E e) {
        return this.base.offerLast(e);
    }

    public boolean offerLast(E e) {
        return this.base.offerFirst(e);
    }

    public E removeFirst() {
        return this.base.removeLast();
    }

    public E removeLast() {
        return this.base.removeFirst();
    }

    public E pollFirst() {
        return this.base.pollLast();
    }

    public E pollLast() {
        return this.base.pollFirst();
    }

    public E getFirst() {
        return this.base.getLast();
    }

    public E getLast() {
        return this.base.getFirst();
    }

    public E peekFirst() {
        return this.base.peekLast();
    }

    public E peekLast() {
        return this.base.peekFirst();
    }

    public boolean removeFirstOccurrence(Object o) {
        return this.base.removeLastOccurrence(o);
    }

    public boolean removeLastOccurrence(Object o) {
        return this.base.removeFirstOccurrence(o);
    }

    // `Queue`'s and the stack's, which in a Deque are synonyms of the ones above. They are written in
    // terms of **this** view, not of the one behind: `push` pushes onto the view's front.
    public boolean add(E e) {
        this.addLast(e);
        return true;
    }

    public boolean offer(E e) {
        return this.offerLast(e);
    }

    public E remove() {
        return this.removeFirst();
    }

    public E poll() {
        return this.pollFirst();
    }

    public E element() {
        return this.getFirst();
    }

    public E peek() {
        return this.peekFirst();
    }

    public void push(E e) {
        this.addFirst(e);
    }

    public E pop() {
        return this.removeFirst();
    }

    public boolean remove(Object o) {
        return this.removeFirstOccurrence(o);
    }
}
