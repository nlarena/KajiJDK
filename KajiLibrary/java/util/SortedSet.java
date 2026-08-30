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

    /**
     * Una **vista** de este conjunto en orden inverso.
     *
     * <p>Vista y no copia: lo que se agregue de un lado se ve del otro. El recorrido hacia atras lo
     * arma `ReverseSortedSet` con `last()` y `headSet()`, sin materializar nada -- ver la nota larga
     * de `ReverseViews.java`.
     */
    default SortedSet<E> reversed() {
        return new ReverseSortedSet<E>(this);
    }
    /**
     * A spliterator over these elements, reporting the sort order.
     *
     * <p>The only one in the library that reports {@code SORTED}, and it has to report the
     * comparator with it: a consumer that knows the order can skip sorting, and it cannot know
     * WHICH order without asking.
     */
    default Spliterator<E> spliterator() {
        final Comparator<? super E> order = this.comparator();
        return new Spliterators.IteratorSpliterator<E>(this,
                Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED) {
            public Comparator<? super E> getComparator() {
                return order;
            }
        };
    }

}
