package java.util;

// KajiLibrary's java.util.Collection<E> — the root of the collection hierarchy: a group
// of elements you can size, test for membership, add to, remove from, empty, and iterate
// (the last inherited from Iterable). Concrete collections (List, Set, Queue) refine it.
// A KajiLibrary subset: the JDK's Collection also has addAll/removeAll/toArray/stream/…
public interface Collection<E> extends Iterable<E> {

    int size();

    boolean isEmpty();

    boolean contains(Object o);

    boolean add(E e);

    boolean remove(Object o);

    void clear();
}
