package java.util;

// An immutable List over a fixed array. Package-private: an internal, used for the constant
// format lists ResourceBundle.Control publishes.
//
// It exists because Collections.unmodifiableList and List.of are not implemented yet, and those
// constants must be genuinely immutable rather than immutable by convention — a caller that can
// mutate FORMAT_DEFAULT changes bundle lookup for every other caller in the process. Every
// mutator here throws UnsupportedOperationException, which is what the JDK's own unmodifiable
// wrappers do.
final class FixedList<E> extends AbstractList<E> implements List<E> {

    // The backing array. Never handed out and never written after construction.
    private final Object[] items;

    // Wraps `items`. The caller must not retain the array; every construction site here builds a
    // fresh one and drops it.
    FixedList(Object[] items) {
        this.items = items;
    }

    public E get(int index) {
        if (index < 0 || index >= this.items.length) {
            throw new IndexOutOfBoundsException();
        }
        return (E) this.items[index];
    }

    public int size() {
        return this.items.length;
    }

    public boolean isEmpty() {
        return this.items.length == 0;
    }

    public int indexOf(Object o) {
        int i = 0;
        while (i < this.items.length) {
            if (o == null) {
                if (this.items[i] == null) {
                    return i;
                }
            } else if (o.equals(this.items[i])) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    public boolean contains(Object o) {
        return this.indexOf(o) >= 0;
    }

    public Iterator<E> iterator() {
        return new FixedListItr<E>(this.items);
    }

    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        String s = "[";
        int i = 0;
        while (i < this.items.length) {
            if (i > 0) {
                s = s + ", ";
            }
            s = s + this.items[i];
            i = i + 1;
        }
        return s + "]";
    }
}

// The iterator over a FixedList. Read-only, like the list.
final class FixedListItr<E> implements Iterator<E> {

    private final Object[] items;
    private int cursor;

    FixedListItr(Object[] items) {
        this.items = items;
        this.cursor = 0;
    }

    public boolean hasNext() {
        return this.cursor < this.items.length;
    }

    public E next() {
        if (this.cursor >= this.items.length) {
            throw new NoSuchElementException();
        }
        E e = (E) this.items[this.cursor];
        this.cursor = this.cursor + 1;
        return e;
    }
}
