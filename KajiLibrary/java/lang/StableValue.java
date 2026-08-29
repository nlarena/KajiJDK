package java.lang;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;


/**
 * A holder that can be set at most ONCE, and is treated as a constant from then on.
 *
 * <p>It sits between two things Java already had and neither of which fits. A {@code final}
 * field is a real constant but has to be computed in the constructor, so anything expensive is
 * paid for whether or not it is ever used. A lazily-initialised mutable field defers the cost
 * but gives up the guarantee: nothing stops a second write, so every read has to be defensive.
 * A stable value defers the computation AND keeps the guarantee — write once, and every reader
 * afterwards sees that one value.
 *
 * <p>The factories are the part worth knowing. Beyond a single holder, this class hands out
 * lazily-computed VIEWS: a supplier that memoises, a function that memoises per input, and a
 * list and a map whose elements are computed the first time each one is asked for. Each is the
 * same idea applied to a shape — one slot, one slot per index, one slot per key.
 *
 * @implNote A KajiLibrary subset of the JDK preview API. The JDK's version is known to the VM,
 *           which lets it constant-fold a set value as if it were {@code final}; this one is
 *           ordinary Java, so it delivers the semantics without the optimisation. Contents are
 *           guarded by a monitor rather than by trusted-final magic.
 */
public interface StableValue<T> {

    /**
     * Sets the content if it has none.
     *
     * @return {@code true} if this call was the one that set it
     */
    boolean trySet(T value);

    /** The content, or {@code other} if unset. */
    T orElse(T other);

    /**
     * The content.
     *
     * @throws NoSuchElementException if unset — a stable value that is empty is not the same as
     *         one holding {@code null}, and returning {@code null} would confuse the two
     */
    T orElseThrow();

    boolean isSet();

    /**
     * The content, computing and setting it with {@code supplier} if unset.
     *
     * <p>This is the method the whole class exists for: it is the lazy-initialisation idiom with
     * the double-write hole closed. If two threads race, both may run the supplier but only one
     * result is kept, and both callers are handed the kept one.
     */
    T orElseSet(Supplier<? extends T> supplier);

    /**
     * Sets the content.
     *
     * @throws IllegalStateException if already set
     */
    void setOrThrow(T value);

    /** Two stable values are equal only if they are the same object. */
    boolean equals(Object obj);

    int hashCode();

    /** A new, unset stable value. */
    static <T> StableValue<T> of() {
        return new StableHolder<T>();
    }

    /** A new stable value already holding {@code content}. */
    static <T> StableValue<T> of(T content) {
        StableHolder<T> holder = new StableHolder<T>();
        holder.setOrThrow(content);
        return holder;
    }

    /** A supplier that calls {@code underlying} at most once and remembers the answer. */
    static <T> Supplier<T> supplier(Supplier<? extends T> underlying) {
        if (underlying == null) {
            throw new NullPointerException();
        }
        return new StableSupplier<T>(underlying);
    }

    /**
     * An {@link IntFunction} over {@code [0, size)} that computes each index at most once.
     *
     * @throws IllegalArgumentException from the returned function, for an index out of range
     */
    static <R> IntFunction<R> intFunction(int size, IntFunction<? extends R> underlying) {
        if (underlying == null) {
            throw new NullPointerException();
        }
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be negative");
        }
        return new StableIntFunction<R>(size, underlying);
    }

    /**
     * A function over a FIXED set of inputs that computes each one at most once.
     *
     * <p>The input set is required, not optional, and that is the design: knowing every key up
     * front is what lets the memo table be allocated once instead of growing, and what turns an
     * unexpected argument into an error rather than an unbounded cache.
     */
    static <T, R> Function<T, R> function(Set<? extends T> inputs,
            Function<? super T, ? extends R> underlying) {
        if (inputs == null || underlying == null) {
            throw new NullPointerException();
        }
        return new StableFunction<T, R>(inputs, underlying);
    }

    /** An unmodifiable list of {@code size} elements, each computed on first access. */
    static <E> List<E> list(int size, IntFunction<? extends E> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be negative");
        }
        return new StableList<E>(size, mapper);
    }

    /** An unmodifiable map over {@code keys}, each value computed on first access. */
    static <K, V> Map<K, V> map(Set<K> keys, Function<? super K, ? extends V> mapper) {
        if (keys == null || mapper == null) {
            throw new NullPointerException();
        }
        return new StableMap<K, V>(keys, mapper);
    }
}


/**
 * The single set-once slot every other shape in this file is built from.
 *
 * <p>{@code set} and {@code content} are two fields and not one, because {@code null} is a legal
 * content: without the flag, "unset" and "set to null" would be indistinguishable.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class StableHolder<T> implements StableValue<T> {

    private final Object sync = new Object();

    private Object content;
    private boolean set;

    StableHolder() {
        this.content = null;
        this.set = false;
    }

    @Override
    public boolean trySet(T value) {
        boolean won = false;
        synchronized (this.sync) {
            if (!this.set) {
                this.content = value;
                this.set = true;
                won = true;
            }
        }
        return won;
    }

    @Override
    public T orElse(T other) {
        Object held;
        boolean have;
        synchronized (this.sync) {
            have = this.set;
            held = this.content;
        }
        if (!have) {
            return other;
        }
        return (T) held;
    }

    @Override
    public T orElseThrow() {
        Object held;
        boolean have;
        synchronized (this.sync) {
            have = this.set;
            held = this.content;
        }
        if (!have) {
            throw new NoSuchElementException("no content");
        }
        return (T) held;
    }

    @Override
    public boolean isSet() {
        boolean have;
        synchronized (this.sync) {
            have = this.set;
        }
        return have;
    }

    @Override
    public T orElseSet(Supplier<? extends T> supplier) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        Object held;
        boolean have;
        synchronized (this.sync) {
            have = this.set;
            held = this.content;
        }
        if (have) {
            return (T) held;
        }
        // Outside the monitor: the supplier is the caller code and may take any lock, or ask
        // another stable value for a value. Racing callers may both compute; one result wins.
        Supplier raw = supplier;
        Object computed = raw.get();
        Object winner;
        synchronized (this.sync) {
            if (!this.set) {
                this.content = computed;
                this.set = true;
            }
            winner = this.content;
        }
        return (T) winner;
    }

    @Override
    public void setOrThrow(T value) {
        if (!this.trySet(value)) {
            throw new IllegalStateException("content already set");
        }
    }
}


/** A supplier that runs the underlying one at most once. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class StableSupplier<T> implements Supplier<T> {

    private final StableHolder<T> slot;
    private final Supplier underlying;

    StableSupplier(Supplier underlying) {
        this.slot = new StableHolder<T>();
        this.underlying = underlying;
    }

    @Override
    public T get() {
        Supplier raw = this.underlying;
        // Through the interface: calling orElseSet on the class that overrides it is reported as
        // ambiguous between the interface declaration and the override (finding #254).
        StableValue<T> as = this.slot;
        // The cast is the price of the raw argument: passing a raw Supplier makes the call an
        // unchecked invocation, so its return type erases to Object.
        return (T) as.orElseSet(raw);
    }
}


/** One slot per index, over a fixed range. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class StableIntFunction<R> implements IntFunction<R> {

    private final Object[] slots;
    private final IntFunction underlying;

    StableIntFunction(int size, IntFunction underlying) {
        this.slots = new Object[size];
        this.underlying = underlying;
        int i = 0;
        while (i < size) {
            this.slots[i] = new StableHolder<R>();
            i = i + 1;
        }
    }

    @Override
    public R apply(int index) {
        if (index < 0 || index >= this.slots.length) {
            throw new IllegalArgumentException("index out of range: " + index);
        }
        StableHolder holder = (StableHolder) this.slots[index];
        Object computed = holder.orElse(StableIntFunction.absent());
        if (computed != StableIntFunction.absent()) {
            return (R) computed;
        }
        IntFunction raw = this.underlying;
        Object value = raw.apply(index);
        holder.trySet(value);
        return (R) holder.orElseThrow();
    }

    // A sentinel distinct from every legal content, so that a slot legitimately holding null is
    // not recomputed forever. One instance, shared: only its identity matters.
    private static final Object ABSENT = new Object();

    private static Object absent() {
        return StableIntFunction.ABSENT;
    }
}


/** One slot per allowed input. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class StableFunction<T, R> implements Function<T, R> {

    private final Map<Object, Object> slots;
    private final Function underlying;

    StableFunction(Set<? extends T> inputs, Function underlying) {
        this.slots = new HashMap<Object, Object>();
        this.underlying = underlying;
        Iterator<? extends T> it = inputs.iterator();
        while (it.hasNext()) {
            Object key = it.next();
            this.slots.put(key, new StableHolder<R>());
        }
    }

    @Override
    public R apply(T input) {
        Object found = this.slots.get(input);
        if (found == null) {
            throw new IllegalArgumentException("input not in the declared set: " + input);
        }
        StableHolder holder = (StableHolder) found;
        if (holder.isSet()) {
            return (R) holder.orElseThrow();
        }
        Function raw = this.underlying;
        Object value = raw.apply(input);
        holder.trySet(value);
        return (R) holder.orElseThrow();
    }
}


/** A fixed-size list whose elements are computed the first time each is read. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class StableList<E> implements List<E> {

    private final StableIntFunction<E> elements;
    private final int size;

    StableList(int size, IntFunction mapper) {
        this.size = size;
        this.elements = new StableIntFunction<E>(size, mapper);
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException("index out of range: " + index);
        }
        return this.elements.apply(index);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return this.indexOf(o) >= 0;
    }

    /** Reading the list to find something computes every element it passes, by definition. */
    @Override
    public int indexOf(Object o) {
        int i = 0;
        while (i < this.size) {
            E here = this.get(i);
            if (o == null) {
                if (here == null) {
                    return i;
                }
            } else if (o.equals(here)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    @Override
    public Iterator<E> iterator() {
        return new StableListIterator<E>(this);
    }

    // ---- everything that would change the list, refused ----

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}


/** Walks a StableList, computing each element as it reaches it. */
final class StableListIterator<E> implements Iterator<E> {

    private final StableList<E> list;
    private int at;

    StableListIterator(StableList<E> list) {
        this.list = list;
        this.at = 0;
    }

    @Override
    public boolean hasNext() {
        return this.at < this.list.size();
    }

    @Override
    public E next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        E value = this.list.get(this.at);
        this.at = this.at + 1;
        return value;
    }
}


/** A map with fixed keys whose values are computed the first time each is read. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class StableMap<K, V> implements Map<K, V> {

    private final StableFunction<K, V> values;
    private final Object[] keys;

    StableMap(Set<K> keys, Function mapper) {
        this.values = new StableFunction<K, V>(keys, mapper);
        int n = keys.size();
        this.keys = new Object[n];
        Iterator<K> it = keys.iterator();
        int i = 0;
        while (it.hasNext() && i < n) {
            this.keys[i] = it.next();
            i = i + 1;
        }
    }

    @Override
    public int size() {
        return this.keys.length;
    }

    @Override
    public boolean isEmpty() {
        return this.keys.length == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        int i = 0;
        while (i < this.keys.length) {
            Object here = this.keys[i];
            if (key == null) {
                if (here == null) {
                    return true;
                }
            } else if (key.equals(here)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** Asking whether a value is present computes every value, which is the honest answer. */
    @Override
    public boolean containsValue(Object value) {
        int i = 0;
        while (i < this.keys.length) {
            V here = this.values.apply((K) this.keys[i]);
            if (value == null) {
                if (here == null) {
                    return true;
                }
            } else if (value.equals(here)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** The value for {@code key}, computed on first access, or null for an unknown key. */
    @Override
    public V get(Object key) {
        if (!this.containsKey(key)) {
            return null;
        }
        return this.values.apply((K) key);
    }

    @Override
    public Set<K> keySet() {
        HashSet<K> out = new HashSet<K>();
        int i = 0;
        while (i < this.keys.length) {
            if (this.keys[i] != null) {
                out.add((K) this.keys[i]);
            }
            i = i + 1;
        }
        return out;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
