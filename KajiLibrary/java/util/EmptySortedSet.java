package java.util;

// El conjunto ordenado vacio e inmutable que devuelven Collections.emptySortedSet() y
// emptyNavigableSet(). Package-private.
//
// Para List, Set y Map alcanzaba con las colecciones fijas que ya estaban (`FixedList`,
// `FixedSet`, `FixedMap`) construidas sobre un arreglo de largo cero. Para las variantes
// ordenadas no alcanza, porque `SortedSet` y `NavigableSet` piden veinte metodos mas que un Set
// no tiene: comparator, los tres cortes, first/last, y toda la navegacion.
//
// Al ser vacio, todos tienen una respuesta trivial, y esa trivialidad es justamente lo que hace
// que valga la pena tenerlo aparte: no hay estado, no hay comparaciones, y una sola instancia
// alcanzaria para todos los usos. Se crea una por llamada igual, que es mas barato que el mapa de
// instancias que haria falta para compartirla con seguridad entre parametrizaciones.
//
// Los cortes (subSet/headSet/tailSet) devuelven `this`: un corte de lo vacio es lo vacio. No se
// valida que `from <= to`, cosa que el JDK si hace y tira IllegalArgumentException. Queda dicho.
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

    // Sin comparador: lo vacio no ordena nada, y `null` es como se dice "orden natural".
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

    // first/last se niegan; lower/floor/ceiling/higher devuelven null. No es una inconsistencia:
    // los primeros prometen un elemento y no lo hay, los segundos ya usan null para "no hay
    // ninguno que cumpla".
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

// El mapa ordenado vacio e inmutable, por la misma razon y con la misma forma.
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
