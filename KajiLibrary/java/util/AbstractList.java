package java.util;

// The skeleton for lists backed by an index: give it `get(int)` and `size()` and it derives
// iteration and search. Its counterpart {@link AbstractSequentialList} does the same for lists
// backed by links, deriving indexed access from an iterator instead — the two skeletons exist
// precisely because a list can be efficient at one or the other, rarely both.
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {

    protected AbstractList() {
    }

    public abstract E get(int index);

    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    // Appending is inserting at the end — so a subclass that implements add(int, E) gets this.
    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    public int indexOf(Object o) {
        int found = -1;
        int n = size();
        for (int i = 0; i < n; i++) {
            if (found < 0) {
                Object e = get(i);
                if (o == null) {
                    if (e == null) {
                        found = i;
                    }
                } else if (o.equals(e)) {
                    found = i;
                }
            }
        }
        return found;
    }

    // Walks by index, which is exactly what "backed by an index" buys.
    public Iterator<E> iterator() {
        return new AbstractListItr<E>(this);
    }

    // ---- what List adds over Collection, derived from the index   ---------------------------

    /**
     * How many times this list has been structurally modified.
     *
     * <p>The JDK keeps it so an iterator can detect that the list changed underneath it and throw
     * ConcurrentModificationException. Here it is declared because it is protected API —a subclass
     * from another package can read it— but **nobody consults it yet**: this library's iterators do
     * not detect concurrent modification. It is said so nobody assumes otherwise.
     */
    protected transient int modCount = 0;

    // The index of the LAST occurrence of `o`, or -1. It walks from the end, which is what tells it
    // from indexOf: the first match going backwards is the last going forwards, and that way it stops
    // sooner in the typical case.
    public int lastIndexOf(Object o) {
        int i = this.size() - 1;
        while (i >= 0) {
            E e = this.get(i);
            if (o == null) {
                if (e == null) {
                    return i;
                }
            } else if (o.equals(e)) {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    // A two-way cursor from the start.
    public ListIterator<E> listIterator() {
        return new AbstractListLitr<E>(this, 0);
    }

    // A two-way cursor from `index`.
    public ListIterator<E> listIterator(int index) {
        return new AbstractListLitr<E>(this, index);
    }

    // A **view** of [fromIndex, toIndex): writing into it writes into this list.
    public List<E> subList(int fromIndex, int toIndex) {
        return new SubList<E>(this, fromIndex, toIndex);
    }

    // It inserts all of `c`'s from `index` on, in its iterator's order.
    public boolean addAll(int index, Collection<? extends E> c) {
        if (index < 0 || index > this.size()) {
            throw new IndexOutOfBoundsException();
        }
        boolean changed = false;
        int at = index;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            this.add(at, it.next());
            at = at + 1;
            changed = true;
        }
        return changed;
    }

    /**
     * It removes [fromIndex, toIndex).
     *
     * <p>Protected and not public on purpose, just as in the JDK: it is the hook a subclass uses to
     * give a cheap implementation of removal by range — `SubList.clear()` goes through here —
     * without offering it to any old caller, who has `subList(a, b).clear()`.
     */
    protected void removeRange(int fromIndex, int toIndex) {
        int i = toIndex;
        while (i > fromIndex) {
            this.remove(i - 1);
            i = i - 1;
        }
    }

    /**
     * Equality by content: two lists are equal if they have the same elements in the same order, no
     * matter which class they are.
     *
     * <p>It was missing, and it is one of the absences that do not show by measuring signatures:
     * `equals` and `hashCode` appear as inherited from Object, so no member count marks them. What
     * was inherited from Object is equality by **identity**, and with that
     * `new ArrayList(...).equals(new ArrayList(...))` gave false with the same content, no `List`
     * worked as a map key, and `List.of("x").equals(List.of("x"))` was false too.
     *
     * <p>It goes here and not in each list because the specification requires it symmetric across
     * different implementations: an ArrayList has to be equal to a LinkedList with the same elements.
     * An `equals` by concrete class would break precisely that.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }
        List<?> other = (List<?>) o;
        Iterator<E> a = this.iterator();
        Iterator<?> b = other.iterator();
        while (a.hasNext() && b.hasNext()) {
            if (!Objects.equals(a.next(), b.next())) {
                return false;
            }
        }
        // The two iterators are compared instead of the two size()s: that way equality does not
        // depend on size() being cheap, and nothing extra is walked when they differ at the first
        // element.
        return !a.hasNext() && !b.hasNext();
    }

    /**
     * The hash List's contract demands: 31 times the accumulator plus the element's hash, in order.
     * The formula is specified in detail, and it is not negotiable -- two equal lists of different
     * classes have to give the same number, and that is only achieved by fixing the sum.
     */
    public int hashCode() {
        int h = 1;
        Iterator<E> it = this.iterator();
        while (it.hasNext()) {
            E e = it.next();
            h = 31 * h + (e == null ? 0 : e.hashCode());
        }
        return h;
    }
}

// The index-walking iterator every AbstractList hands out.
final class AbstractListItr<E> implements Iterator<E> {

    private final AbstractList<E> list;
    private int cursor;

    AbstractListItr(AbstractList<E> list) {
        this.list = list;
    }

    public boolean hasNext() {
        return cursor < list.size();
    }

    public E next() {
        if (cursor >= list.size()) {
            throw new NoSuchElementException();
        }
        E e = list.get(cursor);
        cursor++;
        return e;
    }
}
