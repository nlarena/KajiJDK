package java.util;

// An iterator for lists, which can walk **both** ways and modify as it goes: the cursor sits
// *between* elements, so `nextIndex`/`previousIndex` describe the gap and `add` inserts there.
// That is what lets a caller edit a list while traversing it, which a plain Iterator cannot.
public interface ListIterator<E> extends Iterator<E> {

    boolean hasNext();

    E next();

    boolean hasPrevious();

    E previous();

    // The index of the element next() would return, or the list size at the end.
    int nextIndex();

    // The index of the element previous() would return, or -1 at the start.
    int previousIndex();

    void remove();

    // Replace the element last returned by next() or previous().
    void set(E e);

    // Insert before the element next() would return.
    void add(E e);
}
