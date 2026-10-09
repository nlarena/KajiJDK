package java.util;

// The view List.subList(from, to) returns. Package-private: the contract only promises a List back.
//
// It is a **view**, not a copy, and that is the whole point: writing into the sublist writes into the
// original, and `list.subList(a, b).clear()` is the idiomatic way of removing a range. A copy would
// make that line remove nothing, in silence.
//
// Its own size (`length`) is adjusted on each insertion or removal through the view. What is **not**
// detected is a modification made directly on the list behind while the view exists: the JDK catches
// that with `modCount` and throws ConcurrentModificationException. Here it does not, and it is said
// plainly: using the view after touching the original from outside gives meaningless results instead
// of an exception.
final class SubList<E> extends AbstractList<E> {

    private final List<E> base;
    private final int offset;
    private int length;

    SubList(List<E> base, int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > base.size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }
        this.base = base;
        this.offset = fromIndex;
        this.length = toIndex - fromIndex;
    }

    private void checkIndex(int index, int bound) {
        if (index < 0 || index >= bound) {
            throw new IndexOutOfBoundsException();
        }
    }

    public E get(int index) {
        this.checkIndex(index, this.length);
        return this.base.get(this.offset + index);
    }

    public int size() {
        return this.length;
    }

    public E set(int index, E element) {
        this.checkIndex(index, this.length);
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
        this.checkIndex(index, this.length);
        E old = this.base.remove(this.offset + index);
        this.length = this.length - 1;
        return old;
    }

    public boolean add(E e) {
        this.add(this.length, e);
        return true;
    }

    public void clear() {
        int i = this.length;
        while (i > 0) {
            this.base.remove(this.offset + i - 1);
            i = i - 1;
        }
        this.length = 0;
    }
}
