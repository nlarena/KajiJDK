package java.util;

// Compiled with `-cp KajiLibrary` so Set/Iterator bind to KajiLibrary's own (subset) types.
import java.lang.Cloneable;
import java.io.Serializable;
import java.util.Set;
import java.util.Iterator;

// KajiLibrary's java.util.HashSet<E> — a set backed by a hash table (open addressing with
// linear probing over one Object[]), doubling past a ~50% load factor. `add` returns false
// if the element is already present; `remove` re-inserts the trailing cluster to keep the
// probe invariant. `iterator()` walks the table (see HashSetItr below). (The JDK's HashSet
// delegates to a HashMap; ours holds its own table.)
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Serializable, Cloneable {

    // Package-private so HashSetItr can walk the table (still implementation, not API surface).
    Object[] table;
    private int size;

    /**
     * The null element lives apart from the table.
     *
     * <p>For the same reason as in {@link HashMap}: the table is open-addressed and uses null as the
     * empty-slot mark, so a null inside would be at once "taken" and "free". And accepting it is
     * needed: a `HashSet` allows <b>one</b> null element, and without that a map with a null key
     * could not return it from `keySet()`.
     *
     * <p>Package-private because the iterator has to see it in order to emit it.
     */
    boolean hasNull = false;

    public HashSet() {
        this.table = new Object[16];
        this.size = 0;
    }

    /**
     * With an initial capacity.
     *
     * <p>The table is sized at **twice** what was asked for, and that is no arbitrary margin: this
     * implementation is open-addressed with linear probing, and past half occupancy the collision
     * clusters start merging into each other. Asking for capacity for `n` means "I want to put `n` in
     * without it growing", and for that `2n` slots are needed.
     */
    public HashSet(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        int cap = initialCapacity * 2;
        if (cap < 16) {
            cap = 16;
        }
        this.table = new Object[cap];
        this.size = 0;
    }

    // The load factor is accepted and **ignored**: this table does not use it (see above). The JDK
    // takes it to decide when to grow; here that threshold is fixed at half.
    public HashSet(int initialCapacity, float loadFactor) {
        this(initialCapacity);
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
    }

    /**
     * A set sized for `numElements` without it growing.
     *
     * <p>It exists because `new HashSet<>(n)` does **not** mean that: that `n` is the table's
     * capacity, not the number of elements, and with the JDK's load factor a `new HashSet<>(100)`
     * grows at 75. It is one of the oldest traps in the API, and that is why Java 19 added this
     * factory with a name that does say what it does.
     */
    public static <T> HashSet<T> newHashSet(int numElements) {
        if (numElements < 0) {
            throw new IllegalArgumentException("Negative number of elements: " + numElements);
        }
        return new HashSet<T>(numElements);
    }

    // It copies another collection's elements, dropping the repeats.
    //
    // It is not a luxury: it is the idiom for freezing an argument the caller could go on modifying
    // (`this.violations = new HashSet<>(violations)`), and there was no way of writing it. Until #293
    // it could be written all the same and compiled **wrong** in silence -- the argument was
    // evaluated, the no-argument constructor was called and the set was born empty.
    public HashSet(Collection<? extends E> c) {
        this.table = new Object[16];
        this.size = 0;
        this.addAll(c);
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    // The slot holding `e`, or the first empty slot on its probe sequence if absent.
    private int slotFor(Object e) {
        int cap = this.table.length;
        int i = e.hashCode() & (cap - 1);
        while (this.table[i] != null) {
            if (this.table[i].equals(e)) {
                return i;
            }
            i = (i + 1) & (cap - 1);
        }
        return i;
    }

    public boolean contains(Object o) {
        if (o == null) {
            return this.hasNull;
        }
        return this.table[this.slotFor(o)] != null;
    }

    public boolean add(E e) {
        if (e == null) {
            if (this.hasNull) {
                return false;
            }
            this.hasNull = true;
            this.size = this.size + 1;
            return true;
        }
        if (this.size * 2 >= this.table.length) {
            this.resize();
        }
        int i = this.slotFor(e);
        if (this.table[i] != null) {
            return false;
        }
        this.table[i] = e;
        this.size = this.size + 1;
        return true;
    }

    public boolean remove(Object o) {
        if (o == null) {
            if (!this.hasNull) {
                return false;
            }
            this.hasNull = false;
            this.size = this.size - 1;
            return true;
        }
        int cap = this.table.length;
        int i = this.slotFor(o);
        if (this.table[i] == null) {
            return false;
        }
        this.table[i] = null;
        this.size = this.size - 1;
        int j = (i + 1) & (cap - 1);
        while (this.table[j] != null) {
            Object e = this.table[j];
            this.table[j] = null;
            this.size = this.size - 1;
            this.add((E) e);
            j = (j + 1) & (cap - 1);
        }
        return true;
    }

    public void clear() {
        for (int i = 0; i < this.table.length; i++) {
            this.table[i] = null;
        }
        this.size = 0;
        this.hasNull = false;
    }

    public Iterator<E> iterator() {
        return new HashSetItr<E>(this);
    }

    // Double the table and re-insert every element into the fresh, larger array.
    private void resize() {
        Object[] old = this.table;
        int newCap = old.length * 2;
        this.table = new Object[newCap];
        // The null is not in the table: its +1 has to be kept by hand.
        this.size = this.hasNull ? 1 : 0;
        for (int i = 0; i < old.length; i++) {
            if (old[i] != null) {
                this.add((E) old[i]);
            }
        }
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.SIZED);
    }
}

// HashSet's iterator, as a same-file top-level class (compiler-generated enclosing capture is
// broken for a class inside a generic one — finding #13 — so no inner/anonymous class). Walks
// the backing table, skipping empty slots.
// The iterator walks a SNAPSHOT of the elements, not the table.
//
// ===============================================================================================
// WHY A SNAPSHOT, WHEN WALKING THE TABLE IS CHEAPER
// ===============================================================================================
//
// Because of `remove()`. This set uses open addressing, and `HashSet.remove` closes the gap it
// leaves by re-inserting the cluster that followed it -- see that method. Re-inserting can land an
// element in a slot BEFORE the one the iterator is on, and an index-based walk would then skip it.
// The bug would show up only on removals that happen to land in a probe cluster, which is the worst
// kind to find.
//
// The snapshot costs one array per iteration and makes `remove()` correct by construction: the walk
// is over a list that nothing reshuffles, and the removal goes to the set through its own `remove`.
//
// What it does not do is fail fast. This set keeps no modification count, so a change made behind
// the iterator's back is invisible here as it was before; that is a separate gap and this does not
// widen it.
final class HashSetItr<E> implements Iterator<E> {

    private final HashSet<E> set;
    private final Object[] snapshot;
    private int index;
    private E last;
    private boolean removable;

    HashSetItr(HashSet<E> set) {
        this.set = set;
        this.snapshot = new Object[set.size()];
        int n = 0;
        if (set.hasNull) {
            // The null goes first, and once. See the field of the same name on `HashSet`.
            this.snapshot[n] = null;
            n = n + 1;
        }
        for (int i = 0; i < set.table.length && n < this.snapshot.length; i++) {
            if (set.table[i] != null) {
                this.snapshot[n] = set.table[i];
                n = n + 1;
            }
        }
    }

    public boolean hasNext() {
        return this.index < this.snapshot.length;
    }

    public E next() {
        if (this.index >= this.snapshot.length) {
            throw new NoSuchElementException();
        }
        this.last = (E) this.snapshot[this.index];
        this.index = this.index + 1;
        this.removable = true;
        return this.last;
    }

    /**
     * Removes from the set the element {@link #next} last returned.
     *
     * @throws IllegalStateException if `next` has not been called, or if this is a second `remove`
     *     for the same element
     */
    public void remove() {
        if (!this.removable) {
            throw new IllegalStateException();
        }
        this.removable = false;
        this.set.remove(this.last);
    }
}
