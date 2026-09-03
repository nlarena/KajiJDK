package java.util.concurrent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;

// A hash map safe for concurrent use. The JDK stripes its table into independently locked
// bins so unrelated keys never contend; KajiJDK guards one plain {@link HashMap} with the
// intrinsic monitor of a private `sync` object. The *observable* contract is the same —
// every operation is atomic, and the compare-and-act methods below are indivisible — and
// on a runtime whose threads interleave between opcodes the coarse lock costs nothing real.
//
// Single-exit style throughout (finding #105).
public class ConcurrentHashMap<K, V> implements ConcurrentMap<K, V>, Serializable {

    private final Object sync = new Object();
    private final HashMap<K, V> map = new HashMap<K, V>();

    public ConcurrentHashMap() {
    }

    // The JDK sizes its table from this hint; our HashMap grows on demand, so it is only
    // an API courtesy.
    public ConcurrentHashMap(int initialCapacity) {
    }

    // Same courtesy, plus a load factor this map has no table to apply it to.
    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
    }

    /**
     * The fully-tuned form.
     *
     * @param concurrencyLevel the number of threads expected to write at once. In the JDK it
     *        sized the lock striping; there is exactly one monitor here, so nothing to size. The
     *        parameter is accepted, validated and ignored -- and the ignoring costs throughput,
     *        never correctness, which is the trade this whole class already makes.
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (initialCapacity < 0 || loadFactor <= 0.0f || concurrencyLevel <= 0) {
            throw new IllegalArgumentException("bad map parameters");
        }
    }

    // A map holding the mappings of `m`.
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.putAll(m);
    }

    public int size() {
        int n;
        synchronized (sync) {
            n = map.size();
        }
        return n;
    }

    public boolean isEmpty() {
        boolean empty;
        synchronized (sync) {
            empty = map.isEmpty();
        }
        return empty;
    }

    public boolean containsKey(Object key) {
        boolean has;
        synchronized (sync) {
            has = map.containsKey(key);
        }
        return has;
    }

    public boolean containsValue(Object value) {
        boolean has;
        synchronized (sync) {
            has = map.containsValue(value);
        }
        return has;
    }

    public V get(Object key) {
        V v;
        synchronized (sync) {
            v = map.get(key);
        }
        return v;
    }

    /**
     * The keys of this map, as a {@link KeySetView}.
     *
     * <p>Narrower than {@code Map.keySet()}'s {@code Set<K>} on purpose, exactly as in the JDK: a
     * KeySetView can be used as a concurrent Set in its own right, and returning the wider type
     * would force a cast at every use.
     *
     * <p>**Deliberate divergence**: the JDK's view is live and this one is a snapshot -- see the
     * note on {@link #values()}. What the view does keep from the JDK is the write-through half:
     * {@code remove} and {@code clear} on it act on the map.
     */
    public KeySetView<K, V> keySet() {
        return new KeySetView<K, V>(this, null);
    }

    /**
     * The keys, as a set whose {@code add} really works: an added key is mapped to
     * {@code mappedValue}.
     *
     * <p>{@link #keySet()} has no value to store, so its {@code add} can only throw. Naming one
     * here is what turns this map into a concurrent *Set* -- which is the whole of
     * {@link #newKeySet}.
     *
     * @throws NullPointerException if {@code mappedValue} is null, since a null value is how this
     *         map says "absent"
     */
    public KeySetView<K, V> keySet(V mappedValue) {
        if (mappedValue == null) {
            throw new NullPointerException();
        }
        return new KeySetView<K, V>(this, mappedValue);
    }

    /**
     * A new, empty concurrent Set backed by a ConcurrentHashMap.
     *
     * <p>This is the reason KeySetView takes a mapped value at all: there is no
     * ConcurrentHashSet class, and there does not need to be -- a set is a map whose values are
     * all the same, and Boolean.TRUE is that value.
     */
    public static <K> KeySetView<K, Boolean> newKeySet() {
        ConcurrentHashMap<K, Boolean> backing = new ConcurrentHashMap<K, Boolean>();
        return backing.keySet(Boolean.TRUE);
    }

    public static <K> KeySetView<K, Boolean> newKeySet(int initialCapacity) {
        ConcurrentHashMap<K, Boolean> backing =
                new ConcurrentHashMap<K, Boolean>(initialCapacity);
        return backing.keySet(Boolean.TRUE);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        synchronized (sync) {
            map.putAll(m);
        }
    }

    public V put(K key, V value) {
        V prev;
        synchronized (sync) {
            prev = map.put(key, value);
        }
        return prev;
    }

    public V remove(Object key) {
        V prev;
        synchronized (sync) {
            prev = map.remove(key);
        }
        return prev;
    }

    public void clear() {
        synchronized (sync) {
            map.clear();
        }
    }

    public V putIfAbsent(K key, V value) {
        V existing;
        synchronized (sync) {
            existing = map.get(key);
            if (existing == null) {
                map.put(key, value);
            }
        }
        return existing;
    }

    public boolean remove(Object key, Object value) {
        boolean removed;
        synchronized (sync) {
            // The receiver is bound to an `Object` local before the call: invoking a
            // method on a receiver whose static type is a *type variable* is silently
            // dropped by our javac (finding #111) — it emits the argument in place of the
            // call, so this would have branched on `value` instead of comparing.
            Object current = map.get(key);
            if (current != null && current.equals(value)) {
                map.remove(key);
                removed = true;
            } else {
                removed = false;
            }
        }
        return removed;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        boolean replaced;
        synchronized (sync) {
            Object current = map.get(key);   // Object-typed receiver — see #111 above.
            if (current != null && current.equals(oldValue)) {
                map.put(key, newValue);
                replaced = true;
            } else {
                replaced = false;
            }
        }
        return replaced;
    }

    public V replace(K key, V value) {
        V prev;
        synchronized (sync) {
            prev = map.get(key);
            if (prev != null) {
                map.put(key, value);
            }
        }
        return prev;
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
            java.util.Map.Entry<K, V> e = Map.entry(k, this.get(k));   // #285: el
            out.add(e);                                               // local nombra el tipo
        }
        return out;
    }

    // ---------------------------------------------------------------- the Hashtable inheritance

    /**
     * Whether some key maps to {@code value} -- a synonym for {@link #containsValue}.
     *
     * <p>Only here because Hashtable had it and ConcurrentHashMap was written to be a drop-in
     * replacement for one. It is a trap in that name: {@code contains} on a Map reads as "contains
     * this key", and it means the opposite. Use containsValue.
     */
    public boolean contains(Object value) {
        return this.containsValue(value);
    }

    // The keys as an Enumeration, for the same reason: Hashtable compatibility.
    public Enumeration<K> keys() {
        return new ChmEnumeration<K>(this.keyList());
    }

    public Enumeration<V> elements() {
        ArrayList<K> keys = this.keyList();
        ArrayList<V> values = new ArrayList<V>();
        for (int i = 0; i < keys.size(); i++) {
            V v = this.get(keys.get(i));
            if (v != null) {
                values.add(v);
            }
        }
        return new ChmEnumeration<V>(values);
    }

    /**
     * The number of mappings, as a {@code long}.
     *
     * <p>{@link #size} is an {@code int} and a map may legitimately hold more entries than an int
     * can count -- that is the whole reason this method exists, and why the JDK says to prefer it.
     */
    public long mappingCount() {
        return (long) this.size();
    }

    // ---------------------------------------------------------------- bulk operations
    //
    // Every one of these takes a `parallelismThreshold` and every one of them ignores it: the
    // traversal is sequential. That is not a shortcut, it is forced by the design. Splitting a
    // traversal across threads needs a table that can be split -- the JDK's striped bins -- and
    // this map is one HashMap behind one monitor, so there is nothing to divide. The JDK itself
    // runs sequentially whenever the estimated size is below the threshold, so a caller who reads
    // the contract gets an execution the contract allows; what changes is speed, never the result.
    //
    // The traversal is over a SNAPSHOT of the keys, taken under the monitor and then walked
    // without it. Holding the lock for the whole traversal would let an arbitrary user function
    // block every other thread on the map, and the JDK's own bulk operations are documented as
    // weakly consistent -- so a snapshot is within the contract and a global lock would not be.
    //
    // A key whose mapping disappeared between the snapshot and the read is skipped rather than
    // visited with a null value: `null` is this map's "absent", and handing it to a caller's
    // BiConsumer would be inventing a mapping that never existed.

    // The keys as of now. The one place the monitor is taken for a bulk operation.
    //
    // Package-private and not private because {@link KeySetView} reads it, and a nested class
    // reaching a private member of its enclosing one needs a synthetic accessor our javac does
    // not always generate (finding #268). Widening it by one step costs nothing outside the
    // package and removes the question.
    ArrayList<K> keyList() {
        ArrayList<K> out;
        synchronized (sync) {
            out = new ArrayList<K>(map.keySet());
        }
        return out;
    }

    // The live mappings of the snapshot, paired. Built once per bulk call so that key and value
    // are read together and a caller never sees a pair that was never in the map.
    private ArrayList<Map.Entry<K, V>> entrySnapshot() {
        ArrayList<K> keys = this.keyList();
        ArrayList<Map.Entry<K, V>> out = new ArrayList<Map.Entry<K, V>>();
        for (int i = 0; i < keys.size(); i++) {
            K k = keys.get(i);
            V v = this.get(k);
            if (v != null) {
                Map.Entry<K, V> e = Map.entry(k, v);
                out.add(e);
            }
        }
        return out;
    }

    // ---- over mappings ----

    public void forEach(long parallelismThreshold, BiConsumer<? super K, ? super V> action) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<K, V> e = entries.get(i);
            action.accept(e.getKey(), e.getValue());
        }
    }

    public <U> void forEach(long parallelismThreshold,
                            BiFunction<? super K, ? super V, ? extends U> transformer,
                            Consumer<? super U> action) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<K, V> e = entries.get(i);
            U u = transformer.apply(e.getKey(), e.getValue());
            // A null from the transformer means "not interesting", as it does throughout this
            // family; passing it on would make every action have to re-check.
            if (u != null) {
                action.accept(u);
            }
        }
    }

    /**
     * The first non-null result of applying {@code searchFunction} to a mapping, or null.
     *
     * <p>"First" is not a promise about order -- the JDK's runs in parallel and returns whichever
     * answer arrives first. What is promised is that the search stops once it has one, which is
     * what makes this different from a reduce over the same function.
     */
    public <U> U search(long parallelismThreshold,
                        BiFunction<? super K, ? super V, ? extends U> searchFunction) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        U found = null;
        int i = 0;
        while (found == null && i < entries.size()) {
            Map.Entry<K, V> e = entries.get(i);
            found = searchFunction.apply(e.getKey(), e.getValue());
            i = i + 1;
        }
        return found;
    }

    /**
     * Accumulates all mappings with {@code reducer}, after mapping each through
     * {@code transformer}; null if the map is empty.
     *
     * <p>Null and not a zero: there is no identity element to fall back on, because {@code U} is
     * the caller's type and this method has no way to make one. The primitive forms below do take
     * a basis for exactly that reason.
     */
    public <U> U reduce(long parallelismThreshold,
                        BiFunction<? super K, ? super V, ? extends U> transformer,
                        BiFunction<? super U, ? super U, ? extends U> reducer) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        U acc = null;
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<K, V> e = entries.get(i);
            U u = transformer.apply(e.getKey(), e.getValue());
            if (u != null) {
                if (acc == null) {
                    acc = u;
                } else {
                    acc = reducer.apply(acc, u);
                }
            }
        }
        return acc;
    }

    public double reduceToDouble(long parallelismThreshold,
                                 ToDoubleBiFunction<? super K, ? super V> transformer,
                                 double basis, DoubleBinaryOperator reducer) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        double acc = basis;
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<K, V> e = entries.get(i);
            acc = reducer.applyAsDouble(acc, transformer.applyAsDouble(e.getKey(), e.getValue()));
        }
        return acc;
    }

    public long reduceToLong(long parallelismThreshold,
                             ToLongBiFunction<? super K, ? super V> transformer,
                             long basis, LongBinaryOperator reducer) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        long acc = basis;
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<K, V> e = entries.get(i);
            acc = reducer.applyAsLong(acc, transformer.applyAsLong(e.getKey(), e.getValue()));
        }
        return acc;
    }

    public int reduceToInt(long parallelismThreshold,
                           ToIntBiFunction<? super K, ? super V> transformer,
                           int basis, IntBinaryOperator reducer) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        int acc = basis;
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<K, V> e = entries.get(i);
            acc = reducer.applyAsInt(acc, transformer.applyAsInt(e.getKey(), e.getValue()));
        }
        return acc;
    }

    // ---- over keys ----

    public void forEachKey(long parallelismThreshold, Consumer<? super K> action) {
        ArrayList<K> keys = this.keyList();
        for (int i = 0; i < keys.size(); i++) {
            action.accept(keys.get(i));
        }
    }

    public <U> void forEachKey(long parallelismThreshold,
                               Function<? super K, ? extends U> transformer,
                               Consumer<? super U> action) {
        ArrayList<K> keys = this.keyList();
        for (int i = 0; i < keys.size(); i++) {
            U u = transformer.apply(keys.get(i));
            if (u != null) {
                action.accept(u);
            }
        }
    }

    public <U> U searchKeys(long parallelismThreshold,
                            Function<? super K, ? extends U> searchFunction) {
        ArrayList<K> keys = this.keyList();
        U found = null;
        int i = 0;
        while (found == null && i < keys.size()) {
            found = searchFunction.apply(keys.get(i));
            i = i + 1;
        }
        return found;
    }

    public K reduceKeys(long parallelismThreshold,
                        BiFunction<? super K, ? super K, ? extends K> reducer) {
        ArrayList<K> keys = this.keyList();
        K acc = null;
        for (int i = 0; i < keys.size(); i++) {
            K k = keys.get(i);
            if (k != null) {
                if (acc == null) {
                    acc = k;
                } else {
                    acc = reducer.apply(acc, k);
                }
            }
        }
        return acc;
    }

    public <U> U reduceKeys(long parallelismThreshold,
                            Function<? super K, ? extends U> transformer,
                            BiFunction<? super U, ? super U, ? extends U> reducer) {
        ArrayList<K> keys = this.keyList();
        U acc = null;
        for (int i = 0; i < keys.size(); i++) {
            U u = transformer.apply(keys.get(i));
            if (u != null) {
                if (acc == null) {
                    acc = u;
                } else {
                    acc = reducer.apply(acc, u);
                }
            }
        }
        return acc;
    }

    public double reduceKeysToDouble(long parallelismThreshold,
                                     ToDoubleFunction<? super K> transformer,
                                     double basis, DoubleBinaryOperator reducer) {
        ArrayList<K> keys = this.keyList();
        double acc = basis;
        for (int i = 0; i < keys.size(); i++) {
            acc = reducer.applyAsDouble(acc, transformer.applyAsDouble(keys.get(i)));
        }
        return acc;
    }

    public long reduceKeysToLong(long parallelismThreshold, ToLongFunction<? super K> transformer,
                                 long basis, LongBinaryOperator reducer) {
        ArrayList<K> keys = this.keyList();
        long acc = basis;
        for (int i = 0; i < keys.size(); i++) {
            acc = reducer.applyAsLong(acc, transformer.applyAsLong(keys.get(i)));
        }
        return acc;
    }

    public int reduceKeysToInt(long parallelismThreshold, ToIntFunction<? super K> transformer,
                               int basis, IntBinaryOperator reducer) {
        ArrayList<K> keys = this.keyList();
        int acc = basis;
        for (int i = 0; i < keys.size(); i++) {
            acc = reducer.applyAsInt(acc, transformer.applyAsInt(keys.get(i)));
        }
        return acc;
    }

    // ---- over values ----

    private ArrayList<V> valueSnapshot() {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        ArrayList<V> out = new ArrayList<V>();
        for (int i = 0; i < entries.size(); i++) {
            out.add(entries.get(i).getValue());
        }
        return out;
    }

    public void forEachValue(long parallelismThreshold, Consumer<? super V> action) {
        ArrayList<V> values = this.valueSnapshot();
        for (int i = 0; i < values.size(); i++) {
            action.accept(values.get(i));
        }
    }

    public <U> void forEachValue(long parallelismThreshold,
                                 Function<? super V, ? extends U> transformer,
                                 Consumer<? super U> action) {
        ArrayList<V> values = this.valueSnapshot();
        for (int i = 0; i < values.size(); i++) {
            U u = transformer.apply(values.get(i));
            if (u != null) {
                action.accept(u);
            }
        }
    }

    public <U> U searchValues(long parallelismThreshold,
                              Function<? super V, ? extends U> searchFunction) {
        ArrayList<V> values = this.valueSnapshot();
        U found = null;
        int i = 0;
        while (found == null && i < values.size()) {
            found = searchFunction.apply(values.get(i));
            i = i + 1;
        }
        return found;
    }

    public V reduceValues(long parallelismThreshold,
                          BiFunction<? super V, ? super V, ? extends V> reducer) {
        ArrayList<V> values = this.valueSnapshot();
        V acc = null;
        for (int i = 0; i < values.size(); i++) {
            V v = values.get(i);
            if (v != null) {
                if (acc == null) {
                    acc = v;
                } else {
                    acc = reducer.apply(acc, v);
                }
            }
        }
        return acc;
    }

    public <U> U reduceValues(long parallelismThreshold,
                              Function<? super V, ? extends U> transformer,
                              BiFunction<? super U, ? super U, ? extends U> reducer) {
        ArrayList<V> values = this.valueSnapshot();
        U acc = null;
        for (int i = 0; i < values.size(); i++) {
            U u = transformer.apply(values.get(i));
            if (u != null) {
                if (acc == null) {
                    acc = u;
                } else {
                    acc = reducer.apply(acc, u);
                }
            }
        }
        return acc;
    }

    public double reduceValuesToDouble(long parallelismThreshold,
                                       ToDoubleFunction<? super V> transformer,
                                       double basis, DoubleBinaryOperator reducer) {
        ArrayList<V> values = this.valueSnapshot();
        double acc = basis;
        for (int i = 0; i < values.size(); i++) {
            acc = reducer.applyAsDouble(acc, transformer.applyAsDouble(values.get(i)));
        }
        return acc;
    }

    public long reduceValuesToLong(long parallelismThreshold,
                                   ToLongFunction<? super V> transformer,
                                   long basis, LongBinaryOperator reducer) {
        ArrayList<V> values = this.valueSnapshot();
        long acc = basis;
        for (int i = 0; i < values.size(); i++) {
            acc = reducer.applyAsLong(acc, transformer.applyAsLong(values.get(i)));
        }
        return acc;
    }

    public int reduceValuesToInt(long parallelismThreshold, ToIntFunction<? super V> transformer,
                                 int basis, IntBinaryOperator reducer) {
        ArrayList<V> values = this.valueSnapshot();
        int acc = basis;
        for (int i = 0; i < values.size(); i++) {
            acc = reducer.applyAsInt(acc, transformer.applyAsInt(values.get(i)));
        }
        return acc;
    }

    // ---- over entries ----

    public void forEachEntry(long parallelismThreshold, Consumer<? super Map.Entry<K, V>> action) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        for (int i = 0; i < entries.size(); i++) {
            action.accept(entries.get(i));
        }
    }

    public <U> void forEachEntry(long parallelismThreshold,
                                 Function<Map.Entry<K, V>, ? extends U> transformer,
                                 Consumer<? super U> action) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        for (int i = 0; i < entries.size(); i++) {
            U u = transformer.apply(entries.get(i));
            if (u != null) {
                action.accept(u);
            }
        }
    }

    public <U> U searchEntries(long parallelismThreshold,
                               Function<Map.Entry<K, V>, ? extends U> searchFunction) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        U found = null;
        int i = 0;
        while (found == null && i < entries.size()) {
            found = searchFunction.apply(entries.get(i));
            i = i + 1;
        }
        return found;
    }

    public Map.Entry<K, V> reduceEntries(long parallelismThreshold,
                                         BiFunction<Map.Entry<K, V>, Map.Entry<K, V>,
                                                 ? extends Map.Entry<K, V>> reducer) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        Map.Entry<K, V> acc = null;
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<K, V> e = entries.get(i);
            if (acc == null) {
                acc = e;
            } else {
                acc = reducer.apply(acc, e);
            }
        }
        return acc;
    }

    public <U> U reduceEntries(long parallelismThreshold,
                               Function<Map.Entry<K, V>, ? extends U> transformer,
                               BiFunction<? super U, ? super U, ? extends U> reducer) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        U acc = null;
        for (int i = 0; i < entries.size(); i++) {
            U u = transformer.apply(entries.get(i));
            if (u != null) {
                if (acc == null) {
                    acc = u;
                } else {
                    acc = reducer.apply(acc, u);
                }
            }
        }
        return acc;
    }

    public double reduceEntriesToDouble(long parallelismThreshold,
                                        ToDoubleFunction<Map.Entry<K, V>> transformer,
                                        double basis, DoubleBinaryOperator reducer) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        double acc = basis;
        for (int i = 0; i < entries.size(); i++) {
            acc = reducer.applyAsDouble(acc, transformer.applyAsDouble(entries.get(i)));
        }
        return acc;
    }

    public long reduceEntriesToLong(long parallelismThreshold,
                                    ToLongFunction<Map.Entry<K, V>> transformer,
                                    long basis, LongBinaryOperator reducer) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        long acc = basis;
        for (int i = 0; i < entries.size(); i++) {
            acc = reducer.applyAsLong(acc, transformer.applyAsLong(entries.get(i)));
        }
        return acc;
    }

    public int reduceEntriesToInt(long parallelismThreshold,
                                  ToIntFunction<Map.Entry<K, V>> transformer,
                                  int basis, IntBinaryOperator reducer) {
        ArrayList<Map.Entry<K, V>> entries = this.entrySnapshot();
        int acc = basis;
        for (int i = 0; i < entries.size(); i++) {
            acc = reducer.applyAsInt(acc, transformer.applyAsInt(entries.get(i)));
        }
        return acc;
    }

    /**
     * The keys of a ConcurrentHashMap, seen as a Set.
     *
     * <p>Two things in one, and it is worth separating them. Reading -- {@code contains},
     * {@code iterator}, {@code size} -- is just the map's keys. WRITING is where the mapped value
     * comes in: a Set's {@code add} has only a key to offer, and this map needs a value for it, so
     * a view created by {@link ConcurrentHashMap#keySet()} refuses to add and one created by
     * {@link ConcurrentHashMap#keySet(Object)} stores the value it was given. That is the entire
     * difference between the two factories.
     *
     * <p>Removal always works, mapped value or not: forgetting a key needs nothing extra.
     */
    public static final class KeySetView<K, V> implements Set<K>, Serializable {

        private final ConcurrentHashMap<K, V> map;
        // The value an added key is mapped to, or null when this view cannot add.
        private final V value;

        KeySetView(ConcurrentHashMap<K, V> map, V value) {
            this.map = map;
            this.value = value;
        }

        // The value `add` stores, or null if this view refuses to add. Public because it is the
        // only way to tell the two kinds of view apart from outside.
        public V getMappedValue() {
            return value;
        }

        // The map behind this view. Reading the map through the set is legitimate -- the set is a
        // projection of it, not a copy of its keys.
        public ConcurrentHashMap<K, V> getMap() {
            return map;
        }

        public int size() {
            return map.size();
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        public Iterator<K> iterator() {
            return map.keyList().iterator();
        }

        public boolean add(K e) {
            if (value == null) {
                throw new UnsupportedOperationException(
                        "this view has no mapped value; use keySet(mappedValue)");
            }
            return map.putIfAbsent(e, value) == null;
        }

        public boolean addAll(Collection<? extends K> c) {
            boolean changed = false;
            Iterator<? extends K> it = c.iterator();
            while (it.hasNext()) {
                if (this.add(it.next())) {
                    changed = true;
                }
            }
            return changed;
        }

        public boolean remove(Object o) {
            return map.remove(o) != null;
        }

        public void clear() {
            map.clear();
        }

        public boolean containsAll(Collection<?> c) {
            boolean all = true;
            Iterator<?> it = c.iterator();
            while (it.hasNext()) {
                if (!this.contains(it.next())) {
                    all = false;
                }
            }
            return all;
        }

        public boolean removeAll(Collection c) {
            boolean changed = false;
            Iterator it = c.iterator();
            while (it.hasNext()) {
                if (this.remove(it.next())) {
                    changed = true;
                }
            }
            return changed;
        }

        public boolean retainAll(Collection<?> c) {
            boolean changed = false;
            ArrayList<K> keys = map.keyList();
            for (int i = 0; i < keys.size(); i++) {
                K k = keys.get(i);
                if (!c.contains(k)) {
                    if (this.remove(k)) {
                        changed = true;
                    }
                }
            }
            return changed;
        }

        public boolean removeIf(Predicate<? super K> filter) {
            boolean changed = false;
            ArrayList<K> keys = map.keyList();
            for (int i = 0; i < keys.size(); i++) {
                K k = keys.get(i);
                if (filter.test(k)) {
                    if (this.remove(k)) {
                        changed = true;
                    }
                }
            }
            return changed;
        }

        public void forEach(Consumer<? super K> action) {
            ArrayList<K> keys = map.keyList();
            for (int i = 0; i < keys.size(); i++) {
                action.accept(keys.get(i));
            }
        }

        public Object[] toArray() {
            ArrayList<K> keys = map.keyList();
            Object[] out = new Object[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                out[i] = keys.get(i);
            }
            return out;
        }

        public <T> T[] toArray(T[] a) {
            // Filled by hand rather than delegating to ArrayList.toArray(T[]): a `T[]` argument
            // passed on to another generic method is where finding #279 reports a spurious
            // ambiguity, and the loop is shorter than the workaround would be.
            ArrayList<K> keys = map.keyList();
            Object[] out = a;
            if (out.length < keys.size()) {
                out = new Object[keys.size()];
            }
            for (int i = 0; i < keys.size(); i++) {
                out[i] = keys.get(i);
            }
            if (out.length > keys.size()) {
                out[keys.size()] = null;
            }
            return (T[]) out;
        }

        public Spliterator<K> spliterator() {
            return Spliterators.spliterator(map.keyList(),
                    Spliterator.DISTINCT | Spliterator.CONCURRENT | Spliterator.NONNULL);
        }

        // Set equality: same size, same members. Deliberately not identity -- a KeySetView must
        // compare equal to any other Set with the same keys, which is what the Set contract says
        // and what a caller putting one in a HashMap depends on.
        public boolean equals(Object o) {
            boolean same = false;
            if (o == this) {
                same = true;
            } else if (o instanceof Set) {
                Set other = (Set) o;
                if (other.size() == this.size()) {
                    same = this.containsAll(other);
                }
            }
            return same;
        }

        public int hashCode() {
            int h = 0;
            ArrayList<K> keys = map.keyList();
            for (int i = 0; i < keys.size(); i++) {
                K k = keys.get(i);
                if (k != null) {
                    h = h + k.hashCode();
                }
            }
            return h;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            ArrayList<K> keys = map.keyList();
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(String.valueOf(keys.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
    }
}

// The keys or values of a ConcurrentHashMap as an Enumeration, over a list the caller already
// snapshotted. Top-level and package-private: a class nested in a *generic* class is miscompiled
// (finding #13), and ConcurrentHashMap is generic.
final class ChmEnumeration<E> implements Enumeration<E> {

    private final ArrayList<E> items;
    private int at;

    ChmEnumeration(ArrayList<E> items) {
        this.items = items;
    }

    public boolean hasMoreElements() {
        return at < items.size();
    }

    public E nextElement() {
        if (at >= items.size()) {
            throw new java.util.NoSuchElementException();
        }
        E e = items.get(at);
        at = at + 1;
        return e;
    }
}
