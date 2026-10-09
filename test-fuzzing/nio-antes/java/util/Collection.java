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
    // This note used to say that route was closed because `java.util.Spliterator` does not exist and
    // `StreamSupport` therefore could not be used: both exist, and `spliterator()` is declared right
    // below. What is left is that this library's streams are eager, so the lazy route would buy
    // nothing -- this walks the iterator into an array and hands it to `Stream.of`, which yields the
    // same observable thing. The difference is laziness, not results: the JDK's stream pulls from the
    // source on demand, this one snapshots first.
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
     *  <p>Over the iterator, with the size the collection knows. With no characteristics of its own:
     * a `Collection` promises no order, no uniqueness, nothing.
     *
     */
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, 0);
    }

    // ---- the bulk operations -----------------------------------------------------------------
    //
    // Abstract, as in the JDK. `AbstractCollection` derives them from `iterator()`, `size()`,
    // `contains()`, `add()` and `remove()`, so an implementation that inherits from the skeleton
    // writes none of them.

    // Whether all of `c`'s elements are in this collection.
    boolean containsAll(Collection<?> c);

    // It adds all of `c`'s; it returns whether this collection changed.
    boolean addAll(Collection<? extends E> c);

    // It removes every occurrence of each element of `c`.
    boolean removeAll(Collection<?> c);

    // It keeps only the elements that are also in `c`.
    boolean retainAll(Collection<?> c);

    // The elements in a fresh array, in the iterator's order.
    Object[] toArray();

    // The elements in `a` if they fit, or in a fresh array of the same runtime type if not.
    <T> T[] toArray(T[] a);

    // The elements in an array `generator` makes at exactly the right size.
    //
    // It exists so `c.toArray(String[]::new)` can be written instead of `c.toArray(new String[0])`,
    // which is the same idea said without the empty array in the way.
    default <T> T[] toArray(java.util.function.IntFunction<T[]> generator) {
        return this.toArray(generator.apply(0));
    }

    // It removes the elements that satisfy `filter`; it returns whether anything changed.
    default boolean removeIf(java.util.function.Predicate<? super E> filter) {
        boolean changed = false;
        Object[] snapshot = this.toArray();
        int i = 0;
        while (i < snapshot.length) {
            if (filter.test((E) snapshot[i])) {
                while (this.remove(snapshot[i])) {
                    changed = true;
                }
            }
            i = i + 1;
        }
        return changed;
    }

    // A possibly parallel stream over these elements.
    //
    // **A deliberate divergence**: here it returns the same sequential stream as `stream()`. The
    // library does not have the parallel splitting engine yet, and a method that says "parallel" and
    // runs sequentially is preferable to one that does not exist: the result is the same, only
    // without the gain.
    default java.util.stream.Stream<E> parallelStream() {
        return this.stream();
    }

}
