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

    // ---- los `default` del JDK 8+ ----------------------------------------------------------

    // Reemplaza cada elemento por el que devuelva `operator`.
    default void replaceAll(java.util.function.UnaryOperator<E> operator) {
        int i = 0;
        while (i < this.size()) {
            this.set(i, operator.apply(this.get(i)));
            i = i + 1;
        }
    }

    // Ordena la lista con `c`, o por orden natural si `c` es null.
    //
    // Insercion sobre `get`/`set`: el JDK vuelca a un arreglo, llama a `Arrays.sort` y reescribe.
    // Aca `Arrays.sort(T[], Comparator)` todavia no existe, y el cuerpo de un `default` es interno
    // — lo observable es que quede ordenada y que la ordenacion sea **estable**, que la insercion
    // cumple porque solo mueve un elemento cuando el de la izquierda es estrictamente mayor.
    default void sort(Comparator<? super E> c) {
        int i = 1;
        while (i < this.size()) {
            E actual = this.get(i);
            int j = i - 1;
            while (j >= 0 && mayor(this.get(j), actual, c)) {
                this.set(j + 1, this.get(j));
                j = j - 1;
            }
            this.set(j + 1, actual);
            i = i + 1;
        }
    }

    // `a > b` segun `c`, o segun el orden natural de `a` si `c` es null.
    private static <E> boolean mayor(E a, E b, Comparator<? super E> c) {
        if (c == null) {
            return ((Comparable<E>) a).compareTo(b) > 0;
        }
        return c.compare(a, b) > 0;
    }

    // ---- las factorias inmutables (JDK 9+) --------------------------------------------------
    //
    // Devuelven una lista **inmutable** que rechaza elementos nulos. A diferencia de `Set.of`, los
    // repetidos si se aceptan: una lista es una secuencia, y repetir es parte de lo que es.

    static <E> List<E> of() {
        return new FixedList<E>(new Object[0]);
    }

    static <E> List<E> of(E e1) {
        Object[] a = new Object[1];
        a[0] = e1;
        return new FixedList<E>(a);
    }

    static <E> List<E> of(E e1, E e2) {
        Object[] a = new Object[2];
        a[0] = e1;
        a[1] = e2;
        return new FixedList<E>(a);
    }

    static <E> List<E> of(E e1, E e2, E e3) {
        Object[] a = new Object[3];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        return new FixedList<E>(a);
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4) {
        Object[] a = new Object[4];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        return new FixedList<E>(a);
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        Object[] a = new Object[5];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        return new FixedList<E>(a);
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        Object[] a = new Object[6];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        a[5] = e6;
        return new FixedList<E>(a);
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        Object[] a = new Object[7];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        a[5] = e6;
        a[6] = e7;
        return new FixedList<E>(a);
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        Object[] a = new Object[8];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        a[5] = e6;
        a[6] = e7;
        a[7] = e8;
        return new FixedList<E>(a);
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
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
        return new FixedList<E>(a);
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
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
        return new FixedList<E>(a);
    }

    // La lista de los elementos dados.
    static <E> List<E> of(E... elements) {
        Object[] a = new Object[elements.length];
        int i = 0;
        while (i < elements.length) {
            if (elements[i] == null) {
                throw new NullPointerException();
            }
            a[i] = elements[i];
            i = i + 1;
        }
        return new FixedList<E>(a);
    }

    // Una copia inmutable de `coll`, sacada en el momento: cambios posteriores no se ven.
    static <E> List<E> copyOf(Collection<? extends E> coll) {
        Object[] a = new Object[coll.size()];
        int i = 0;
        Iterator<? extends E> it = coll.iterator();
        while (it.hasNext()) {
            E e = it.next();
            if (e == null) {
                throw new NullPointerException();
            }
            a[i] = e;
            i = i + 1;
        }
        return new FixedList<E>(a);
    }


    // ---- lo que List agrega sobre Collection -------------------------------------------------

    // El indice de la ULTIMA aparicion de `o`, o -1.
    int lastIndexOf(Object o);

    // Un cursor bidireccional sobre esta lista, desde el principio.
    ListIterator<E> listIterator();

    // Un cursor bidireccional sobre esta lista, desde `index`.
    ListIterator<E> listIterator(int index);

    // Una vista de la porcion [fromIndex, toIndex).
    List<E> subList(int fromIndex, int toIndex);

    // Inserta todos los de `c` a partir de `index`.
    boolean addAll(int index, Collection<? extends E> c);

}
