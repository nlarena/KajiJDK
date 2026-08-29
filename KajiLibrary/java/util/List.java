package java.util;

// Same-package import is a workaround for the frozen javac's finder, which does not
// auto-load an unqualified same-package type that lives only on the classpath (finding #4).
import java.util.Collection;
import java.util.SequencedCollection;
import java.util.NoSuchElementException;

// KajiLibrary's java.util.List<E> — an ordered Collection addressable by integer index:
// get/set/insert/remove at a position, and search by value. A KajiLibrary subset (the JDK
// adds listIterator/subList/replaceAll/sort/…).
//
// It is a SequencedCollection (Java 21) because an index *is* an encounter order: the two ends
// and the reverse view are not new capability, only names for what get(int)/size() already
// allowed. That is why every sequenced member below is a working `default` — the ones
// SequencedCollection inherits refuse, and a list has no reason to.
public interface List<E> extends Collection<E>, SequencedCollection<E> {

    E get(int index);

    E set(int index, E element);

    void add(int index, E element);

    E remove(int index);

    int indexOf(Object o);
    /**
     * A spliterator over these elements.
     *
     *  <p>ORDERED, que es lo unico que una lista promete y una coleccion no: el recorrido repite el
     * orden de la lista.
     *
     */
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.ORDERED);
    }

    // --- the sequenced half ---

    // Narrowed to List<E>: reversing a list gives back something still addressable by index, and
    // callers should not have to cast to say so. The JDK narrows here for the same reason.
    //
    // A view over this list, not a copy — writes through it land here.
    default List<E> reversed() {
        return new ReverseOrderListView<E>(this);
    }

    // Empty is NoSuchElementException, not UnsupportedOperationException: asking for an end that
    // does not exist is a different failure from a list that refuses ends at all.
    default E getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(0);
    }

    default E getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(size() - 1);
    }

    default void addFirst(E e) {
        add(0, e);
    }

    default void addLast(E e) {
        add(size(), e);
    }

    default E removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return remove(0);
    }

    default E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return remove(size() - 1);
    }

}
