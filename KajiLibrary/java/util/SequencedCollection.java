package java.util;

// A collection with a defined encounter order, and therefore two ends (Java 21). It is what
// List, Deque and LinkedHashSet had in common all along but could not express: the ability to
// ask for the first and last element, and to walk the whole thing backwards via reversed().
//
// Only addFirst/addLast refuse by default: where an element goes is the implementation's business,
// and a sorted or unmodifiable collection has no answer. The other four are written on iterator()
// and reversed(), which every implementation has to provide anyway, so they come out right without
// anyone writing them -- an unmodifiable collection still refuses removeFirst, because it is its
// iterator's remove() that refuses.
//
// Reading them off the iterator is also what gets the empty case right: getFirst has to throw
// NoSuchElementException, and next() on an exhausted iterator throws exactly that.
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
        return this.iterator().next();
    }

    default E getLast() {
        return this.reversed().iterator().next();
    }

    default E removeFirst() {
        Iterator<E> it = this.iterator();
        E e = it.next();
        it.remove();
        return e;
    }

    default E removeLast() {
        Iterator<E> it = this.reversed().iterator();
        E e = it.next();
        it.remove();
        return e;
    }
}
