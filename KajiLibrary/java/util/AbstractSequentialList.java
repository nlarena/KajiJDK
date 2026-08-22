package java.util;

// The skeleton for lists backed by links rather than an index: the subclass provides
// `listIterator(int)`, and indexed access is derived by walking to the position. It is the
// mirror image of {@link AbstractList}, which derives iteration from indexing — which skeleton
// to extend is a statement about which operation the structure is actually good at.
public abstract class AbstractSequentialList<E> extends AbstractList<E> {

    protected AbstractSequentialList() {
    }

    // The one method a subclass must provide: a cursor positioned at `index`.
    public abstract ListIterator<E> listIterator(int index);

    public E get(int index) {
        ListIterator<E> it = listIterator(index);
        if (!it.hasNext()) {
            throw new IndexOutOfBoundsException();
        }
        return it.next();
    }

    public E set(int index, E element) {
        ListIterator<E> it = listIterator(index);
        if (!it.hasNext()) {
            throw new IndexOutOfBoundsException();
        }
        E old = it.next();
        it.set(element);
        return old;
    }

    public void add(int index, E element) {
        ListIterator<E> it = listIterator(index);
        it.add(element);
    }

    public E remove(int index) {
        ListIterator<E> it = listIterator(index);
        if (!it.hasNext()) {
            throw new IndexOutOfBoundsException();
        }
        E old = it.next();
        it.remove();
        return old;
    }

    public Iterator<E> iterator() {
        return listIterator(0);
    }
}
