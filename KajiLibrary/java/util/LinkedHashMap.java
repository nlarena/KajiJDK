package java.util;

// Same-package imports work around the frozen javac's finder (finding #4). `Map.Entry` is imported
// rather than written `Map.Entry`, because a qualified nested type is not resolved (finding #101).
import java.util.Map;

// A hash map that also remembers the **order its keys were first inserted in**. It is the map
// you reach for when a HashMap's arbitrary order would be a bug — building a config file whose
// sections must come out in the order they were written, or a JSON object whose fields must
// round-trip — and it is one of the cheapest useful data structures in the library: a hash
// table for the lookups, plus one doubly-linked list threaded through the very same entry
// objects for the order. No second table, no sorting, no comparator; two extra pointers per
// entry and O(1) on every operation, exactly like a plain HashMap.
//
// The second mode is what makes it more than a tidy HashMap. With `accessOrder = true` the
// list is reordered on every *lookup* as well, so the head is always the least recently used
// entry — and the {@link #removeEldestEntry} hook, called after each insertion, decides
// whether to evict it. Those two together are an LRU cache in about five lines of subclass:
//
//     new LinkedHashMap<K,V>(16, 0.75f, true) { protected boolean removeEldestEntry(...) {
//         return size() > capacity; } }
//
// That is why `removeEldestEntry` is `protected` and returns false here: it is a subclass
// hook, and its default answer is "never evict".
//
// The hash table itself is separate chaining over a power-of-two table, with the JDK's spread
// step (`h ^ (h >>> 16)`) before masking. The mask keeps only the *low* bits of the hash, so a
// hashCode whose entropy lives in its high bits — `Integer.hashCode` of a large multiple of
// the table length, say — would collide on every key; XOR-ing the high half down first is the
// cheap fix. Compare {@link Hashtable}, which pays a `%` instead and mixes all the bits that
// way.
//
// Iteration in insertion order is exposed through the package-private `firstEntry`/`afterEntry`
// pair below, the same seam {@link TreeMap} offers as `firstNode`/`successor`: our `Map` has no
// entrySet view, so {@link LinkedHashSet} walks the list through those two methods instead.
//
// Subset of the JDK's: the collection views (keySet/values/entrySet), putAll, the SequencedMap
// operations (putFirst/putLast/reversed) and the default-method family are not modelled.
// size/isEmpty/containsKey/put/remove are declared here rather than inherited, because the JDK's
// LinkedHashMap gets them from HashMap and ours cannot extend KajiLibrary's HashMap: overriding
// `put` would have to call `super.put`, and our javac's bytecode generator has no `super` call.
public class LinkedHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

    // The buckets: each holds a chain of entries linked by `next`.
    private LhmEntry<K, V>[] table;

    private int size;

    private int threshold;

    private float loadFactor;

    // false → the list is insertion order (the default); true → access order, i.e. LRU.
    private boolean accessOrder;

    // Ends of the order list. `head` is the eldest entry, `tail` the youngest. Package-private
    // so the iteration seam below can hand `head` out without copying the map.
    LhmEntry<K, V> head;

    LhmEntry<K, V> tail;

    public LinkedHashMap() {
        init(16, 0.75f, false);
    }

    public LinkedHashMap(int initialCapacity) {
        init(initialCapacity, 0.75f, false);
    }

    public LinkedHashMap(int initialCapacity, float loadFactor) {
        init(initialCapacity, loadFactor, false);
    }

    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        init(initialCapacity, loadFactor, accessOrder);
    }

    private void init(int initialCapacity, float loadFactor, boolean accessOrder) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity");
        }
        if (loadFactor <= 0.0f) {
            throw new IllegalArgumentException("Illegal load factor");
        }
        // Round the requested capacity up to a power of two: the index is computed with a
        // mask, which is only a valid modulo when the length is one.
        int cap = 1;
        while (cap < initialCapacity) {
            cap = cap * 2;
        }
        this.loadFactor = loadFactor;
        this.accessOrder = accessOrder;
        this.table = new LhmEntry[cap];
        this.threshold = (int) ((float) cap * loadFactor);
    }

    // --- hashing --------------------------------------------------------------------

    // Fold the high 16 bits into the low ones before masking, so a hash whose entropy sits
    // above the mask still spreads the keys across the table.
    private static int spread(Object key) {
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private int indexFor(int hash) {
        return hash & (table.length - 1);
    }

    // The entry holding `key`, or null. `e.key` goes through an `Object` local before
    // `equals`: a call on a receiver whose static type is a *type variable* is silently
    // dropped by our javac (finding #111).
    private LhmEntry<K, V> entryFor(Object key) {
        int hash = spread(key);
        LhmEntry<K, V> e = table[indexFor(hash)];
        LhmEntry<K, V> found = null;
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

    // --- the order list -------------------------------------------------------------
    //
    // Three operations, all O(1), all on the same entry objects the hash table already holds.
    // Nothing here touches the buckets, and nothing in the bucket code touches these links —
    // keeping the two structures independent is what makes the class simple.

    private void linkAtTail(LhmEntry<K, V> e) {
        e.before = tail;
        e.after = null;
        if (tail == null) {
            head = e;
        } else {
            tail.after = e;
        }
        tail = e;
    }

    private void unlinkFromOrder(LhmEntry<K, V> e) {
        LhmEntry<K, V> b = e.before;
        LhmEntry<K, V> a = e.after;
        if (b == null) {
            head = a;
        } else {
            b.after = a;
        }
        if (a == null) {
            tail = b;
        } else {
            a.before = b;
        }
        e.before = null;
        e.after = null;
    }

    // Move a touched entry to the young end. Only in access order — in insertion order a
    // lookup must not disturb anything, which is the whole contract.
    private void afterAccess(LhmEntry<K, V> e) {
        if (accessOrder && tail != e) {
            unlinkFromOrder(e);
            linkAtTail(e);
        }
    }

    // --- Map ------------------------------------------------------------------------

    // Recorre la **lista de orden** (`head` -> `after`), no la tabla: es la que define el orden
    // de iteracion de un LinkedHashMap (finding #205).
    //
    // Divergencia: el JDK devuelve una vista **ordenada por insercion**; este devuelve un HashSet,
    // que no conserva ese orden. Se recorre en orden, pero el Set resultante no lo promete.
    public Set<K> keySet() {
        HashSet<K> out = new HashSet<K>();
        LhmEntry<K, V> e = this.head;
        while (e != null) {
            out.add(e.key);
            e = e.after;
        }
        return out;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        Iterator<? extends K> it = m.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            this.put(k, m.get(k));
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(Object key) {
        return entryFor(key) != null;
    }

    public V get(Object key) {
        LhmEntry<K, V> e = entryFor(key);
        Object v = null;
        if (e != null) {
            v = e.value;
            afterAccess(e);
        }
        return (V) v;
    }

    public V put(K key, V value) {
        Object k = key;
        int hash = spread(k);
        LhmEntry<K, V> existing = entryFor(key);
        Object old = null;
        if (existing != null) {
            old = existing.value;
            existing.value = value;
            // A re-put is an access, not an insertion: the key keeps its original position
            // in insertion order, and moves to the young end only in access order.
            afterAccess(existing);
        } else {
            if (size >= threshold) {
                resize();
                hash = spread(k);
            }
            int index = indexFor(hash);
            LhmEntry<K, V> e = new LhmEntry<K, V>(hash, key, value, table[index]);
            table[index] = e;
            linkAtTail(e);
            size = size + 1;
            // The eviction hook runs after the insertion, so a cache that keeps N entries
            // briefly holds N+1 and then drops the eldest — which is why the usual test is
            // `size() > capacity` rather than `>=`.
            LhmEntry<K, V> eldest = head;
            if (eldest != null && removeEldestEntry(eldest)) {
                removeEntry(eldest);
            }
        }
        return (V) old;
    }

    public V remove(Object key) {
        LhmEntry<K, V> e = entryFor(key);
        Object old = null;
        if (e != null) {
            old = e.value;
            removeEntry(e);
        }
        return (V) old;
    }

    // Take an entry out of both structures: unlink it from its bucket chain and from the
    // order list. Missing either one leaves the map subtly broken — a ghost in the chain, or
    // an iteration that walks a removed entry.
    private void removeEntry(LhmEntry<K, V> e) {
        int index = indexFor(e.hash);
        LhmEntry<K, V> p = table[index];
        LhmEntry<K, V> prev = null;
        boolean done = false;
        while (p != null && !done) {
            if (p == e) {
                if (prev == null) {
                    table[index] = p.next;
                } else {
                    prev.next = p.next;
                }
                done = true;
            } else {
                prev = p;
                p = p.next;
            }
        }
        unlinkFromOrder(e);
        e.next = null;
        size = size - 1;
    }

    // Walking the *order list* rather than the table is not just convenience here: it is what
    // keeps the cost proportional to the number of entries instead of to the table length.
    public boolean containsValue(Object value) {
        boolean found = false;
        LhmEntry<K, V> e = head;
        while (e != null) {
            Object v = e.value;
            if (value == null) {
                if (v == null) {
                    found = true;
                }
            } else if (value.equals(v)) {
                found = true;
            }
            e = e.after;
        }
        return found;
    }

    public void clear() {
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        head = null;
        tail = null;
        size = 0;
    }

    // Double the table and re-file every entry. Note the loop runs over the *order list*, and
    // that it does not touch `before`/`after`: growing the table changes which bucket an entry
    // hangs in, never where it sits in insertion order. That separation is the reason a
    // LinkedHashMap's iteration order is stable across resizes while a HashMap's is not.
    private void resize() {
        int newLength = table.length * 2;
        table = new LhmEntry[newLength];
        threshold = (int) ((float) newLength * loadFactor);
        LhmEntry<K, V> e = head;
        while (e != null) {
            int index = indexFor(e.hash);
            e.next = table[index];
            table[index] = e;
            e = e.after;
        }
    }

    // The subclass hook. Called with the eldest entry after every insertion; returning true
    // evicts it. The default never does, which makes a plain LinkedHashMap unbounded.
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return false;
    }

    // --- iteration seam -------------------------------------------------------------
    //
    // Package-private, exactly like {@link TreeMap}'s firstNode/successor and for the same
    // reason: our `Map` has no entrySet view to borrow an iterator from, so a class that needs
    // to walk this one in order — {@link LinkedHashSet} — walks these two instead. Handing out
    // the entries themselves rather than a copy is what keeps the walk O(1) per step and
    // allocation-free.

    LhmEntry<K, V> firstEntry() {
        return head;
    }

    LhmEntry<K, V> lastEntry() {
        return tail;
    }

    LhmEntry<K, V> afterEntry(LhmEntry<K, V> e) {
        LhmEntry<K, V> n = null;
        if (e != null) {
            n = e.after;
        }
        return n;
    }

    /**
     * Los valores de este mapa.
     *
     * <p>**Divergencia deliberada**, la misma que ya declara `keySet()`: la del JDK es una *vista*
     * respaldada por el mapa; esta es una copia sacada en el momento. Y a diferencia de `keySet()`
     * es una `Collection` y no un `Set`, porque los valores **si** pueden repetirse.
     */
    public java.util.Collection<V> values() {
        java.util.ArrayList<V> out = new java.util.ArrayList<V>();
        java.util.Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            out.add(this.get(it.next()));
        }
        return out;
    }

    /**
     * Los pares de este mapa.
     *
     * <p>Misma divergencia que `values()`: copia, no vista. Los pares que devuelve son inmutables,
     * asi que `setValue` sobre uno de ellos lanza en vez de escribir en el mapa — que es lo
     * coherente con que sea una copia: escribir en un par que nadie mira seria peor que negarse.
     */
    public java.util.Set<java.util.Map.Entry<K, V>> entrySet() {
        java.util.HashSet<java.util.Map.Entry<K, V>> out =
            new java.util.HashSet<java.util.Map.Entry<K, V>>();
        java.util.Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            out.add(new FixedEntry<K, V>(k, this.get(k)));
        }
        return out;
    }
}

// One entry: the hash-table part (`hash`, `key`, `value`, `next`) and the order-list part
// (`before`, `after`) in a single object. Two structures, one allocation — that is the trick
// the whole class rests on.
//
// Top-level package-private rather than nested, since a nested class inside a *generic* class
// is miscompiled (finding #13). It implements {@link Map.Entry} so `removeEldestEntry` can be
// handed the eldest entry without exposing this type.
final class LhmEntry<K, V> implements Map.Entry<K, V> {

    int hash;
    K key;
    V value;
    // Next in the *bucket chain* — collision resolution, nothing to do with order.
    LhmEntry<K, V> next;
    // Neighbours in the *order list*.
    LhmEntry<K, V> before;
    LhmEntry<K, V> after;

    LhmEntry(int hash, K key, V value, LhmEntry<K, V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }
}
