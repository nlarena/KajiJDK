package java.util;

import java.util.stream.Stream;

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

    // A sequential stream over these elements, in encounter order (finding #205).
    //
    // The JDK's is `default Stream<E> stream() { return StreamSupport.stream(spliterator(), false); }`.
    // That route is closed here: `java.util.Spliterator` doesn't exist, which is also why
    // `StreamSupport` was ruled out — its five public methods all take one. So this walks the
    // iterator into an array and hands it to `Stream.of`, which yields the same observable thing.
    // The difference is laziness, not results: the JDK's stream pulls from the source on demand,
    // this one snapshots first.
    //
    // `default` and not abstract, exactly like the JDK: `Collection` has 15 implementors here, and
    // none of them should have to write this.
    default Stream<E> stream() {
        Object[] items = new Object[size()];
        int i = 0;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            items[i] = it.next();
            i = i + 1;
        }
        // `Stream.of` is varargs; passing the array explicitly is the form that compiles today
        // (a *spread* call to a classpath varargs is still dropped — finding #118/#200).
        return (Stream<E>) Stream.of(items);
    }
    /**
     * A spliterator over these elements.
     *
     *  <p>Sobre el iterador, con el tamano que la coleccion sabe. Sin caracteristicas propias: una
     * `Collection` no promete orden, ni unicidad, ni nada.
     *
     */
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, 0);
    }

}
