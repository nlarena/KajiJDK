package java.util;

// The skeleton for lists backed by an index: give it `get(int)` and `size()` and it derives
// iteration and search. Its counterpart {@link AbstractSequentialList} does the same for lists
// backed by links, deriving indexed access from an iterator instead — the two skeletons exist
// precisely because a list can be efficient at one or the other, rarely both.
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {

    protected AbstractList() {
    }

    public abstract E get(int index);

    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    // Appending is inserting at the end — so a subclass that implements add(int, E) gets this.
    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    public int indexOf(Object o) {
        int found = -1;
        int n = size();
        for (int i = 0; i < n; i++) {
            if (found < 0) {
                Object e = get(i);
                if (o == null) {
                    if (e == null) {
                        found = i;
                    }
                } else if (o.equals(e)) {
                    found = i;
                }
            }
        }
        return found;
    }

    // Walks by index, which is exactly what "backed by an index" buys.
    public Iterator<E> iterator() {
        return new AbstractListItr<E>(this);
    }
}

// The index-walking iterator every AbstractList hands out.
final class AbstractListItr<E> implements Iterator<E> {

    private final AbstractList<E> list;
    private int cursor;

    AbstractListItr(AbstractList<E> list) {
        this.list = list;
    }

    public boolean hasNext() {
        return cursor < list.size();
    }

    public E next() {
        if (cursor >= list.size()) {
            throw new NoSuchElementException();
        }
        E e = list.get(cursor);
        cursor++;
        return e;
    }
}
