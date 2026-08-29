package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.Map;

// A {@link Map} whose keys are the constants of a single enum type — and which therefore does
// not need to hash anything. Every enum constant already carries a small, dense, unique integer
// (`ordinal()`), so the map is just an array indexed by it: `get` is a bounds check and an array
// read, `put` is an array write, and there is no hash, no bucket, no collision, no Entry object,
// and no load factor. It is as close to free as a Map gets, and it is strictly better than a
// HashMap for this key type on every axis — speed, memory, and worst case.
//
// The two things that make it work are worth naming, because they are also its limits:
//
//   - **The key space is known and dense.** Ordinals are 0..n-1 with no gaps, which is exactly
//     the shape an array wants. That is a property of enums specifically, not of "small ints"
//     in general.
//   - **The map is fixed to one enum type.** That is what the `Class<K>` constructor argument is
//     for: an EnumMap is not a general map that happens to hold enums, and mixing two enum types
//     in one would make ordinals ambiguous.
//
// Where the JDK sizes the array once, from `keyType.getEnumConstants()`, this one grows it on
// demand: KajiLibrary's {@link Class} has no reflective access to an enum's constants (that
// needs static-field reflection the VM does not implement), so we learn the universe's size from
// the ordinals we are actually handed. The observable behaviour is the same; only the number of
// array allocations differs.
//
// Null *values* are allowed and are stored as a sentinel, because the array has no other way to
// tell "mapped to null" from "not mapped at all" — the same problem {@link Hashtable} solved by
// banning nulls outright and HashMap solved with a separate containsKey.
//
// Subset of the JDK's: the collection views (keySet/values/entrySet), putAll, the
// `EnumMap(Map)` constructor (our `Map` has no iteration to copy through) and clone are not
// modelled. Iteration in ordinal order would be the natural seam to add next.
public class EnumMap<K extends Enum, V> implements Map<K, V> {

    // Stands in for a null value in the array, so that `vals[i] == null` can keep its one clear
    // meaning: nothing is mapped here. A reference-typed `static final` is safe; a primitive one
    // would read back as 0 (finding #112).
    private static final Object NULL_VALUE = new Object();

    // The enum type this map is keyed by. Kept for the type check on the way in — the array
    // cannot tell a wrong enum's ordinal 2 from the right enum's.
    private final Class<K> keyType;

    // Values by ordinal. `vals[i] == null` means absent.
    private Object[] vals;

    // Las CLAVES por ordinal, en paralelo a `vals` (finding #205). Sin esto no hay `keySet()`
    // posible: de un ordinal no se vuelve a la constante sin el universo del enum, y obtenerlo
    // pide `Class.getEnumConstants()` — reflexion sobre `$VALUES`, que la VM no implementa.
    // Guardar la clave que ya nos pasaron cuesta un puntero por entrada y no necesita nada.
    private Object[] keys;

    private int size;

    public EnumMap(Class<K> keyType) {
        this.keyType = keyType;
        this.vals = new Object[8];
        this.keys = new Object[8];
    }

    public EnumMap(EnumMap<K, V> m) {
        this.keyType = m.keyType;
        this.vals = new Object[m.vals.length];
        this.keys = new Object[m.vals.length];
        for (int i = 0; i < m.vals.length; i++) {
            this.vals[i] = m.vals[i];
            this.keys[i] = m.keys[i];
        }
        this.size = m.size;
    }

    // The ordinal of `key`, or -1 if it is not a constant of this map's enum type.
    //
    // El bind a un local `Enum` antes de llamar `ordinal()` ya no es un rodeo: lo era por el
    // finding #111 (una llamada sobre un receptor de tipo *variable de tipo* se descartaba en
    // silencio), que quedo **arreglado el 2026-08-24**. Se conserva porque el parametro es `Object`
    // y el cast hace falta igual, pero ya no hay nada que esquivar.
    private int ordinalOf(Object key) {
        int index = -1;
        if (key != null && keyType.isInstance(key)) {
            Enum e = (Enum) key;
            index = e.ordinal();
        }
        return index;
    }

    private static Object maskNull(Object value) {
        Object v = value;
        if (v == null) {
            v = NULL_VALUE;
        }
        return v;
    }

    private static Object unmaskNull(Object value) {
        Object v = value;
        if (v == NULL_VALUE) {
            v = null;
        }
        return v;
    }

    // Grow so that `ordinal` is a legal index. Doubling, so a map filled in ordinal order pays
    // amortized O(1) per entry rather than reallocating on each new constant.
    private void ensureCapacity(int ordinal) {
        if (ordinal >= vals.length) {
            int newLength = vals.length * 2;
            while (newLength <= ordinal) {
                newLength = newLength * 2;
            }
            Object[] bigger = new Object[newLength];
            Object[] biggerKeys = new Object[newLength];
            for (int i = 0; i < vals.length; i++) {
                bigger[i] = vals[i];
                biggerKeys[i] = keys[i];
            }
            vals = bigger;
            keys = biggerKeys;
        }
    }

    // Las claves presentes, recorridas en orden de ordinal (finding #205). Divergencia: el JDK
    // devuelve una vista ordenada por ordinal; esta es un HashSet, que no conserva ese orden.
    public Set<K> keySet() {
        HashSet<K> out = new HashSet<K>();
        int i = 0;
        while (i < vals.length) {
            if (vals[i] != null) {
                out.add((K) keys[i]);
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
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(Object key) {
        int i = ordinalOf(key);
        return i >= 0 && i < vals.length && vals[i] != null;
    }

    public V get(Object key) {
        int i = ordinalOf(key);
        Object v = null;
        if (i >= 0 && i < vals.length) {
            v = unmaskNull(vals[i]);
        }
        return (V) v;
    }

    public V put(K key, V value) {
        int i = ordinalOf(key);
        if (i < 0) {
            // Either null or a constant of some other enum. Both are the caller confusing two
            // key spaces, and silently ignoring that would corrupt the map.
            throw new ClassCastException("key is not a constant of this map's enum type");
        }
        ensureCapacity(i);
        Object old = vals[i];
        if (old == null) {
            size = size + 1;
        }
        vals[i] = maskNull(value);
        keys[i] = key;
        return (V) unmaskNull(old);
    }

    public V remove(Object key) {
        int i = ordinalOf(key);
        Object old = null;
        if (i >= 0 && i < vals.length && vals[i] != null) {
            old = vals[i];
            vals[i] = null;
            keys[i] = null;
            size = size - 1;
        }
        return (V) unmaskNull(old);
    }

    // Linear in the *universe*, not in the number of entries — the one operation where the array
    // layout is not a win, and the same cost a HashMap pays for the same question.
    public boolean containsValue(Object value) {
        Object target = maskNull(value);
        boolean found = false;
        for (int i = 0; i < vals.length; i++) {
            Object v = vals[i];
            if (v != null) {
                if (v == target || target.equals(v)) {
                    found = true;
                }
            }
        }
        return found;
    }

    public void clear() {
        for (int i = 0; i < vals.length; i++) {
            vals[i] = null;
        }
        size = 0;
    }

    // Equality against another EnumMap only. The JDK's compares entry sets, so an EnumMap can
    // equal any Map with the same entries; our `Map` has no entrySet to do that with.
    public boolean equals(Object o) {
        boolean same;
        if (o == this) {
            same = true;
        } else if (!(o instanceof EnumMap)) {
            same = false;
        } else {
            EnumMap<K, V> other = (EnumMap<K, V>) o;
            if (other.size != size) {
                same = false;
            } else {
                same = true;
                int limit = vals.length;
                if (other.vals.length > limit) {
                    limit = other.vals.length;
                }
                for (int i = 0; i < limit; i++) {
                    Object mine = null;
                    Object theirs = null;
                    if (i < vals.length) {
                        mine = vals[i];
                    }
                    if (i < other.vals.length) {
                        theirs = other.vals[i];
                    }
                    if (mine == null) {
                        if (theirs != null) {
                            same = false;
                        }
                    } else if (theirs == null || !mine.equals(theirs)) {
                        same = false;
                    }
                }
            }
        }
        return same;
    }

    // Map's hash is the *sum* of the entry hashes, which makes it order-independent — the right
    // choice for a structure whose iteration order is not part of its identity. An entry's hash
    // is key.hashCode() ^ value.hashCode(); here the key's hash stands in as its ordinal, since
    // that is all this map knows about it.
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < vals.length; i++) {
            Object v = vals[i];
            if (v != null) {
                int vh = 0;
                if (v != NULL_VALUE) {
                    vh = v.hashCode();
                }
                h = h + (i ^ vh);
            }
        }
        return h;
    }
}
