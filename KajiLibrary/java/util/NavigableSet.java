package java.util;

// A sorted set that can also answer **approximate** queries: not just "is this element here?"
// but "what is the closest one below it?" — which is what turns an ordered set into an index.
// lower/floor/ceiling/higher are the four combinations of direction and whether an exact match
// counts.
public interface NavigableSet<E> extends SortedSet<E> {

    // The greatest element strictly less than `e`, or null.
    E lower(E e);

    // The greatest element less than **or equal to** `e`, or null.
    E floor(E e);

    // The least element greater than or equal to `e`, or null.
    E ceiling(E e);

    // The least element strictly greater than `e`, or null.
    E higher(E e);

    // Remove and return the first (resp. last) element, or null if empty.
    E pollFirst();

    E pollLast();

    Iterator<E> iterator();

    // A reverse-ordered view of this set.
    NavigableSet<E> descendingSet();

    Iterator<E> descendingIterator();

    // Range views, with each endpoint independently inclusive or exclusive.
    NavigableSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive);

    NavigableSet<E> headSet(E to, boolean inclusive);

    /**
     * La vista dada vuelta, que en un conjunto navegable **es** `descendingSet()`.
     *
     * <p>No es una simplificacion: son el mismo objeto en el JDK tambien. `reversed()` llego con
     * `SequencedCollection` en Java 21 y `descendingSet()` estaba desde el 6 -- la unica diferencia
     * es que el nombre nuevo vale para cualquier coleccion con orden y el viejo solo aca.
     */
    default NavigableSet<E> reversed() {
        return this.descendingSet();
    }

    NavigableSet<E> tailSet(E from, boolean inclusive);
}
