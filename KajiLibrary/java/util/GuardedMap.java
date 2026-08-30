package java.util;

// El lado Map de los envoltorios de Collections: unmodifiableMap, synchronizedMap y checkedMap,
// con sus variantes ordenadas y navegables. Misma idea que GuardedCollection -- una sola familia
// con tres interruptores -- y las mismas reglas sobre el cerrojo.
//
// Lo especifico de un mapa son las tres **vistas**: keySet, values y entrySet. Todas tienen que
// salir envueltas, o serian el atajo obvio para modificar un mapa de solo lectura: bastaria con
// `unmodifiableMap(m).keySet().remove(k)`. Y entrySet necesita ademas que cada entrada salga
// envuelta, porque `Map.Entry.setValue` escribe en el mapa sin pasar por ninguna de sus vistas.
//
// Divergencia anotada: en el JDK las vistas de un synchronizedMap comparten el mutex del mapa,
// asi que `synchronized (mapa) { ... }` tambien excluye a quien recorre el keySet. Aca cada vista
// toma su propio monitor. Para el uso normal -- sincronizar sobre el mapa mientras se lo recorre
// entero -- da lo mismo; para el que mezcle vistas y mapa desde dos hilos, no.
class GuardedMap<K, V> implements Map<K, V> {

    final Map<K, V> back;
    final Class<K> keyType;
    final Class<V> valueType;
    final boolean readOnly;

    GuardedMap(Map<K, V> back, Class<K> keyType, Class<V> valueType, boolean readOnly) {
        if (back == null) {
            throw new NullPointerException();
        }
        this.back = back;
        this.keyType = keyType;
        this.valueType = valueType;
        this.readOnly = readOnly;
    }

    final void noWrite() {
        if (this.readOnly) {
            throw new UnsupportedOperationException();
        }
    }

    final void check(K key, V value) {
        if (this.keyType != null && key != null && !this.keyType.isInstance(key)) {
            throw new ClassCastException("Attempt to insert " + key.getClass().getName()
                    + " key into map with key type " + this.keyType.getName());
        }
        if (this.valueType != null && value != null && !this.valueType.isInstance(value)) {
            throw new ClassCastException("Attempt to insert " + value.getClass().getName()
                    + " value into map with value type " + this.valueType.getName());
        }
    }

    public int size() {
        synchronized (this) {
            return this.back.size();
        }
    }

    public boolean isEmpty() {
        synchronized (this) {
            return this.back.isEmpty();
        }
    }

    public boolean containsKey(Object key) {
        synchronized (this) {
            return this.back.containsKey(key);
        }
    }

    public boolean containsValue(Object value) {
        synchronized (this) {
            return this.back.containsValue(value);
        }
    }

    public V get(Object key) {
        synchronized (this) {
            return this.back.get(key);
        }
    }

    public V put(K key, V value) {
        this.noWrite();
        this.check(key, value);
        synchronized (this) {
            return this.back.put(key, value);
        }
    }

    public V remove(Object key) {
        this.noWrite();
        synchronized (this) {
            return this.back.remove(key);
        }
    }

    // Se valida el lote entero antes de escribir nada: dejar un mapa a medio copiar y despues
    // tirar ClassCastException seria peor que no copiar.
    public void putAll(Map<? extends K, ? extends V> m) {
        this.noWrite();
        if (this.keyType != null || this.valueType != null) {
            Iterator<? extends K> it = m.keySet().iterator();
            while (it.hasNext()) {
                K k = it.next();
                this.check(k, m.get(k));
            }
        }
        synchronized (this) {
            this.back.putAll(m);
        }
    }

    public void clear() {
        this.noWrite();
        synchronized (this) {
            this.back.clear();
        }
    }

    public Set<K> keySet() {
        synchronized (this) {
            return new GuardedSet<K>(this.back.keySet(), null, this.readOnly);
        }
    }

    public Collection<V> values() {
        synchronized (this) {
            return new GuardedCollection<V>(this.back.values(), null, this.readOnly);
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        synchronized (this) {
            if (this.readOnly) {
                return new GuardedEntrySet<K, V>(this.back.entrySet());
            }
            return this.back.entrySet();
        }
    }

    // Los defaults de Map que escriben. Van explicitos aunque el `put` de abajo ya se negaria:
    // `putIfAbsent` sobre una clave presente no llega a llamarlo, y devolveria el valor viejo tan
    // campante en un mapa que se declaro de solo lectura.
    public V putIfAbsent(K key, V value) {
        this.noWrite();
        this.check(key, value);
        synchronized (this) {
            return this.back.putIfAbsent(key, value);
        }
    }

    public boolean remove(Object key, Object value) {
        this.noWrite();
        synchronized (this) {
            return this.back.remove(key, value);
        }
    }

    public boolean replace(K key, V oldValue, V newValue) {
        this.noWrite();
        this.check(key, newValue);
        synchronized (this) {
            return this.back.replace(key, oldValue, newValue);
        }
    }

    public V replace(K key, V value) {
        this.noWrite();
        this.check(key, value);
        synchronized (this) {
            return this.back.replace(key, value);
        }
    }

    public void replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V> function) {
        this.noWrite();
        synchronized (this) {
            this.back.replaceAll(function);
        }
    }

    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        this.noWrite();
        synchronized (this) {
            return this.back.computeIfAbsent(key, mappingFunction);
        }
    }

    public V computeIfPresent(K key,
            java.util.function.BiFunction<? super K, ? super V, ? extends V> f) {
        this.noWrite();
        synchronized (this) {
            return this.back.computeIfPresent(key, f);
        }
    }

    public V compute(K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> f) {
        this.noWrite();
        synchronized (this) {
            return this.back.compute(key, f);
        }
    }

    public V merge(K key, V value, java.util.function.BiFunction<? super V, ? super V, ? extends V> f) {
        this.noWrite();
        synchronized (this) {
            return this.back.merge(key, value, f);
        }
    }

    public V getOrDefault(Object key, V defaultValue) {
        synchronized (this) {
            return this.back.getOrDefault(key, defaultValue);
        }
    }

    public void forEach(java.util.function.BiConsumer<? super K, ? super V> action) {
        synchronized (this) {
            this.back.forEach(action);
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        synchronized (this) {
            return this.back.equals(o);
        }
    }

    public int hashCode() {
        synchronized (this) {
            return this.back.hashCode();
        }
    }

    public String toString() {
        synchronized (this) {
            return this.back.toString();
        }
    }
}

class GuardedSequencedMap<K, V> extends GuardedMap<K, V> implements SequencedMap<K, V> {

    GuardedSequencedMap(SequencedMap<K, V> back, Class<K> keyType, Class<V> valueType,
            boolean readOnly) {
        super(back, keyType, valueType, readOnly);
    }

    final SequencedMap<K, V> seq() {
        return (SequencedMap<K, V>) this.back;
    }

    public SequencedMap<K, V> reversed() {
        synchronized (this) {
            return new GuardedSequencedMap<K, V>(this.seq().reversed(), this.keyType,
                    this.valueType, this.readOnly);
        }
    }

    public Map.Entry<K, V> firstEntry() {
        synchronized (this) {
            return this.wrapEntry(this.seq().firstEntry());
        }
    }

    public Map.Entry<K, V> lastEntry() {
        synchronized (this) {
            return this.wrapEntry(this.seq().lastEntry());
        }
    }

    // Una entrada de solo lectura sale envuelta; una que se pueda escribir, tal cual.
    final Map.Entry<K, V> wrapEntry(Map.Entry<K, V> e) {
        if (e == null || !this.readOnly) {
            return e;
        }
        return new GuardedEntry<K, V>(e);
    }

    public Map.Entry<K, V> pollFirstEntry() {
        this.noWrite();
        synchronized (this) {
            return this.seq().pollFirstEntry();
        }
    }

    public Map.Entry<K, V> pollLastEntry() {
        this.noWrite();
        synchronized (this) {
            return this.seq().pollLastEntry();
        }
    }

    public V putFirst(K k, V v) {
        this.noWrite();
        this.check(k, v);
        synchronized (this) {
            return this.seq().putFirst(k, v);
        }
    }

    public V putLast(K k, V v) {
        this.noWrite();
        this.check(k, v);
        synchronized (this) {
            return this.seq().putLast(k, v);
        }
    }
}

class GuardedSortedMap<K, V> extends GuardedSequencedMap<K, V> implements SortedMap<K, V> {

    GuardedSortedMap(SortedMap<K, V> back, Class<K> keyType, Class<V> valueType, boolean readOnly) {
        super(back, keyType, valueType, readOnly);
    }

    final SortedMap<K, V> sorted() {
        return (SortedMap<K, V>) this.back;
    }

    public Comparator<? super K> comparator() {
        synchronized (this) {
            return this.sorted().comparator();
        }
    }

    public SortedMap<K, V> subMap(K from, K to) {
        synchronized (this) {
            return new GuardedSortedMap<K, V>(this.sorted().subMap(from, to), this.keyType,
                    this.valueType, this.readOnly);
        }
    }

    public SortedMap<K, V> headMap(K to) {
        synchronized (this) {
            return new GuardedSortedMap<K, V>(this.sorted().headMap(to), this.keyType,
                    this.valueType, this.readOnly);
        }
    }

    public SortedMap<K, V> tailMap(K from) {
        synchronized (this) {
            return new GuardedSortedMap<K, V>(this.sorted().tailMap(from), this.keyType,
                    this.valueType, this.readOnly);
        }
    }

    public K firstKey() {
        synchronized (this) {
            return this.sorted().firstKey();
        }
    }

    public K lastKey() {
        synchronized (this) {
            return this.sorted().lastKey();
        }
    }
}

class GuardedNavigableMap<K, V> extends GuardedSortedMap<K, V> implements NavigableMap<K, V> {

    GuardedNavigableMap(NavigableMap<K, V> back, Class<K> keyType, Class<V> valueType,
            boolean readOnly) {
        super(back, keyType, valueType, readOnly);
    }

    final NavigableMap<K, V> nav() {
        return (NavigableMap<K, V>) this.back;
    }

    public Map.Entry<K, V> lowerEntry(K key) {
        synchronized (this) {
            return this.wrapEntry(this.nav().lowerEntry(key));
        }
    }

    public K lowerKey(K key) {
        synchronized (this) {
            return this.nav().lowerKey(key);
        }
    }

    public Map.Entry<K, V> floorEntry(K key) {
        synchronized (this) {
            return this.wrapEntry(this.nav().floorEntry(key));
        }
    }

    public K floorKey(K key) {
        synchronized (this) {
            return this.nav().floorKey(key);
        }
    }

    public Map.Entry<K, V> ceilingEntry(K key) {
        synchronized (this) {
            return this.wrapEntry(this.nav().ceilingEntry(key));
        }
    }

    public K ceilingKey(K key) {
        synchronized (this) {
            return this.nav().ceilingKey(key);
        }
    }

    public Map.Entry<K, V> higherEntry(K key) {
        synchronized (this) {
            return this.wrapEntry(this.nav().higherEntry(key));
        }
    }

    public K higherKey(K key) {
        synchronized (this) {
            return this.nav().higherKey(key);
        }
    }

    public NavigableMap<K, V> descendingMap() {
        synchronized (this) {
            return new GuardedNavigableMap<K, V>(this.nav().descendingMap(), this.keyType,
                    this.valueType, this.readOnly);
        }
    }

    public NavigableSet<K> navigableKeySet() {
        synchronized (this) {
            return new GuardedNavigableSet<K>(this.nav().navigableKeySet(), null, this.readOnly);
        }
    }

    public NavigableSet<K> descendingKeySet() {
        synchronized (this) {
            return new GuardedNavigableSet<K>(this.nav().descendingKeySet(), null, this.readOnly);
        }
    }

    public NavigableMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
        synchronized (this) {
            NavigableMap<K, V> m = this.nav().subMap(from, fromInclusive, to, toInclusive);
            return new GuardedNavigableMap<K, V>(m, this.keyType, this.valueType, this.readOnly);
        }
    }

    public NavigableMap<K, V> headMap(K to, boolean inclusive) {
        synchronized (this) {
            return new GuardedNavigableMap<K, V>(this.nav().headMap(to, inclusive), this.keyType,
                    this.valueType, this.readOnly);
        }
    }

    public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
        synchronized (this) {
            return new GuardedNavigableMap<K, V>(this.nav().tailMap(from, inclusive), this.keyType,
                    this.valueType, this.readOnly);
        }
    }
}

// El entrySet de un mapa de solo lectura. Hereda todo de GuardedSet y solo cambia el iterador,
// que es de donde salen las entradas.
final class GuardedEntrySet<K, V> extends GuardedSet<Map.Entry<K, V>> {

    GuardedEntrySet(Set<Map.Entry<K, V>> back) {
        super(back, null, true);
    }

    public Iterator<Map.Entry<K, V>> iterator() {
        return new GuardedEntryItr<K, V>(this.back.iterator());
    }
}

final class GuardedEntryItr<K, V> implements Iterator<Map.Entry<K, V>> {

    private final Iterator<Map.Entry<K, V>> back;

    GuardedEntryItr(Iterator<Map.Entry<K, V>> back) {
        this.back = back;
    }

    public boolean hasNext() {
        return this.back.hasNext();
    }

    public Map.Entry<K, V> next() {
        return new GuardedEntry<K, V>(this.back.next());
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

// Una entrada que deja leer y no deja escribir. `equals`/`hashCode` delegan porque Map.Entry los
// define por contenido, y una entrada envuelta tiene que seguir encontrandose dentro del mismo
// entrySet.
final class GuardedEntry<K, V> implements Map.Entry<K, V> {

    private final Map.Entry<K, V> back;

    GuardedEntry(Map.Entry<K, V> back) {
        this.back = back;
    }

    public K getKey() {
        return this.back.getKey();
    }

    public V getValue() {
        return this.back.getValue();
    }

    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object o) {
        return this.back.equals(o);
    }

    public int hashCode() {
        return this.back.hashCode();
    }

    public String toString() {
        return this.back.toString();
    }
}
