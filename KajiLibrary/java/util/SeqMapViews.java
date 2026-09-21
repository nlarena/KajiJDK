package java.util;

// The three sequenced views of a {@link SequencedMap}, built on `keySet()`, `values()` and
// `entrySet()`.
//
// They are named package-private classes and not anonymous ones for the reason the rest of this
// library gives: an anonymous class inside an interface `default` drags a capture this compiler
// handles worse than a field does.
//
// All three are VIEWS and not copies: they read the map on every call, so a change to the map shows
// through and a removal writes back. `reversed()` is the same view over `map.reversed()`, which is
// what lets one implementation serve both directions -- the reversal lives in the map, not here.
//
// The four end operations are not here: `SequencedCollection` writes them on `iterator()` and
// `reversed()`, which is exactly what these three would have written.

final class SeqMapKeySet<K, V> extends AbstractSet<K> implements SequencedSet<K> {

    private final SequencedMap<K, V> map;

    SeqMapKeySet(SequencedMap<K, V> map) {
        this.map = map;
    }

    public Iterator<K> iterator() {
        return this.map.keySet().iterator();
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

    public boolean remove(Object o) {
        if (!this.map.containsKey(o)) {
            return false;
        }
        this.map.remove(o);
        return true;
    }

    public void clear() {
        this.map.clear();
    }

    public SequencedSet<K> reversed() {
        return new SeqMapKeySet<K, V>(this.map.reversed());
    }
}

// The values, in the map's encounter order. A collection and not a set: two keys may map to the
// same value, and dropping the duplicate would be describing a different map.
final class SeqMapValues<K, V> extends AbstractCollection<V> implements SequencedCollection<V> {

    private final SequencedMap<K, V> map;

    SeqMapValues(SequencedMap<K, V> map) {
        this.map = map;
    }

    public Iterator<V> iterator() {
        return this.map.values().iterator();
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public boolean contains(Object o) {
        return this.map.containsValue(o);
    }

    public void clear() {
        this.map.clear();
    }

    public SequencedCollection<V> reversed() {
        return new SeqMapValues<K, V>(this.map.reversed());
    }
}

// The entries. `contains` compares key AND value, which is what an entry set means: an entry whose
// key is there with another value is not in this set.
final class SeqMapEntrySet<K, V> extends AbstractSet<Map.Entry<K, V>>
        implements SequencedSet<Map.Entry<K, V>> {

    private final SequencedMap<K, V> map;

    SeqMapEntrySet(SequencedMap<K, V> map) {
        this.map = map;
    }

    public Iterator<Map.Entry<K, V>> iterator() {
        return this.map.entrySet().iterator();
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
        Object k = e.getKey();
        if (!this.map.containsKey(k)) {
            return false;
        }
        Object mine = this.map.get(k);
        Object theirs = e.getValue();
        return mine == null ? theirs == null : mine.equals(theirs);
    }

    public boolean remove(Object o) {
        if (!this.contains(o)) {
            return false;
        }
        this.map.remove(((Map.Entry<?, ?>) o).getKey());
        return true;
    }

    public void clear() {
        this.map.clear();
    }

    public SequencedSet<Map.Entry<K, V>> reversed() {
        return new SeqMapEntrySet<K, V>(this.map.reversed());
    }
}
