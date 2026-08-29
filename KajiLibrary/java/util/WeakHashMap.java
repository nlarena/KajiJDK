package java.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
// Same-package import works around the frozen javac's finder (finding #4).
import java.util.Map;

// A map whose **keys do not keep themselves alive**. Each entry holds its key through a
// {@link WeakReference}, so once nothing else in the program refers to that key the collector
// is free to reclaim it — and the entry quietly disappears from the map.
//
// The problem it solves is the classic leak of associating data with objects you do not own.
// Attach per-object metadata in an ordinary HashMap and the map becomes the last thing keeping
// every one of those objects alive: the cache *is* the leak. Every entry is a strong reference,
// and a map nobody ever removes from grows forever. WeakHashMap inverts that: the map observes
// the keys rather than owning them.
//
// Three consequences, and they are the reason it is a specialist tool rather than a default:
//
//   - **Entries vanish on their own.** `size()` can shrink between two calls with no
//     modification in between, and an iteration can see an entry disappear underneath it.
//     Code that assumes a map only changes when you change it is wrong here.
//   - **The value must not refer to the key.** A value holding a strong reference to its own
//     key keeps the key reachable through the map's own table, so the entry is immortal — the
//     leak comes back, now harder to see. (The JDK solves this with a `WeakReference` value or
//     an indirection; we simply document it.)
//   - **The key must be identity-ish.** Weak references are about *this object* dying, so a key
//     type whose instances are freely recreated (`String`, boxed `Integer`) gets you entries
//     whose lifetime is unrelated to anything meaningful.
//
// The mechanism has two halves and both matter. The obvious half is the WeakReference: the
// collector clears it when the referent dies, so `get()` starts returning null. That alone is
// not enough — the *entry* is still in the table, holding its value strongly, and nothing has
// told the map. The second half is the {@link ReferenceQueue}: each entry registers itself on
// one at construction, the collector pushes cleared references onto it, and `expungeStaleEntries`
// drains that queue at the start of **every** operation. That drain is what turns "the key is
// gone" into "the entry is gone", and it is why the class works at all.
//
// The trick that makes the drain possible is worth pointing out: each entry caches its key's
// hash. By the time an entry reaches the queue its key is already null, so the map could never
// recompute which bucket it belonged to — the cached hash is the only way home.
//
// Subset of the JDK's: putAll, the collection views (keySet/values/entrySet) and
// newWeakHashMap are not modelled.
public class WeakHashMap<K, V> implements Map<K, V> {

    // A null key has no object to die with, so it is stored as this strongly-held sentinel and
    // translated back on the way out — a null-keyed entry simply never expires. A
    // reference-typed `static final` is safe; a primitive one would read back as 0 (#112).
    private static final Object NULL_KEY = new Object();

    private WhmEntry<V>[] table;

    private int size;

    private int threshold;

    private float loadFactor;

    // Where the collector deposits entries whose key has died. Final, and shared by every
    // entry of this map: it is the map's inbox.
    private final ReferenceQueue queue;

    public WeakHashMap() {
        queue = new ReferenceQueue();
        init(16, 0.75f);
    }

    public WeakHashMap(int initialCapacity) {
        queue = new ReferenceQueue();
        init(initialCapacity, 0.75f);
    }

    public WeakHashMap(int initialCapacity, float loadFactor) {
        queue = new ReferenceQueue();
        init(initialCapacity, loadFactor);
    }

    private void init(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Initial Capacity");
        }
        if (loadFactor <= 0.0f) {
            throw new IllegalArgumentException("Illegal Load factor");
        }
        int cap = 1;
        while (cap < initialCapacity) {
            cap = cap * 2;
        }
        this.loadFactor = loadFactor;
        this.table = new WhmEntry[cap];
        this.threshold = (int) ((float) cap * loadFactor);
    }

    private static Object maskNull(Object key) {
        Object k = key;
        if (k == null) {
            k = NULL_KEY;
        }
        return k;
    }

    private static Object unmaskNull(Object key) {
        Object k = key;
        if (k == NULL_KEY) {
            k = null;
        }
        return k;
    }

    private static int spread(Object key) {
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private int indexFor(int hash) {
        return hash & (table.length - 1);
    }

    // Equality that tolerates the null a cleared reference gives back. A boolean-valued
    // ternary is rejected by our javac (finding #109), so this is spelled out.
    private static boolean eq(Object a, Object b) {
        boolean same;
        if (a == null) {
            same = b == null;
        } else {
            same = a == b || a.equals(b);
        }
        return same;
    }

    // --- the drain ------------------------------------------------------------------

    // Unlink every entry the collector has handed back. Called at the top of every operation:
    // that is the "on each operation" part of the contract, and the only thing standing
    // between a dead key and a permanently retained value.
    private void expungeStaleEntries() {
        Reference r = queue.poll();
        while (r != null) {
            WhmEntry<V> stale = (WhmEntry<V>) r;
            // The key is already null here, which is exactly why the entry cached its hash.
            int index = indexFor(stale.hash);
            WhmEntry<V> p = table[index];
            WhmEntry<V> prev = null;
            boolean done = false;
            while (p != null && !done) {
                if (p == stale) {
                    if (prev == null) {
                        table[index] = p.next;
                    } else {
                        prev.next = p.next;
                    }
                    // Drop the value too. Leaving it set would keep the *value* alive off a
                    // detached entry — a smaller version of the leak this class exists to
                    // prevent.
                    p.value = null;
                    p.next = null;
                    size = size - 1;
                    done = true;
                } else {
                    prev = p;
                    p = p.next;
                }
            }
            r = queue.poll();
        }
    }

    // The entry holding `key`, or null. Entries whose key has died but whose queue notification
    // has not been processed yet simply fail to match: `e.key()` is null and no live key
    // equals null.
    private WhmEntry<V> entryFor(Object key) {
        int hash = spread(key);
        WhmEntry<V> e = table[indexFor(hash)];
        WhmEntry<V> found = null;
        while (e != null && found == null) {
            if (e.hash == hash) {
                Object k = e.key();
                if (eq(k, key)) {
                    found = e;
                }
            }
            e = e.next;
        }
        return found;
    }

    // --- Map ------------------------------------------------------------------------

    // Note this is not a pure accessor: it drains first, so the answer reflects the keys that
    // have died since the last call. A `size()` that could only shrink at a modification would
    // be a lie.
    // Buckets + cadenas de colision, saltando las entradas cuya clave ya murio: `WhmEntry` es una
    // `WeakReference`, y un `get()` nulo significa que el GC se llevo la clave (finding #205).
    public Set<K> keySet() {
        HashSet<K> out = new HashSet<K>();
        int i = 0;
        while (i < this.table.length) {
            WhmEntry<V> e = this.table[i];
            while (e != null) {
                Object k = e.get();
                if (k != null) {
                    out.add((K) k);
                }
                e = e.next;
            }
            i = i + 1;
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
        expungeStaleEntries();
        return size;
    }

    public boolean isEmpty() {
        expungeStaleEntries();
        return size == 0;
    }

    public boolean containsKey(Object key) {
        expungeStaleEntries();
        Object k = maskNull(key);
        return entryFor(k) != null;
    }

    public V get(Object key) {
        expungeStaleEntries();
        Object k = maskNull(key);
        WhmEntry<V> e = entryFor(k);
        Object v = null;
        if (e != null) {
            v = e.value;
        }
        return (V) v;
    }

    public V put(K key, V value) {
        expungeStaleEntries();
        Object k = maskNull(key);
        int hash = spread(k);
        WhmEntry<V> e = table[indexFor(hash)];
        Object old = null;
        boolean replaced = false;
        while (e != null && !replaced) {
            if (e.hash == hash) {
                Object ek = e.key();
                if (eq(ek, k)) {
                    old = e.value;
                    e.value = value;
                    replaced = true;
                }
            }
            e = e.next;
        }
        if (!replaced) {
            if (size >= threshold) {
                resize();
            }
            int index = indexFor(hash);
            // Registering on the queue happens here, in the entry's constructor: an entry that
            // forgot to pass the queue would still be cleared by the collector but would never
            // be reported, and would sit in the table forever holding its value.
            table[index] = new WhmEntry<V>(k, value, queue, hash, table[index]);
            size = size + 1;
        }
        return (V) old;
    }

    public V remove(Object key) {
        expungeStaleEntries();
        Object k = maskNull(key);
        int hash = spread(k);
        int index = indexFor(hash);
        WhmEntry<V> e = table[index];
        WhmEntry<V> prev = null;
        Object old = null;
        boolean done = false;
        while (e != null && !done) {
            boolean hit = false;
            if (e.hash == hash) {
                Object ek = e.key();
                hit = eq(ek, k);
            }
            if (hit) {
                if (prev == null) {
                    table[index] = e.next;
                } else {
                    prev.next = e.next;
                }
                old = e.value;
                e.value = null;
                size = size - 1;
                done = true;
            } else {
                prev = e;
                e = e.next;
            }
        }
        return (V) old;
    }

    public boolean containsValue(Object value) {
        expungeStaleEntries();
        boolean found = false;
        for (int i = 0; i < table.length; i++) {
            WhmEntry<V> e = table[i];
            while (e != null) {
                Object v = e.value;
                if (eq(value, v)) {
                    found = true;
                }
                e = e.next;
            }
        }
        return found;
    }

    public void clear() {
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        size = 0;
        // Drain *after* dropping the table: entries the collector cleared while we were
        // emptying it would otherwise be re-examined against buckets that no longer hold them.
        expungeStaleEntries();
    }

    // Double the table and re-file every entry that still has a live key. The resize doubles as
    // a sweep: an entry whose key died but whose queue notice has not arrived yet is dropped
    // here rather than carried into the new table.
    private void resize() {
        WhmEntry<V>[] old = table;
        int newLength = old.length * 2;
        table = new WhmEntry[newLength];
        threshold = (int) ((float) newLength * loadFactor);
        size = 0;
        for (int i = 0; i < old.length; i++) {
            WhmEntry<V> e = old[i];
            while (e != null) {
                WhmEntry<V> next = e.next;
                Object k = e.key();
                if (k == null) {
                    e.value = null;
                    e.next = null;
                } else {
                    int index = indexFor(e.hash);
                    e.next = table[index];
                    table[index] = e;
                    size = size + 1;
                }
                e = next;
            }
        }
    }
}

// One entry — and it **is** the weak reference to its key rather than holding one, which is the
// whole design. Extending {@link WeakReference} means the object the collector clears and
// enqueues is the very object sitting in the bucket chain, so a queued reference can be cast
// straight back to an entry and unlinked. An entry that merely *contained* a WeakReference
// would leave the map holding a queued reference with no way back to the entry around it.
//
// The key is not a field here: it lives in Reference's referent slot, the one the collector
// treats specially. `get()` reads it, and returns null once it has died.
//
// Top-level package-private rather than nested, since a nested class inside a *generic* class
// is miscompiled (finding #13). Generic in V only — the key is untyped because Reference is.
final class WhmEntry<V> extends WeakReference {

    // The key's hash, cached at construction. Once the key dies this is the only surviving
    // clue to which bucket the entry belongs in, so the expunge could not work without it.
    int hash;

    V value;

    WhmEntry<V> next;

    WhmEntry(Object key, V value, ReferenceQueue queue, int hash, WhmEntry<V> next) {
        // Registers with the map's queue: this is the subscription that makes the key's death
        // observable.
        super(key, queue);
        this.hash = hash;
        this.value = value;
        this.next = next;
    }

    // The key, or null once the collector has cleared the reference. This wrapper exists for a
    // compiler reason, not a design one: our javac silently *deletes* a call to a method that
    // the receiver's static type only **inherits** from an external (classpath) superclass, and
    // `get()` is declared on Reference, not on WeakReference — so `entry.get()` compiles to
    // nothing at all. Routing the call through a receiver typed as the declaring class emits it
    // properly. Reported this session; see the run notes.
    Object key() {
        Reference r = (Reference) this;
        return r.get();
    }
}
