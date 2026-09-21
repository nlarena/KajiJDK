package java.util;


// The skeleton for maps. In the JDK a subclass provides `entrySet()` and everything else —
// size, get, containsKey, the iteration — is derived from walking it.
//
// KajiLibrary's `Map` is the subset without the collection views, so there is no entry set to
// walk: `entrySet()` is declared (a subclass may still provide one) but the derivations that
// would need it are left to the subclass. What this class does give, and what makes it worth
// having, is the pair every map otherwise has to repeat: `isEmpty()` in terms of `size()`, and
// the mutators refusing by default so a read-only map inherits the right behaviour.
public abstract class AbstractMap<K, V> implements Map<K, V> {

    protected AbstractMap() {
    }

    public abstract Set<Map.Entry<K, V>> entrySet();

    // The keys, derived from `entrySet()` — which is precisely the primitive the whole of
    // `AbstractMap` hangs off (finding #205). Subclasses that can do it more cheaply override it.
    public Set<K> keySet() {
        HashSet<K> out = new HashSet<K>();
        Iterator<Map.Entry<K, V>> it = this.entrySet().iterator();
        while (it.hasNext()) {
            out.add(it.next().getKey());
        }
        return out;
    }

    // The three basic queries, derived from `entrySet()` like everything else (§ and the JDK declares
    // them concrete right here). They were missing: `AbstractMap` let them fall through to `Map`'s
    // abstract ones, and that forced each concrete subclass to write them or be left incomplete with
    // nobody saying so -- finding #284 uncovered them, and it also explains why `values()` further
    // down compiled calling `this.get(...)`.
    //
    // Subclasses that can answer them more cheaply override them; almost all do.
    public boolean containsKey(Object key) {
        Iterator<Map.Entry<K, V>> it = this.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> e = it.next();
            if (key == null ? e.getKey() == null : key.equals(e.getKey())) {
                return true;
            }
        }
        return false;
    }

    public boolean containsValue(Object value) {
        Iterator<Map.Entry<K, V>> it = this.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> e = it.next();
            if (value == null ? e.getValue() == null : value.equals(e.getValue())) {
                return true;
            }
        }
        return false;
    }

    public V get(Object key) {
        Iterator<Map.Entry<K, V>> it = this.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> e = it.next();
            if (key == null ? e.getKey() == null : key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        Iterator<? extends K> it = m.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            this.put(k, m.get(k));
        }
    }

    public abstract int size();

    public boolean isEmpty() {
        return size() == 0;
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
    // The values, as a Collection.
    //
    // **A deliberate divergence**, the same one `keySet()` already declares: the JDK's is a *view*
    // backed by the map; this one is a copy. And unlike `keySet()`, values **can** repeat, which is
    // why it is a Collection and not a Set.
    public Collection<V> values() {
        ArrayList<V> out = new ArrayList<V>();
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            out.add(this.get(it.next()));
        }
        return out;
    }

    /**
     * Equality by content: the same keys, and each with the same value.
     *
     * <p>The same absence as AbstractList's, and for the same invisible reason: inheriting Object's
     * `equals`, a HashMap and a LinkedHashMap with the same content gave false.
     *
     * <p>It walks by `keySet()` and `get()` instead of comparing the two entry sets as the JDK does,
     * because that way equality does not depend on each implementation's entries having their own
     * `equals` properly written: it is enough for the map to know how to look up by key.
     *
     * <p>The null value case asks for the extra `containsKey` step: "the key is not there" and "it is,
     * and it is null" look the same from `get`, and they are not the same thing.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        Map<?, ?> other = (Map<?, ?>) o;
        if (other.size() != this.size()) {
            return false;
        }
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            V v = this.get(k);
            Object w = other.get(k);
            if (v == null) {
                if (w != null || !other.containsKey(k)) {
                    return false;
                }
            } else {
                if (!v.equals(w)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The hash Map's contract demands: the SUM of the entries' hashes, and an entry's is
     * `hash(key) ^ hash(value)`.
     *
     * <p>That it be a sum and not a positional combination is on purpose: a map has no order, so the
     * sum has to come out the same however it is walked. It is the only way for an equal HashMap and
     * TreeMap to have the same hash.
     */
    public int hashCode() {
        int h = 0;
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            V v = this.get(k);
            h = h + ((k == null ? 0 : k.hashCode()) ^ (v == null ? 0 : v.hashCode()));
        }
        return h;
    }

    /**
     * The map as {@code {key=value, key=value}}, in the order its iterator walks it.
     *
     * <p>Without this, any map that does not define it itself --HashMap included-- falls to Object's
     * `toString` and prints as `java.util.HashMap@3`. It is the kind of hole that breaks nothing
     * until somebody logs a map and reads an address instead of its data.
     *
     * <p>A self-reference prints as "(this Map)" and is not recursed into, which is what the JDK
     * does: a map that contains itself would overflow the stack on the first line of the log.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Iterator<K> it = this.keySet().iterator();
        boolean first = true;
        while (it.hasNext()) {
            K k = it.next();
            V v = this.get(k);
            if (!first) {
                sb.append(',').append(' ');
            }
            first = false;
            // The intermediate `Object`s are not decoration: `String.valueOf(k)` with `k` of a type
            // variable picks the `char[]` overload in this compiler (COMPILER_FINDINGS #341), and an
            // empty string comes out. With the type written by hand, it picks `Object`'s.
            Object ko = k;
            Object vo = v;
            sb.append(ko == this ? "(this Map)" : String.valueOf(ko));
            sb.append('=');
            sb.append(vo == this ? "(this Map)" : String.valueOf(vo));
        }
        sb.append('}');
        return sb.toString();
    }
}
