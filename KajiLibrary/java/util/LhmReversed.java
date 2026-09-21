package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Map;
import java.util.Set;

// LinkedHashMap's sequenced views: the map backwards, and its three collections (keys, values,
// entries) in either direction.
//
// **Views, not copies.** It is the decision that governs the whole file. `m.reversed()` returns
// something that shares the same entry objects as `m`: putting a pair in on one side is seen on the
// other, and walking one while the other is modified is as valid (or as invalid) as doing it on the
// map directly. A copy would be several times shorter to write and would be wrong -- `reversed()`'s
// contract says "view", and a method that returns a snapshot when it promises a mirror lies at the
// one point where anyone cares.
//
// One single thing is what makes all of this cheap: LinkedHashMap's order list is **doubly linked**
// already. Walking it backwards costs the same as forwards, so reversing is choosing which of the two
// pointers to advance along. Hence the `rev` boolean that runs through the file: there are not two
// implementations, there is one with a direction of travel.

/** The map seen backwards. */
final class LhmReversed<K, V> extends AbstractMap<K, V> implements SequencedMap<K, V> {

    private final LinkedHashMap<K, V> base;

    LhmReversed(LinkedHashMap<K, V> base) {
        this.base = base;
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

    // A `put` on the reversed view adds at the **front of the view's order**, which is the end of the
    // order of the map behind. It is what makes the view consistent with itself: the last thing added
    // is iterated first, just as in the map directly.
    public V put(K key, V value) {
        return this.base.put(key, value);
    }

    public V remove(Object key) {
        return this.base.remove(key);
    }

    public void clear() {
        this.base.clear();
    }

    /** Reversing the reversed is the map behind, not a third wrapper. */
    public SequencedMap<K, V> reversed() {
        return this.base;
    }

    public V putFirst(K key, V value) {
        return this.base.putLast(key, value);
    }

    public V putLast(K key, V value) {
        return this.base.putFirst(key, value);
    }

    public Map.Entry<K, V> firstEntry() {
        return this.base.lastEntryOf();
    }

    public Map.Entry<K, V> lastEntry() {
        return this.base.firstEntryOf();
    }

    public Map.Entry<K, V> pollFirstEntry() {
        Map.Entry<K, V> e = this.firstEntry();
        if (e != null) {
            this.base.remove(e.getKey());
        }
        return e;
    }

    public Map.Entry<K, V> pollLastEntry() {
        Map.Entry<K, V> e = this.lastEntry();
        if (e != null) {
            this.base.remove(e.getKey());
        }
        return e;
    }

    public SequencedSet<K> sequencedKeySet() {
        return new LhmKeySet<K, V>(this.base, true);
    }

    public SequencedCollection<V> sequencedValues() {
        return new LhmValues<K, V>(this.base, true);
    }

    public SequencedSet<Map.Entry<K, V>> sequencedEntrySet() {
        return new LhmEntrySet<K, V>(this.base, true);
    }

    // `AbstractMap` leaves `entrySet()` abstract and builds everything else on it --`keySet`,
    // `values`, `toString`, `equals`-- so without this the class compiled but any of those blew up.
    // The reversed view exists already: it is the same one `sequencedEntrySet` returns, and a
    // `SequencedSet` is a `Set`. There is no second implementation, there is one object with two
    // names.
    public Set<Map.Entry<K, V>> entrySet() {
        return this.sequencedEntrySet();
    }
}

// The walk of the order list in one direction or the other. It is the only place in the file that
// knows the list exists; everything else rests on this.
final class LhmWalk<K, V> {

    private final LinkedHashMap<K, V> base;
    private final boolean rev;

    LhmWalk(LinkedHashMap<K, V> base, boolean rev) {
        this.base = base;
        this.rev = rev;
    }

    LhmEntry<K, V> first() {
        return this.rev ? this.base.lastEntryOf() : this.base.firstEntryOf();
    }

    LhmEntry<K, V> nextOf(LhmEntry<K, V> e) {
        return this.rev ? this.base.beforeEntry(e) : this.base.afterEntry(e);
    }
}

// The iterator over the entries, in whichever direction `rev` says. The three iterators below wrap
// it and project what each of them needs.
final class LhmEntryItr<K, V> implements Iterator<Map.Entry<K, V>> {

    private final LhmWalk<K, V> walk;
    private LhmEntry<K, V> upcoming;
    private boolean started = false;

    LhmEntryItr(LinkedHashMap<K, V> base, boolean rev) {
        this.walk = new LhmWalk<K, V>(base, rev);
    }

    private void startThread() {
        if (!this.started) {
            this.upcoming = this.walk.first();
            this.started = true;
        }
    }

    public boolean hasNext() {
        this.startThread();
        return this.upcoming != null;
    }

    public Map.Entry<K, V> next() {
        this.startThread();
        if (this.upcoming == null) {
            throw new NoSuchElementException();
        }
        LhmEntry<K, V> e = this.upcoming;
        this.upcoming = this.walk.nextOf(e);
        return e;
    }
}

final class LhmKeyItr<K, V> implements Iterator<K> {

    private final LhmEntryItr<K, V> it;

    LhmKeyItr(LinkedHashMap<K, V> base, boolean rev) {
        this.it = new LhmEntryItr<K, V>(base, rev);
    }

    public boolean hasNext() {
        return this.it.hasNext();
    }

    public K next() {
        return this.it.next().getKey();
    }
}

final class LhmValueItr<K, V> implements Iterator<V> {

    private final LhmEntryItr<K, V> it;

    LhmValueItr(LinkedHashMap<K, V> base, boolean rev) {
        this.it = new LhmEntryItr<K, V>(base, rev);
    }

    public boolean hasNext() {
        return this.it.hasNext();
    }

    public V next() {
        return this.it.next().getValue();
    }
}

/** The keys, in the map's order or backwards. */
final class LhmKeySet<K, V> extends AbstractSet<K> implements SequencedSet<K> {

    private final LinkedHashMap<K, V> base;
    private final boolean rev;

    LhmKeySet(LinkedHashMap<K, V> base, boolean rev) {
        this.base = base;
        this.rev = rev;
    }

    public Iterator<K> iterator() {
        return new LhmKeyItr<K, V>(this.base, this.rev);
    }

    public int size() {
        return this.base.size();
    }

    public boolean contains(Object o) {
        return this.base.containsKey(o);
    }

    // Taking a key out of the view takes it out of the map: that is what "view" means.
    public boolean remove(Object o) {
        boolean wasThere = this.base.containsKey(o);
        this.base.remove(o);
        return wasThere;
    }

    public void clear() {
        this.base.clear();
    }

    public SequencedSet<K> reversed() {
        return new LhmKeySet<K, V>(this.base, !this.rev);
    }
}

/** The values, in the map's order or backwards. */
final class LhmValues<K, V> extends AbstractCollection<V> implements SequencedCollection<V> {

    private final LinkedHashMap<K, V> base;
    private final boolean rev;

    LhmValues(LinkedHashMap<K, V> base, boolean rev) {
        this.base = base;
        this.rev = rev;
    }

    public Iterator<V> iterator() {
        return new LhmValueItr<K, V>(this.base, this.rev);
    }

    public int size() {
        return this.base.size();
    }

    public void clear() {
        this.base.clear();
    }

    public SequencedCollection<V> reversed() {
        return new LhmValues<K, V>(this.base, !this.rev);
    }
}

/** The entries, in the map's order or backwards. */
final class LhmEntrySet<K, V> extends AbstractSet<Map.Entry<K, V>>
        implements SequencedSet<Map.Entry<K, V>> {

    private final LinkedHashMap<K, V> base;
    private final boolean rev;

    LhmEntrySet(LinkedHashMap<K, V> base, boolean rev) {
        this.base = base;
        this.rev = rev;
    }

    public Iterator<Map.Entry<K, V>> iterator() {
        return new LhmEntryItr<K, V>(this.base, this.rev);
    }

    public int size() {
        return this.base.size();
    }

    public void clear() {
        this.base.clear();
    }

    public SequencedSet<Map.Entry<K, V>> reversed() {
        return new LhmEntrySet<K, V>(this.base, !this.rev);
    }
}
