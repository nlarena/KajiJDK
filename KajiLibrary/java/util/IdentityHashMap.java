package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.Map;

// A map that compares keys with `==` and never with `equals` — and hashes them with
// {@link System#identityHashCode} rather than with `hashCode()`. It deliberately violates the
// {@link Map} contract, which is defined in terms of `equals`, and the JDK's own javadoc says
// so in the first paragraph. It is not a faster HashMap and it is not a general-purpose map;
// reaching for it because "my keys are unique anyway" is a bug waiting for the first key that
// gets copied.
//
// The reason it exists is *bookkeeping about objects*, where "the same object" is the question
// and "an equal object" is the wrong answer:
//
//   - **Graph traversal.** A serializer walking an object graph has to know whether it has
//     already written *this* node. Two structurally equal nodes are still two nodes, and with
//     an equals-based map the second one would be silently dropped — turning a tree into a DAG.
//   - **Cycle detection**, for the same reason: `a.equals(b)` on a cyclic graph may not even
//     terminate, while `a == b` always does.
//   - **Per-object state** kept outside the object: proxies, interning tables, "have I already
//     visited this?" marks.
//
// A second, quieter reason: a key whose `equals`/`hashCode` are expensive, or *mutable*, or
// user-supplied and untrusted, breaks a normal hash map. Identity comparison touches no user
// code at all, so it cannot be slow, wrong, or hostile.
//
// The layout is the JDK's and is unusual: **one** `Object[]` where a key sits at an even index
// and its value in the slot right after it, with linear probing. No Entry objects at all — an
// entry is a pair of adjacent array slots. That halves the allocation of a chained table and
// keeps a key and its value on the same cache line, which is exactly what a hot identity lookup
// wants.
//
// Subset of the JDK's: putAll, the collection views (keySet/values/entrySet) and clone are not
// modelled.
public class IdentityHashMap<K, V> implements Map<K, V> {

    // `null` is a perfectly good identity, but a null slot is also how the table says "empty",
    // so a null key is stored as this sentinel and translated back on the way out. A
    // reference-typed `static final` is safe here; a primitive one would read back as 0
    // (finding #112).
    private static final Object NULL_KEY = new Object();

    // Key at even index i, value at i + 1. Length is always a power of two, and always even.
    private Object[] table;

    private int size;

    public IdentityHashMap() {
        table = new Object[64];
    }

    // `expectedMaxSize` is a hint: the table is sized so it will not have to grow before
    // holding that many entries at the 2/3 load factor below.
    public IdentityHashMap(int expectedMaxSize) {
        if (expectedMaxSize < 0) {
            throw new IllegalArgumentException("expectedMaxSize is negative");
        }
        int cap = 4;
        while (cap * 2 < expectedMaxSize * 3) {
            cap = cap * 2;
        }
        table = new Object[cap * 2];
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

    // The JDK's index function, and worth reading twice. `identityHashCode` is derived from
    // the object's address or from a per-object random word, so its *low* bits are often the
    // least interesting — objects allocated together differ by a fixed stride. `(h << 1) -
    // (h << 8)` is `h * -254`, a cheap multiply-by-odd-ish that folds the higher bits down;
    // the mask then keeps the result inside the table. It comes out **even** for free, which
    // is what makes it a valid key index in a key/value-interleaved array.
    private int indexFor(Object key) {
        int h = System.identityHashCode(key);
        return ((h << 1) - (h << 8)) & (table.length - 1);
    }

    // Step to the next key slot, wrapping. Two at a time, because entries are pairs.
    private int nextIndex(int i) {
        int n = i + 2;
        if (n >= table.length) {
            n = 0;
        }
        return n;
    }

    // The index of `key`'s slot: the slot holding *that exact object*, or the first empty slot
    // on its probe sequence if it is absent. `==` is the entire class; no `equals` is called
    // here or anywhere else in this file.
    private int slotFor(Object key) {
        int i = indexFor(key);
        while (table[i] != null && table[i] != key) {
            i = nextIndex(i);
        }
        return i;
    }

    // La tabla alterna clave y valor, asi que se avanza de a dos (finding #205).
    //
    // Divergencia que importa aca mas que en los otros: este mapa compara por **identidad**, pero
    // el `HashSet` que se devuelve compara por `equals`. Dos claves distintas-por-identidad pero
    // iguales-por-equals colapsan en una sola entrada del set.
    public Set<K> keySet() {
        HashSet<K> out = new HashSet<K>();
        int i = 0;
        while (i < this.table.length) {
            if (this.table[i] != null) {
                out.add((K) unmaskNull(this.table[i]));
            }
            i = i + 2;
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
        Object k = maskNull(key);
        return table[slotFor(k)] != null;
    }

    public V get(Object key) {
        Object k = maskNull(key);
        return (V) table[slotFor(k) + 1];
    }

    // Values are compared by identity too, not just keys. That surprises people, but it is the
    // consistent choice: a map that ignores `equals` for lookups has no business trusting it
    // for a scan.
    public boolean containsValue(Object value) {
        boolean found = false;
        for (int i = 0; i < table.length; i = i + 2) {
            if (table[i] != null && table[i + 1] == value) {
                found = true;
            }
        }
        return found;
    }

    public V put(K key, V value) {
        // Grow *before* probing, so the index we are about to compute belongs to the final
        // table. The 2/3 load factor is lower than a chained table's 0.75 because linear
        // probing degrades faster: past that point clusters start merging.
        if (size * 3 >= table.length) {
            resize();
        }
        Object k = maskNull(key);
        int i = slotFor(k);
        Object old = table[i + 1];
        if (table[i] == null) {
            size = size + 1;
        }
        table[i] = k;
        table[i + 1] = value;
        return (V) old;
    }

    public V remove(Object key) {
        Object k = maskNull(key);
        int i = slotFor(k);
        Object old = null;
        if (table[i] != null) {
            old = table[i + 1];
            table[i] = null;
            table[i + 1] = null;
            size = size - 1;
            // Emptying a slot can cut a probe sequence in half, orphaning every entry after
            // it in the same cluster. Re-inserting the rest of the cluster restores the
            // invariant that a lookup never stops early.
            int j = nextIndex(i);
            while (table[j] != null) {
                Object rk = table[j];
                Object rv = table[j + 1];
                table[j] = null;
                table[j + 1] = null;
                size = size - 1;
                this.put((K) unmaskNull(rk), (V) rv);
                j = nextIndex(j);
            }
        }
        return (V) old;
    }

    public void clear() {
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        size = 0;
    }

    // Double the table and re-insert every live pair into the fresh, larger array.
    private void resize() {
        Object[] old = table;
        table = new Object[old.length * 2];
        size = 0;
        for (int i = 0; i < old.length; i = i + 2) {
            if (old[i] != null) {
                this.put((K) unmaskNull(old[i]), (V) old[i + 1]);
            }
        }
    }

    // Equality is identity-based on both sides, and only against another IdentityHashMap. The
    // JDK's falls back to the ordinary entry-set comparison for any other Map; ours has no
    // entrySet to do that with, and comparing an identity map to an equals-based one by
    // `equals` would be answering a question neither map asked.
    public boolean equals(Object o) {
        boolean same;
        if (o == this) {
            same = true;
        } else if (!(o instanceof IdentityHashMap)) {
            same = false;
        } else {
            IdentityHashMap<K, V> other = (IdentityHashMap<K, V>) o;
            if (other.size() != size) {
                same = false;
            } else {
                same = true;
                for (int i = 0; i < table.length; i = i + 2) {
                    if (table[i] != null) {
                        Object k = unmaskNull(table[i]);
                        // `get` on the other map probes by identity, so this asks exactly
                        // "does it hold *this object* mapped to *that object*".
                        if (other.get(k) != table[i + 1] || !other.containsKey(k)) {
                            same = false;
                        }
                    }
                }
            }
        }
        return same;
    }

    // XOR per entry so the result is order-independent (the probe order is an implementation
    // detail), summed so that two entries with swapped key and value do not cancel out.
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < table.length; i = i + 2) {
            if (table[i] != null) {
                Object k = unmaskNull(table[i]);
                h = h + (System.identityHashCode(k) ^ System.identityHashCode(table[i + 1]));
            }
        }
        return h;
    }
}
