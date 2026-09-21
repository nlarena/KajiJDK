package java.util.concurrent;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

// A list that never mutates its backing array: every write copies the array, swaps the copy
// in, and leaves readers walking the old one. Reads therefore take no lock at all and can
// never see a half-finished write — the trade is that each write is O(n), so this pays off
// exactly when reads vastly outnumber writes (a listener list, a config snapshot).
//
// An iterator holds the array it started on, so it reflects the list as of its creation and
// never throws ConcurrentModificationException.
//
// Single-exit style throughout (finding #105).
public class CopyOnWriteArrayList<E> implements List<E>, Serializable {

    private final Object sync = new Object();
    // Never mutated in place — replaced wholesale under `sync` by every write.
    private volatile Object[] elements = new Object[0];

    public CopyOnWriteArrayList() {
    }

    /**
     * A list holding the elements of {@code c}, in the order its iterator returns them.
     *
     * <p>The elements are copied into a fresh array rather than adopting whatever
     * {@code c.toArray()} hands back. Two reasons, and both are bugs avoided: some collections
     * return their *own* backing array, which the caller could then mutate behind this list's back
     * -- destroying the one invariant the class has, that the array is never written after
     * publication; and {@code toArray()} on a typed collection may return a {@code String[]} rather
     * than an {@code Object[]}, which would make a later {@code set} of an unrelated type throw
     * ArrayStoreException from deep inside an unrelated call.
     */
    public CopyOnWriteArrayList(java.util.Collection<? extends E> c) {
        Object[] source = c.toArray();
        Object[] copy = new Object[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i];
        }
        elements = copy;
    }

    // A list over a copy of the given array; the caller keeps its own and may mutate it freely.
    public CopyOnWriteArrayList(E[] toCopyIn) {
        Object[] copy = new Object[toCopyIn.length];
        for (int i = 0; i < toCopyIn.length; i++) {
            copy[i] = toCopyIn[i];
        }
        elements = copy;
    }

    public int size() {
        return elements.length;
    }

    public boolean isEmpty() {
        return elements.length == 0;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    // Null-safe equality. Written as a helper with an explicit if/else because a
    // **boolean-valued** ternary (`o == null ? e == null : o.equals(e)`). This note said our javac
    // rejected that shape with "operando no numérico" -- finding #109, which is fixed. The helper
    // stays because it reads well and is used from several places, but it is no longer a
    // workaround.
    private static boolean eq(Object a, Object b) {
        boolean same;
        if (a == null) {
            same = b == null;
        } else {
            same = a.equals(b);
        }
        return same;
    }

    public int indexOf(Object o) {
        Object[] snapshot = elements;
        int found = -1;
        for (int i = 0; i < snapshot.length; i++) {
            if (found < 0) {
                Object e = snapshot[i];
                if (eq(o, e)) {
                    found = i;
                }
            }
        }
        return found;
    }

    public E get(int index) {
        Object[] snapshot = elements;
        if (index < 0 || index >= snapshot.length) {
            throw new IndexOutOfBoundsException();
        }
        return (E) snapshot[index];
    }

    public boolean add(E e) {
        synchronized (sync) {
            Object[] old = elements;
            Object[] copy = new Object[old.length + 1];
            for (int i = 0; i < old.length; i++) {
                copy[i] = old[i];
            }
            copy[old.length] = e;
            elements = copy;
        }
        return true;
    }

    public void add(int index, E element) {
        synchronized (sync) {
            Object[] old = elements;
            if (index < 0 || index > old.length) {
                throw new IndexOutOfBoundsException();
            }
            Object[] copy = new Object[old.length + 1];
            for (int i = 0; i < index; i++) {
                copy[i] = old[i];
            }
            copy[index] = element;
            for (int i = index; i < old.length; i++) {
                copy[i + 1] = old[i];
            }
            elements = copy;
        }
    }

    public E set(int index, E element) {
        E prev;
        synchronized (sync) {
            Object[] old = elements;
            if (index < 0 || index >= old.length) {
                throw new IndexOutOfBoundsException();
            }
            prev = (E) old[index];
            Object[] copy = new Object[old.length];
            for (int i = 0; i < old.length; i++) {
                copy[i] = old[i];
            }
            copy[index] = element;
            elements = copy;
        }
        return prev;
    }

    public E remove(int index) {
        E prev;
        synchronized (sync) {
            Object[] old = elements;
            if (index < 0 || index >= old.length) {
                throw new IndexOutOfBoundsException();
            }
            prev = (E) old[index];
            Object[] copy = new Object[old.length - 1];
            for (int i = 0; i < index; i++) {
                copy[i] = old[i];
            }
            for (int i = index + 1; i < old.length; i++) {
                copy[i - 1] = old[i];
            }
            elements = copy;
        }
        return prev;
    }

    public boolean remove(Object o) {
        boolean removed;
        synchronized (sync) {
            int index = indexOf(o);
            if (index >= 0) {
                Object[] old = elements;
                Object[] copy = new Object[old.length - 1];
                for (int i = 0; i < index; i++) {
                    copy[i] = old[i];
                }
                for (int i = index + 1; i < old.length; i++) {
                    copy[i - 1] = old[i];
                }
                elements = copy;
                removed = true;
            } else {
                removed = false;
            }
        }
        return removed;
    }

    // Add only if absent, atomically — the reason this class exists for listener lists.
    public boolean addIfAbsent(E e) {
        boolean added;
        synchronized (sync) {
            if (indexOf(e) < 0) {
                add(e);
                added = true;
            } else {
                added = false;
            }
        }
        return added;
    }

    /**
     * Adds the elements of {@code c} that are not already here, and reports how many went in.
     *
     * <p>Held under the monitor for the whole batch: {@link #addIfAbsent} is already atomic on its
     * own, but a caller adding a batch wants "none of these is a duplicate of anything, including of
     * each other", and that is only true if no other writer interleaves. Duplicates *within* {@code
     * c} are dropped too, since each element is checked against the list as it stands.
     */
    public int addAllAbsent(java.util.Collection<? extends E> c) {
        int added = 0;
        synchronized (sync) {
            Iterator<? extends E> it = c.iterator();
            while (it.hasNext()) {
                E e = it.next();
                if (addIfAbsent(e)) {
                    added = added + 1;
                }
            }
        }
        return added;
    }

    // The index of the first occurrence of `e` at or after `index`, or -1. The bounded form exists
    // so a caller scanning for repeated occurrences does not restart from zero each time -- turning
    // an O(n^2) sweep into an O(n) one.
    public int indexOf(E e, int index) {
        Object[] snapshot = elements;
        int found = -1;
        for (int i = index; i < snapshot.length; i++) {
            if (found < 0 && eq(e, snapshot[i])) {
                found = i;
            }
        }
        return found;
    }

    // The index of the last occurrence of `e` at or before `index`, or -1 -- the backward sweep.
    public int lastIndexOf(E e, int index) {
        Object[] snapshot = elements;
        int found = -1;
        int from = index;
        if (from >= snapshot.length) {
            from = snapshot.length - 1;
        }
        for (int i = from; i >= 0; i--) {
            if (found < 0 && eq(e, snapshot[i])) {
                found = i;
            }
        }
        return found;
    }

    public void clear() {
        synchronized (sync) {
            elements = new Object[0];
        }
    }

    // Walks the array as it was when the iterator was created.
    public Iterator<E> iterator() {
        return new CowItr<E>(elements);
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
                        Spliterator.IMMUTABLE);
    }

    // ---- the bulk operations, written out -----------------------------------------------------
    //
    // Written out and not inherited because in the JDK this class extends no skeleton: it is a bare
    // `implements List`. The bodies are `AbstractCollection`'s, over a snapshot and not over the
    // live iterator.
    //
    // The real cost here is another: every `remove` of this class copies the whole array, so a
    // `removeAll` costs one copy per element removed. It is copy-on-write's known counterpart, not
    // an oversight -- whoever uses this list writes little and reads a lot.

    public boolean containsAll(java.util.Collection<?> c) {
        java.util.Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            if (!this.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    public boolean addAll(java.util.Collection<? extends E> c) {
        boolean changed = false;
        java.util.Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            if (this.add(it.next())) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeAll(java.util.Collection<?> c) {
        boolean changed = false;
        Object[] snapshot = this.toArray();
        int i = 0;
        while (i < snapshot.length) {
            if (c.contains(snapshot[i])) {
                while (this.remove(snapshot[i])) {
                    changed = true;
                }
            }
            i = i + 1;
        }
        return changed;
    }

    public boolean retainAll(java.util.Collection<?> c) {
        boolean changed = false;
        Object[] snapshot = this.toArray();
        int i = 0;
        while (i < snapshot.length) {
            if (!c.contains(snapshot[i])) {
                while (this.remove(snapshot[i])) {
                    changed = true;
                }
            }
            i = i + 1;
        }
        return changed;
    }

    public Object[] toArray() {
        Object[] out = new Object[this.size()];
        int i = 0;
        java.util.Iterator<E> it = this.iterator();
        while (it.hasNext() && i < out.length) {
            out[i] = it.next();
            i = i + 1;
        }
        return out;
    }

    public <T> T[] toArray(T[] a) {
        int n = this.size();
        Object[] dest = a;
        if (a.length < n) {
            dest = (Object[]) Array.newInstance(a.getClass().getComponentType(), n);
        }
        int i = 0;
        java.util.Iterator<E> it = this.iterator();
        while (it.hasNext() && i < n) {
            dest[i] = it.next();
            i = i + 1;
        }
        if (dest.length > n) {
            dest[n] = null;
        }
        return (T[]) dest;
    }

    // ---- what List adds over Collection --------------------------------------------------------
    //
    // Written out and not inherited for the same reason as the bulk operations: in the JDK this
    // class extends no skeleton. `AbstractListLitr` and `SubList`, which are what `AbstractList`
    // uses, are package-private to `java.util` and cannot be seen from here.

    // The index of the LAST occurrence of `o`, or -1.
    public int lastIndexOf(Object o) {
        int i = this.size() - 1;
        while (i >= 0) {
            E e = this.get(i);
            if (o == null) {
                if (e == null) {
                    return i;
                }
            } else if (o.equals(e)) {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    // A bidirectional cursor from the start.
    public java.util.ListIterator<E> listIterator() {
        return new CowLitr<E>(this, 0);
    }

    // A bidirectional cursor from `index`.
    public java.util.ListIterator<E> listIterator(int index) {
        return new CowLitr<E>(this, index);
    }

    // A view of [fromIndex, toIndex).
    public java.util.List<E> subList(int fromIndex, int toIndex) {
        return new CowSubList<E>(this, fromIndex, toIndex);
    }

    // It inserts all of `c` from `index` on, in its iterator's order.
    public boolean addAll(int index, java.util.Collection<? extends E> c) {
        if (index < 0 || index > this.size()) {
            throw new IndexOutOfBoundsException();
        }
        boolean changed = false;
        int at = index;
        java.util.Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            this.add(at, it.next());
            at = at + 1;
            changed = true;
        }
        return changed;
    }
}

// The snapshot iterator: it holds the array the list had at creation, so later writes
// (which replace the array) are invisible to it and it can never see a torn update.
final class CowItr<E> implements Iterator<E> {

    private final Object[] snapshot;
    private int cursor;

    CowItr(Object[] snapshot) {
        this.snapshot = snapshot;
    }

    public boolean hasNext() {
        return cursor < snapshot.length;
    }

    public E next() {
        E e = (E) snapshot[cursor];
        cursor++;
        return e;
    }

}


// CopyOnWriteArrayList's ListIterator. A twin of `java.util.AbstractListLitr`, which cannot be seen
// from this package.
//
// The cursor sits between elements; `last` remembers which one the last call returned, because
// `set` and `remove` act on that one and not on the gap.
final class CowLitr<E> implements java.util.ListIterator<E> {

    private final java.util.List<E> list;
    private int cursor;
    private int last;

    CowLitr(java.util.List<E> list, int index) {
        if (index < 0 || index > list.size()) {
            throw new IndexOutOfBoundsException();
        }
        this.list = list;
        this.cursor = index;
        this.last = -1;
    }

    public boolean hasNext() {
        return this.cursor < this.list.size();
    }

    public E next() {
        if (this.cursor >= this.list.size()) {
            throw new java.util.NoSuchElementException();
        }
        E e = this.list.get(this.cursor);
        this.last = this.cursor;
        this.cursor = this.cursor + 1;
        return e;
    }

    public boolean hasPrevious() {
        return this.cursor > 0;
    }

    public E previous() {
        if (this.cursor <= 0) {
            throw new java.util.NoSuchElementException();
        }
        this.cursor = this.cursor - 1;
        this.last = this.cursor;
        return this.list.get(this.cursor);
    }

    public int nextIndex() {
        return this.cursor;
    }

    public int previousIndex() {
        return this.cursor - 1;
    }

    public void remove() {
        if (this.last < 0) {
            throw new IllegalStateException();
        }
        this.list.remove(this.last);
        if (this.last < this.cursor) {
            this.cursor = this.cursor - 1;
        }
        this.last = -1;
    }

    public void set(E e) {
        if (this.last < 0) {
            throw new IllegalStateException();
        }
        this.list.set(this.last, e);
    }

    public void add(E e) {
        this.list.add(this.cursor, e);
        this.cursor = this.cursor + 1;
        this.last = -1;
    }
}

// The view CopyOnWriteArrayList.subList returns. It hangs off `java.util.AbstractList`, which IS
// public, and inherits iterator/listIterator/subList/lastIndexOf and the bulk operations from there.
// A view and not a copy: writing into it writes into the list behind.
final class CowSubList<E> extends java.util.AbstractList<E> {

    private final java.util.List<E> base;
    private final int offset;
    private int length;

    CowSubList(java.util.List<E> base, int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > base.size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }
        this.base = base;
        this.offset = fromIndex;
        this.length = toIndex - fromIndex;
    }

    public E get(int index) {
        if (index < 0 || index >= this.length) {
            throw new IndexOutOfBoundsException();
        }
        return this.base.get(this.offset + index);
    }

    public int size() {
        return this.length;
    }

    public E set(int index, E element) {
        if (index < 0 || index >= this.length) {
            throw new IndexOutOfBoundsException();
        }
        return this.base.set(this.offset + index, element);
    }

    public void add(int index, E element) {
        if (index < 0 || index > this.length) {
            throw new IndexOutOfBoundsException();
        }
        this.base.add(this.offset + index, element);
        this.length = this.length + 1;
    }

    public E remove(int index) {
        if (index < 0 || index >= this.length) {
            throw new IndexOutOfBoundsException();
        }
        E old = this.base.remove(this.offset + index);
        this.length = this.length - 1;
        return old;
    }

    public boolean add(E e) {
        this.add(this.length, e);
        return true;
    }
}
