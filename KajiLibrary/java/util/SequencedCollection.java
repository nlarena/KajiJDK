package java.util;

// A collection with a defined encounter order, and therefore two ends (Java 21). It is what
// List, Deque and LinkedHashSet had in common all along but could not express: the ability to
// ask for the first and last element, and to walk the whole thing backwards via reversed().
//
// The end operations come as defaults that refuse, so an immutable or unmodifiable
// implementation inherits the right behaviour without writing anything.
public interface SequencedCollection<E> extends Collection<E> {

    // A reverse-ordered *view* — not a copy; writes through it affect this collection.
    SequencedCollection<E> reversed();

    default void addFirst(E e) {
        throw new UnsupportedOperationException();
    }

    default void addLast(E e) {
        throw new UnsupportedOperationException();
    }

    default E getFirst() {
        throw new UnsupportedOperationException();
    }

    default E getLast() {
        throw new UnsupportedOperationException();
    }

    default E removeFirst() {
        throw new UnsupportedOperationException();
    }

    default E removeLast() {
        throw new UnsupportedOperationException();
    }
}
