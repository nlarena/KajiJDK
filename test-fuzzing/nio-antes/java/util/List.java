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
     *  <p>ORDERED, which is the one thing a list promises and a collection does not: the traversal
     * repeats the list's order.
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

    // ---- the JDK 8+ `default`s    ----------------------------------------------------------

    // It replaces each element with the one `operator` returns.
    default void replaceAll(java.util.function.UnaryOperator<E> operator) {
        int i = 0;
        while (i < this.size()) {
            this.set(i, operator.apply(this.get(i)));
            i = i + 1;
        }
    }

    // It sorts the list with `c`, or by natural order if `c` is null.
    //
    // Insertion sort over `get`/`set`: the JDK dumps into an array, calls `Arrays.sort` and writes
    // back. Here `Arrays.sort(T[], Comparator)` does not exist yet, and a `default`'s body is
    // internal — what is observable is that it ends up sorted and that the sort is **stable**, which
    // insertion satisfies because it only moves an element when the one on its left is strictly
    // greater.
    default void sort(Comparator<? super E> c) {
        int i = 1;
        while (i < this.size()) {
            E current = this.get(i);
            int j = i - 1;
            while (j >= 0 && greaterThan(this.get(j), current, c)) {
                this.set(j + 1, this.get(j));
                j = j - 1;
            }
            this.set(j + 1, current);
            i = i + 1;
        }
    }

    // `a > b` by `c`, or by `a`'s natural order if `c` is null.
    private static <E> boolean greaterThan(E a, E b, Comparator<? super E> c) {
        if (c == null) {
            return ((Comparable<E>) a).compareTo(b) > 0;
        }
        return c.compare(a, b) > 0;
    }

    // ---- the immutable factories (JDK 9+) ---------------------------------------------------
    //
    // They return an **immutable** list that rejects null elements. Unlike `Set.of`, repeats are
    // accepted: a list is a sequence, and repeating is part of what that is.

    static <E> List<E> of() {
        return new FixedList<E>(new Object[0]);
    }

    static <E> List<E> of(E e1) {
        Object[] a = new Object[1];
        a[0] = e1;
        return new FixedList<E>(FixedList.nonNull(a));
    }

    static <E> List<E> of(E e1, E e2) {
        Object[] a = new Object[2];
        a[0] = e1;
        a[1] = e2;
        return new FixedList<E>(FixedList.nonNull(a));
    }

    static <E> List<E> of(E e1, E e2, E e3) {
        Object[] a = new Object[3];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        return new FixedList<E>(FixedList.nonNull(a));
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4) {
        Object[] a = new Object[4];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        return new FixedList<E>(FixedList.nonNull(a));
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        Object[] a = new Object[5];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        return new FixedList<E>(FixedList.nonNull(a));
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        Object[] a = new Object[6];
        a[0] = e1;
        a[1] = e2;
        a[2] = e3;
        a[3] = e4;
        a[4] = e5;
        a[5] = e6;
        return new FixedList<E>(FixedList.nonNull(a));
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
        return new FixedList<E>(FixedList.nonNull(a));
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
        return new FixedList<E>(FixedList.nonNull(a));
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
        return new FixedList<E>(FixedList.nonNull(a));
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
        return new FixedList<E>(FixedList.nonNull(a));
    }

    // The list of the given elements.
    static <E> List<E> of(E... elements) {
        Object[] a = new Object[elements.length];
        int i = 0;
        while (i < elements.length) {
            a[i] = elements[i];
            i = i + 1;
        }
        return new FixedList<E>(FixedList.nonNull(a));
    }

    // An immutable copy of `coll`, taken at the moment: later changes are not seen.
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
        return new FixedList<E>(FixedList.nonNull(a));
    }


    // ---- what List adds over Collection      -------------------------------------------------

    // The index of the LAST occurrence of `o`, or -1.
    int lastIndexOf(Object o);

    // A two-way cursor over this list, from the start.
    ListIterator<E> listIterator();

    // A two-way cursor over this list, from `index`.
    ListIterator<E> listIterator(int index);

    // A view of the portion [fromIndex, toIndex).
    List<E> subList(int fromIndex, int toIndex);

    // It inserts all of `c`'s from `index` on.
    boolean addAll(int index, Collection<? extends E> c);

}
