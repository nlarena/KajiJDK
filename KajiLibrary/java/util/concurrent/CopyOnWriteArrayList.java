package java.util.concurrent;

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
    // **boolean-valued** ternary (`o == null ? e == null : o.equals(e)`) is rejected by our
    // javac with "operando no numérico" — finding #109. Int- and reference-valued ternaries
    // are fine, so only this shape needs the rewrite.
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

    public void clear() {
        synchronized (sync) {
            elements = new Object[0];
        }
    }

    // Walks the array as it was when the iterator was created.
    public Iterator<E> iterator() {
        return new CowItr<E>(elements);
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
