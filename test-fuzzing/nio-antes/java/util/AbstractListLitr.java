package java.util;

// The ListIterator every AbstractList returns. Package-private, like AbstractListItr.
//
// Until now `ListIterator` was an interface **declared with no implementor at all** in the whole
// library: the type existed and there was nothing to return, so `listIterator()` could not be written
// in any list. This is that implementation.
//
// The cursor sits **between** elements, which is what tells a ListIterator from an Iterator:
// `nextIndex()` is the gap it is standing in, `previous()` steps back over what has been walked, and
// `add` inserts into that gap. `last` remembers the index of the element the last call to
// next()/previous() returned, because `set` and `remove` operate on **that** one, not on the
// cursor.
final class AbstractListLitr<E> implements ListIterator<E> {

    private final List<E> list;

    // The gap it is standing in: 0 is before the first, size() is after the last.
    private int cursor;

    // The index of the last element returned, or -1 if there was no next()/previous() since the last
    // modification. It is what lets `set` and `remove` know what to operate on.
    private int last;

    AbstractListLitr(List<E> list, int index) {
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
            throw new NoSuchElementException();
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
            throw new NoSuchElementException();
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

    // It removes the last one returned. If it came from next(), the cursor steps back one: what was
    // ahead has moved one place back and must not be skipped.
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

    // It replaces the last one returned. It moves neither the cursor nor invalidates `last`:
    // changing a position's value does not change where the walk is.
    public void set(E e) {
        if (this.last < 0) {
            throw new IllegalStateException();
        }
        this.list.set(this.last, e);
    }

    // It inserts into the current gap. The new one is left **behind** the cursor, so `next()` goes on
    // returning what it was going to return; and `last` is invalidated, because after an add there
    // is no "last returned" worth operating on.
    public void add(E e) {
        this.list.add(this.cursor, e);
        this.cursor = this.cursor + 1;
        this.last = -1;
    }
}
