package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.Collection;

// KajiLibrary's java.util.Set<E> — a Collection with no duplicate elements. It adds no new
// operations over Collection; the difference is the uniqueness contract (add returns false
// when the element is already present). Concrete: HashSet.
public interface Set<E> extends Collection<E> {
    /**
     * A spliterator over these elements.
     *
     *  <p>DISTINCT, que es lo que hace a un conjunto un conjunto. Un consumidor que deduplica puede
     * no hacer nada al ver esta caracteristica, y ese ahorro es todo el punto.
     *
     */
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT);
    }

    // ---- las factorias inmutables (JDK 9+) --------------------------------------------------
    //
    // Devuelven un conjunto **inmutable** que rechaza nulos y **elementos repetidos**. Lo segundo
    // es del contrato, no una eleccion nuestra: `Set.of("a", "a")` es IllegalArgumentException en
    // el JDK, no un conjunto de un elemento. Un repetido en un literal es un bug del literal, y
    // tragarselo lo esconde. `copyOf`, en cambio, si descarta repetidos: copiar de una coleccion
    // que los tiene es normal.

    static <E> Set<E> of() {
        return FixedSet.fromArray(new Object[0], 0);
    }

    static <E> Set<E> of(E e1) {
        Object[] a = new Object[1];
        a[0] = e1;
        return FixedSet.fromArray(a, 1);
    }

    static <E> Set<E> of(E e1, E e2) {
        Object[] a = new Object[2];
        a[0] = e1;
        a[1] = e2;
        return FixedSet.fromArray(a, 2);
    }

    static <E> Set<E> of(E e1, E e2, E e3) {
        Object[] a = new Object[3];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        return FixedSet.fromArray(a, 3);
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4) {
        Object[] a = new Object[4];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        return FixedSet.fromArray(a, 4);
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
        Object[] a = new Object[5];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        return FixedSet.fromArray(a, 5);
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        Object[] a = new Object[6];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        a[5] = e6;
        return FixedSet.fromArray(a, 6);
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        Object[] a = new Object[7];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        a[5] = e6;
        a[6] = e7;
        return FixedSet.fromArray(a, 7);
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        Object[] a = new Object[8];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        a[5] = e6;
        a[6] = e7;
        a[7] = e8;
        return FixedSet.fromArray(a, 8);
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        Object[] a = new Object[9];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        a[5] = e6;
        a[6] = e7;
        a[7] = e8;
        a[8] = e9;
        return FixedSet.fromArray(a, 9);
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        Object[] a = new Object[10];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        a[5] = e6;
        a[6] = e7;
        a[7] = e8;
        a[8] = e9;
        a[9] = e10;
        return FixedSet.fromArray(a, 10);
    }

    // El conjunto de los elementos dados.
    static <E> Set<E> of(E... elements) {
        return FixedSet.fromArray(elements, elements.length);
    }

    // Una copia inmutable de `coll`, con los repetidos descartados.
    static <E> Set<E> copyOf(Collection<? extends E> coll) {
        Object[] a = new Object[coll.size()];
        int i = 0;
        Iterator<? extends E> it = coll.iterator();
        while (it.hasNext()) {
            a[i] = it.next();
            i = i + 1;
        }
        return FixedSet.dedup(a, i);
    }


}
