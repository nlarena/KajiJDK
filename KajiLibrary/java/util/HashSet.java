package java.util;

// Compiled with `-cp KajiLibrary` so Set/Iterator bind to KajiLibrary's own (subset) types.
import java.util.Set;
import java.util.Iterator;

// KajiLibrary's java.util.HashSet<E> — a set backed by a hash table (open addressing with
// linear probing over one Object[]), doubling past a ~50% load factor. `add` returns false
// if the element is already present; `remove` re-inserts the trailing cluster to keep the
// probe invariant. `iterator()` walks the table (see HashSetItr below). (The JDK's HashSet
// delegates to a HashMap; ours holds its own table.)
public class HashSet<E> implements Set<E> {

    // Package-private so HashSetItr can walk the table (still implementation, not API surface).
    Object[] table;
    private int size;

    public HashSet() {
        this.table = new Object[16];
        this.size = 0;
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
        return this.table[this.slotFor(o)] != null;
    }

    public boolean add(E e) {
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
    }

    public Iterator<E> iterator() {
        return new HashSetItr<E>(this);
    }

    // Double the table and re-insert every element into the fresh, larger array.
    private void resize() {
        Object[] old = this.table;
        int newCap = old.length * 2;
        this.table = new Object[newCap];
        this.size = 0;
        for (int i = 0; i < old.length; i++) {
            if (old[i] != null) {
                this.add((E) old[i]);
            }
        }
    }
}

// HashSet's iterator, as a same-file top-level class (compiler-generated enclosing capture is
// broken for a class inside a generic one — finding #13 — so no inner/anonymous class). Walks
// the backing table, skipping empty slots.
final class HashSetItr<E> implements Iterator<E> {

    private final HashSet<E> set;
    private int index = 0;

    HashSetItr(HashSet<E> set) {
        this.set = set;
        this.advance();
    }

    // Advance `index` to the next occupied slot (or past the end).
    private void advance() {
        while (this.index < this.set.table.length && this.set.table[this.index] == null) {
            this.index = this.index + 1;
        }
    }

    public boolean hasNext() {
        return this.index < this.set.table.length;
    }

    public E next() {
        E element = (E) this.set.table[this.index];
        this.index = this.index + 1;
        this.advance();
        return element;
    }
}
