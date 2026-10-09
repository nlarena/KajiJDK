package java.util;

// The list `Arrays.asList` returns: a fixed-size **view** over the caller's array.
//
// The difference from {@link FixedList} is the whole point and it is one word: this one does not
// copy. `set` writes into the array that was passed in, and a write to that array shows up here --
// which is what `Arrays.asList` promises and what makes `Collections.sort(Arrays.asList(a))` sort
// `a` itself.
//
// The size is fixed because an array's is: `add` and `remove` refuse, exactly as in `FixedList`.
// That is the half the two share, and it is why `asList` was allowed to return a copy for as long
// as nothing wrote through it.
//
// `null` is a legal element here, unlike in `List.of`. An array can hold nulls, and a view that
// rejected what it is a view of would be describing a different array.
final class ArrayAsList<E> extends AbstractList<E> implements List<E> {

    // The caller's array itself -- not a copy. Holding the reference is the contract.
    private final Object[] items;

    ArrayAsList(Object[] items) {
        this.items = items;
    }

    public E get(int index) {
        if (index < 0 || index >= this.items.length) {
            throw new IndexOutOfBoundsException();
        }
        return (E) this.items[index];
    }

    public E set(int index, E element) {
        if (index < 0 || index >= this.items.length) {
            throw new IndexOutOfBoundsException();
        }
        Object old = this.items[index];
        this.items[index] = element;
        return (E) old;
    }

    public int size() {
        return this.items.length;
    }

    public boolean isEmpty() {
        return this.items.length == 0;
    }

    public int indexOf(Object o) {
        int i = 0;
        while (i < this.items.length) {
            if (o == null) {
                if (this.items[i] == null) {
                    return i;
                }
            } else if (o.equals(this.items[i])) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    public boolean contains(Object o) {
        return this.indexOf(o) >= 0;
    }

    // ---- the three that change the length, and cannot ----

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}
