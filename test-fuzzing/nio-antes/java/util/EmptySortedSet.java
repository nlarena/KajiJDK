package java.util;

// The empty immutable sorted set Collections.emptySortedSet() and emptyNavigableSet() return.
// Package-private.
//
// For List, Set and Map the fixed collections already there (`FixedList`, `FixedSet`, `FixedMap`)
// built over a zero-length array were enough. For the sorted variants they are not, because
// `SortedSet` and `NavigableSet` ask for twenty methods more than a Set has: comparator, the three
// slices, first/last, and all the navigation.
//
// Being empty, all of them have a trivial answer, and that triviality is precisely what makes it
// worth having separately: there is no state, there are no comparisons, and a single instance would
// be enough for every use. One is created per call all the same, which is cheaper than the map of
// instances that would be needed to share it safely across parameterisations.
//
// The slices (subSet/headSet/tailSet) return `this`: a slice of the empty is the empty. It is not
// validated that `from <= to`, which the JDK does do, throwing IllegalArgumentException. Said
// plainly.
final class EmptySortedSet<E> implements NavigableSet<E> {

    EmptySortedSet() {
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean contains(Object o) {
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }

    public Iterator<E> iterator() {
        return new FixedListItr<E>(new Object[0]);
    }

    public Iterator<E> descendingIterator() {
        return new FixedListItr<E>(new Object[0]);
    }

    public Object[] toArray() {
        return new Object[0];
    }

    public <T> T[] toArray(T[] a) {
        if (a.length > 0) {
            a[0] = null;
        }
        return a;
    }

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public NavigableSet<E> reversed() {
        return this;
    }

    // No comparator: the empty orders nothing, and `null` is how "natural order" is said.
    public Comparator<? super E> comparator() {
        return null;
    }

    public SortedSet<E> subSet(E from, E to) {
        return this;
    }

    public SortedSet<E> headSet(E to) {
        return this;
    }

    public SortedSet<E> tailSet(E from) {
        return this;
    }

    public NavigableSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive) {
        return this;
    }

    public NavigableSet<E> headSet(E to, boolean inclusive) {
        return this;
    }

    public NavigableSet<E> tailSet(E from, boolean inclusive) {
        return this;
    }

    public NavigableSet<E> descendingSet() {
        return this;
    }

    // first/last refuse; lower/floor/ceiling/higher return null. It is not an inconsistency: the
    // first two promise an element and there is none, the others already use null for "there is none
    // that satisfies it".
    public E first() {
        throw new NoSuchElementException();
    }

    public E last() {
        throw new NoSuchElementException();
    }

    public E lower(E e) {
        return null;
    }

    public E floor(E e) {
        return null;
    }

    public E ceiling(E e) {
        return null;
    }

    public E higher(E e) {
        return null;
    }

    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        return ((Set<?>) o).isEmpty();
    }

    public int hashCode() {
        return 0;
    }

    public String toString() {
        return "[]";
    }
}

// The empty immutable sorted map, for the same reason and in the same shape.
final class EmptySortedMap<K, V> implements NavigableMap<K, V> {

    EmptySortedMap() {
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean containsKey(Object key) {
        return false;
    }

    public boolean containsValue(Object value) {
        return false;
    }

    public V get(Object key) {
        return null;
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Set<K> keySet() {
        return FixedSet.fromArray(new Object[0], 0);
    }

    public Collection<V> values() {
        return new FixedList<V>(new Object[0]);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return FixedSet.fromArray(new Object[0], 0);
    }

    public NavigableMap<K, V> reversed() {
        return this;
    }

    public Comparator<? super K> comparator() {
        return null;
    }

    public SortedMap<K, V> subMap(K from, K to) {
        return this;
    }

    public SortedMap<K, V> headMap(K to) {
        return this;
    }

    public SortedMap<K, V> tailMap(K from) {
        return this;
    }

    public NavigableMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
        return this;
    }

    public NavigableMap<K, V> headMap(K to, boolean inclusive) {
        return this;
    }

    public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
        return this;
    }

    public NavigableMap<K, V> descendingMap() {
        return this;
    }

    public NavigableSet<K> navigableKeySet() {
        return new EmptySortedSet<K>();
    }

    public NavigableSet<K> descendingKeySet() {
        return new EmptySortedSet<K>();
    }

    public K firstKey() {
        throw new NoSuchElementException();
    }

    public K lastKey() {
        throw new NoSuchElementException();
    }

    public Map.Entry<K, V> firstEntry() {
        return null;
    }

    public Map.Entry<K, V> lastEntry() {
        return null;
    }

    public Map.Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    public Map.Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    public V putFirst(K k, V v) {
        throw new UnsupportedOperationException();
    }

    public V putLast(K k, V v) {
        throw new UnsupportedOperationException();
    }

    public Map.Entry<K, V> lowerEntry(K key) {
        return null;
    }

    public K lowerKey(K key) {
        return null;
    }

    public Map.Entry<K, V> floorEntry(K key) {
        return null;
    }

    public K floorKey(K key) {
        return null;
    }

    public Map.Entry<K, V> ceilingEntry(K key) {
        return null;
    }

    public K ceilingKey(K key) {
        return null;
    }

    public Map.Entry<K, V> higherEntry(K key) {
        return null;
    }

    public K higherKey(K key) {
        return null;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        return ((Map<?, ?>) o).isEmpty();
    }

    public int hashCode() {
        return 0;
    }

    public String toString() {
        return "{}";
    }
}
