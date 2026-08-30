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

    // ---- las operaciones en bloque -----------------------------------------------------------
    //
    // Abstractas, como en el JDK. `AbstractCollection` las deriva a partir de `iterator()`,
    // `size()`, `contains()`, `add()` y `remove()`, asi que una implementacion que herede del
    // esqueleto no escribe ninguna.

    // Si todos los elementos de `c` estan en esta coleccion.
    boolean containsAll(Collection<?> c);

    // Agrega todos los de `c`; devuelve si esta coleccion cambio.
    boolean addAll(Collection<? extends E> c);

    // Quita todas las apariciones de cada elemento de `c`.
    boolean removeAll(Collection<?> c);

    // Deja solo los elementos que tambien estan en `c`.
    boolean retainAll(Collection<?> c);

    // Los elementos en un arreglo nuevo, en el orden del iterador.
    Object[] toArray();

    // Los elementos en `a` si entran, o en un arreglo nuevo del mismo tipo dinamico si no.
    <T> T[] toArray(T[] a);

    // Los elementos en un arreglo que fabrica `generator` con el tamano justo.
    //
    // Existe para poder escribir `c.toArray(String[]::new)` en vez de `c.toArray(new String[0])`,
    // que es la misma idea dicha sin el arreglo vacio de por medio.
    default <T> T[] toArray(java.util.function.IntFunction<T[]> generator) {
        return this.toArray(generator.apply(0));
    }

    // Quita los elementos que cumplan `filter`; devuelve si algo cambio.
    default boolean removeIf(java.util.function.Predicate<? super E> filter) {
        boolean cambio = false;
        Object[] foto = this.toArray();
        int i = 0;
        while (i < foto.length) {
            if (filter.test((E) foto[i])) {
                while (this.remove(foto[i])) {
                    cambio = true;
                }
            }
            i = i + 1;
        }
        return cambio;
    }

    // Un stream posiblemente paralelo sobre estos elementos.
    //
    // **Divergencia deliberada**: aca devuelve el mismo stream secuencial que `stream()`. La
    // biblioteca no tiene todavia el motor de division en paralelo, y un metodo que dijera
    // "paralelo" y corriera secuencial es preferible a uno que no exista: el resultado es el
    // mismo, solo que sin la ganancia.
    default java.util.stream.Stream<E> parallelStream() {
        return this.stream();
    }

}
