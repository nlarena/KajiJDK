package java.util;

// A set whose iteration follows the ordering of its elements — their natural order, or a
// Comparator given at construction. Being ordered is what makes the range views meaningful:
// headSet/tailSet/subSet are the slices of the set, and they are *views*, so they stay in step
// with the set they came from.
public interface SortedSet<E> extends Set<E>, SequencedSet<E> {

    // The ordering, or null when the elements order themselves (Comparable).
    Comparator<? super E> comparator();

    // The elements in [from, to) — a view, not a copy.
    SortedSet<E> subSet(E from, E to);

    // The elements strictly before `to`.
    SortedSet<E> headSet(E to);

    // The elements from `from` on.
    SortedSet<E> tailSet(E from);

    E first();

    E last();
}
