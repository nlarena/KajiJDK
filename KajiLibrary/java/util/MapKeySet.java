package java.util;

// The set `Map.keySet()` returns: a live view of the map's keys.
//
// "Live" is worth pinning down, because it is live in three of its four parts and not in the
// fourth. `size`, `contains`, `remove` and `clear` all ask the map at the moment they are called,
// so removing a key here removes it from the map -- which is the half that matters and the half
// that used to be missing, since `keySet()` handed back a fresh `HashSet` that shared nothing.
//
// The ITERATOR walks a snapshot taken when it is created. That is a deliberate weakening of the
// JDK's contract, which would throw `ConcurrentModificationException` instead: code that iterates
// a key set while removing from the map is common in this library and used to be safe against the
// old copy, and quietly turning it into a crash would be trading one bug for a louder one. What
// the snapshot iterator cannot do is see a key added mid-walk, and `remove()` on it goes to the
// map, so the removal still lands.
final class MapKeySet<K, V> extends AbstractSet<K> {

    private final HashMap<K, V> map;

    MapKeySet(HashMap<K, V> map) {
        this.map = map;
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

    public Iterator<K> iterator() {
        return new MapKeySetItr<K, V>(this.map, this.map.keyArray());
    }
}

// Walks the snapshot; `remove` goes to the map, so it writes through like the set's own remove.
final class MapKeySetItr<K, V> implements Iterator<K> {

    private final HashMap<K, V> map;
    private final Object[] keys;
    private int i;
    private boolean removable;

    MapKeySetItr(HashMap<K, V> map, Object[] keys) {
        this.map = map;
        this.keys = keys;
        this.i = 0;
        this.removable = false;
    }

    public boolean hasNext() {
        return this.i < this.keys.length;
    }

    public K next() {
        if (this.i >= this.keys.length) {
            throw new NoSuchElementException();
        }
        Object k = this.keys[this.i];
        this.i = this.i + 1;
        this.removable = true;
        return (K) k;
    }

    public void remove() {
        if (!this.removable) {
            throw new IllegalStateException();
        }
        this.map.remove(this.keys[this.i - 1]);
        this.removable = false;
    }
}
