package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

// The synchronized hash table from Java 1.0 — what {@link HashMap} replaced in 1.2. The two
// differ in three ways worth knowing, and only the first is the famous one:
//
//   1. Every operation takes the table's monitor. As with {@link Vector}, locking each
//      *operation* is not locking each *transaction*, so a caller who cares still has to
//      synchronize externally — and one who doesn't care is paying for a lock that buys
//      nothing. That is the whole argument for HashMap.
//   2. Null keys and null values are rejected. HashMap allows both. Hashtable's refusal is
//      not an oversight: `get` returning null has to mean "absent", and once null is a legal
//      *value* it stops meaning that. HashMap chose the opposite trade and gained
//      `containsKey` as the way to tell the two apart.
//   3. It is a {@link Dictionary}, an abstract *class*, so it could never also extend
//      anything else. HashMap implements the Map *interface* — the fix the 1.2 collections
//      were designed around.
//
// Internally this is **separate chaining**: the table is an array of buckets, each holding a
// singly-linked list of the entries whose hash lands there. That is the JDK's Hashtable and
// the deliberate contrast with KajiLibrary's HashMap, which uses open addressing (one entry
// per slot, collisions probed to the next free one). Chaining costs a node object per entry
// and a pointer chase per step, but a collision only lengthens one bucket instead of stealing
// a slot from a neighbour, and removal is a simple unlink instead of a cluster re-insert.
//
// The bucket index is `(hash & 0x7FFFFFFF) % length` — the mask forces the sign bit off
// (a negative hash would give a negative index) and the modulo, over an odd table length,
// mixes all the hash bits rather than only the low ones a power-of-two mask would keep.
//
// A note on how this file is written: our javac accepts the `synchronized` *method modifier*
// but does not emit ACC_SYNCHRONIZED for it, so every public method below uses an explicit
// `synchronized (this)` block instead — identical semantics, and exactly the monitor a
// synchronized method would take. Each block assigns to a local and returns *after* the
// block, never from inside it: a `return` from within a synchronized block emits no
// monitorexit and leaks the monitor forever (finding #105). A `throw` is safe.
//
// Subset of the JDK's: the collection views (keySet/values/entrySet), putAll, clone, the
// default-method family (getOrDefault/compute/merge/…) and serialization are not modelled.
// equals/hashCode are omitted too, and for a reason rather than for effort: Map equality is
// defined over entry sets, and our `Map` has no entrySet to compare against an arbitrary
// other Map with.
public class Hashtable<K, V> extends Dictionary<K, V> implements Map<K, V> {

    // The buckets. `table[i]` is the head of the chain for index i, or null for an empty
    // bucket. Package-private so the enumerator can walk it without going through the lock.
    HtEntry<K, V>[] table;

    // Live entries across all chains — the map's size, not the array's length.
    private int count;

    // Rehash once `count` passes this. Kept as a field rather than recomputed so the common
    // path is one int comparison.
    private int threshold;

    // Entries per bucket we are willing to average before growing. 0.75 is the usual
    // compromise: high enough that most of the array is used, low enough that most chains
    // stay length 1.
    private float loadFactor;

    public Hashtable() {
        init(11, 0.75f);
    }

    public Hashtable(int initialCapacity) {
        init(initialCapacity, 0.75f);
    }

    public Hashtable(int initialCapacity, float loadFactor) {
        init(initialCapacity, loadFactor);
    }

    // 11 is the JDK's default length, and odd on purpose: with `% length` an even length
    // would throw away the hash's top bit for every key whose hash is even.
    private void init(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity");
        }
        if (loadFactor <= 0.0f) {
            throw new IllegalArgumentException("Illegal Load factor");
        }
        int cap = initialCapacity;
        if (cap == 0) {
            cap = 1;
        }
        this.loadFactor = loadFactor;
        this.table = new HtEntry[cap];
        this.threshold = (int) ((float) cap * loadFactor);
    }

    // --- unsynchronized internals ---------------------------------------------------
    //
    // The real work lives here, lock-free; the public methods are a thin locked shell over
    // these. Keeping the two apart means the locking is visible in one place instead of
    // tangled through the chain walking — and it lets `put` call itself from `rehash`
    // without thinking about reentrancy.

    // Which bucket a hash belongs in. The mask clears the sign bit: `%` on a negative int
    // yields a negative result, which would index out of the array.
    private int indexFor(int hash) {
        return (hash & 0x7FFFFFFF) % table.length;
    }

    // The entry holding `key`, or null. `e.key` is read into an `Object` local before
    // `equals` is called on it: a call on a receiver whose static type is a *type variable*
    // is silently dropped by our javac (finding #111).
    private HtEntry<K, V> entryFor(Object key) {
        int hash = key.hashCode();
        HtEntry<K, V> e = table[indexFor(hash)];
        HtEntry<K, V> found = null;
        while (e != null && found == null) {
            if (e.hash == hash) {
                Object k = e.key;
                if (k.equals(key)) {
                    found = e;
                }
            }
            e = e.next;
        }
        return found;
    }

    private V putUnlocked(K key, V value) {
        // Both rejections are the point of the class, so they come before anything else.
        // A `throw` from inside the caller's synchronized block is fine — the JVM's
        // exception path runs monitorexit; it is `return` that our javac forgets (#105).
        if (value == null) {
            throw new NullPointerException();
        }
        Object k = key;
        int hash = k.hashCode();
        V old = null;
        HtEntry<K, V> e = table[indexFor(hash)];
        boolean replaced = false;
        while (e != null && !replaced) {
            if (e.hash == hash) {
                Object ek = e.key;
                if (ek.equals(key)) {
                    old = e.value;
                    e.value = value;
                    replaced = true;
                }
            }
            e = e.next;
        }
        if (!replaced) {
            if (count >= threshold) {
                rehashUnlocked();
            }
            // New entries go at the *head* of their chain: O(1), and no traversal to find
            // the tail. Nothing observable depends on chain order.
            int index = indexFor(hash);
            table[index] = new HtEntry<K, V>(hash, key, value, table[index]);
            count = count + 1;
        }
        return old;
    }

    // Grow to roughly twice the length, keeping it odd for the same reason 11 is, and
    // re-file every entry: a bucket index is a function of the table length, so every
    // entry's index changes when the length does.
    private void rehashUnlocked() {
        HtEntry<K, V>[] old = table;
        int newLength = old.length * 2 + 1;
        table = new HtEntry[newLength];
        threshold = (int) ((float) newLength * loadFactor);
        for (int i = 0; i < old.length; i++) {
            HtEntry<K, V> e = old[i];
            while (e != null) {
                HtEntry<K, V> next = e.next;
                int index = indexFor(e.hash);
                e.next = table[index];
                table[index] = e;
                e = next;
            }
        }
    }

    private V removeUnlocked(Object key) {
        int hash = key.hashCode();
        int index = indexFor(hash);
        HtEntry<K, V> e = table[index];
        HtEntry<K, V> prev = null;
        V old = null;
        boolean done = false;
        while (e != null && !done) {
            boolean hit = false;
            if (e.hash == hash) {
                Object k = e.key;
                hit = k.equals(key);
            }
            if (hit) {
                // Unlink: the previous node (or the bucket head) skips over this one.
                if (prev == null) {
                    table[index] = e.next;
                } else {
                    prev.next = e.next;
                }
                old = e.value;
                count = count - 1;
                done = true;
            } else {
                prev = e;
                e = e.next;
            }
        }
        return old;
    }

    private boolean containsValueUnlocked(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        boolean found = false;
        for (int i = 0; i < table.length; i++) {
            HtEntry<K, V> e = table[i];
            while (e != null) {
                Object v = e.value;
                if (value.equals(v)) {
                    found = true;
                }
                e = e.next;
            }
        }
        return found;
    }

    // --- Dictionary / Map -----------------------------------------------------------

    public int size() {
        int n;
        synchronized (this) {
            n = count;
        }
        return n;
    }

    public boolean isEmpty() {
        boolean empty;
        synchronized (this) {
            empty = count == 0;
        }
        return empty;
    }

    public boolean containsKey(Object key) {
        boolean found;
        synchronized (this) {
            found = entryFor(key) != null;
        }
        return found;
    }

    // Searches by *value*, the 1.0 spelling of what 1.2 named containsValue. Note the
    // asymmetry it caused: `contains` on a Map means "contains this value", while `contains`
    // on a Collection means "contains this element" — one of the naming accidents the
    // collections framework was introduced to clean up.
    public boolean contains(Object value) {
        boolean found;
        synchronized (this) {
            found = containsValueUnlocked(value);
        }
        return found;
    }

    public boolean containsValue(Object value) {
        return contains(value);
    }

    public V get(Object key) {
        Object v;
        synchronized (this) {
            HtEntry<K, V> e = entryFor(key);
            if (e == null) {
                v = null;
            } else {
                v = e.value;
            }
        }
        return (V) v;
    }

    public V put(K key, V value) {
        Object old;
        synchronized (this) {
            old = putUnlocked(key, value);
        }
        return (V) old;
    }

    public V remove(Object key) {
        Object old;
        synchronized (this) {
            old = removeUnlocked(key);
        }
        return (V) old;
    }

    public void clear() {
        synchronized (this) {
            for (int i = 0; i < table.length; i++) {
                table[i] = null;
            }
            count = 0;
        }
    }

    // Protected because it is a subclass hook, exactly as in the JDK: a subclass may want to
    // know when the table grows (or to grow it itself). It locks, so a subclass calling it
    // from an already-locked method simply re-enters the monitor.
    protected void rehash() {
        synchronized (this) {
            rehashUnlocked();
        }
    }

    // --- the 1.0 iteration ----------------------------------------------------------
    //
    // Two names for one walk, over two different projections of the same entries. This is
    // what {@link Map}'s keySet/values/entrySet views replaced: an Enumeration can only be
    // consumed, while a view is a live Collection you can pass on, filter, or remove through.

    public Enumeration<K> keys() {
        return new HashtableEnumerator<K, V, K>(this, true);
    }

    public Enumeration<V> elements() {
        return new HashtableEnumerator<K, V, V>(this, false);
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        synchronized (this) {
            b.append('{');
            boolean first = true;
            for (int i = 0; i < table.length; i++) {
                HtEntry<K, V> e = table[i];
                while (e != null) {
                    if (!first) {
                        b.append(',');
                        b.append(' ');
                    }
                    first = false;
                    Object k = e.key;
                    Object v = e.value;
                    b.append(k.toString());
                    b.append('=');
                    b.append(v.toString());
                    e = e.next;
                }
            }
            b.append('}');
        }
        return b.toString();
    }
}

// One entry in a bucket's chain. Top-level package-private rather than nested, since a nested
// class inside a *generic* class is miscompiled (finding #13).
//
// The hash is cached in the node. It costs four bytes per entry and saves a `hashCode()` call
// on every comparison during a chain walk and on every entry during a rehash — which matters
// because for a key like String that call is not free.
final class HtEntry<K, V> {

    int hash;
    K key;
    V value;
    HtEntry<K, V> next;

    HtEntry(int hash, K key, V value, HtEntry<K, V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
}

// The walk behind keys() and elements(): bucket by bucket, chain by chain. One class for both
// because the traversal is identical and only the projection differs — `keys` picks the key
// out of the entry it lands on, `elements` picks the value.
//
// Deliberately *not* locked, like {@link Vector}'s: a lock per `nextElement()` would still not
// make the traversal atomic, so it would buy nothing but the illusion of safety. A caller who
// needs a consistent snapshot has to hold the table's monitor across the whole loop.
// The third type parameter is what the walk hands back — K for keys(), V for elements(). One
// traversal, two projections, and the caller still gets a properly typed Enumeration.
final class HashtableEnumerator<K, V, T> implements Enumeration<T> {

    private final Hashtable<K, V> ht;
    // true → hand back keys, false → hand back values.
    private final boolean keysNotValues;
    private int index;
    private HtEntry<K, V> entry;

    HashtableEnumerator(Hashtable<K, V> ht, boolean keysNotValues) {
        this.ht = ht;
        this.keysNotValues = keysNotValues;
        this.index = 0;
        advance();
    }

    // Park `entry` on the next live node: stay in the current chain if there is more of it,
    // otherwise scan forward for the next non-empty bucket.
    private void advance() {
        while (entry == null && index < ht.table.length) {
            entry = ht.table[index];
            index = index + 1;
        }
    }

    public boolean hasMoreElements() {
        return entry != null;
    }

    public T nextElement() {
        if (entry == null) {
            throw new NoSuchElementException();
        }
        Object result;
        if (keysNotValues) {
            result = entry.key;
        } else {
            result = entry.value;
        }
        entry = entry.next;
        advance();
        return (T) result;
    }
}
