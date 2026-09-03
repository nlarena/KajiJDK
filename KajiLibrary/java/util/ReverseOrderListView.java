package java.util;

// Same-package imports: the classes below are referenced by simple name and must be resolvable
// when this file is compiled with `-cp KajiLibrary`, so they bind to KajiLibrary's own types.
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;

// What {@link List#reversed} hands back: a *view*, so there is exactly one copy of the data and
// a write through the view is a write to the base list. The whole class is one arithmetic
// identity — view index `i` is base index `size() - 1 - i` — applied to every member.
//
// Insertion is the one place that identity needs care: `add(i, e)` must leave the element AT
// view index `i`, and that is judged against the size the list will have afterwards, so the
// base index is `base.size() - i` rather than `base.size() - 1 - i`.
//
// KNOWN COST: `iterator()` walks by index, calling `get` once per step. `List` here does not
// declare `listIterator`, which is what the JDK's reverse view uses to walk a linked list
// backwards in one pass; without it there is no way to ask a base list for a cursor. So
// iterating the reverse of a {@link LinkedList} is O(n^2) — every `get` re-walks the chain. It
// is correct, and it is the honest consequence of the subset; a copy would iterate fast and
// stop being a view, which is the wrong trade.
final class ReverseOrderListView<E> extends AbstractList<E> implements List<E> {

    private final List<E> base;
    // Si la vista deja escribir. Un `reversed()` sobre una lista inmutable tiene que dar una vista
    // inmutable: sin este flag la vista seria mas permisiva que la lista que envuelve, y el rechazo
    // llegaria --si llega-- desde la base, con un mensaje que habla de otra cosa.
    private final boolean modifiable;

    ReverseOrderListView(List<E> base) {
        this(base, true);
    }

    private ReverseOrderListView(List<E> base, boolean modifiable) {
        this.base = base;
        this.modifiable = modifiable;
    }

    /**
     * La vista invertida de `list`, modificable o no.
     *
     * <p>Es la entrada que usa `List.reversed()`. El JDK ademas elige aca entre dos clases segun la
     * lista implemente `RandomAccess` o no --para que la vista herede esa propiedad--; aca hay una
     * sola clase, asi que la vista de una lista de acceso aleatorio **no** se anuncia como tal. Es
     * una diferencia conservadora: quien pregunte por `RandomAccess` va a elegir el algoritmo por
     * cursor, que es correcto para las dos.
     */
    static <T> List<T> of(List<T> list, boolean modifiable) {
        if (list == null) {
            throw new NullPointerException("list");
        }
        return new ReverseOrderListView<T>(list, modifiable);
    }

    // Todo mutador pasa por aca antes de tocar la base.
    private void chequearModificable() {
        if (!this.modifiable) {
            throw new UnsupportedOperationException();
        }
    }

    // Reversing a reverse view is the base list itself, not a third object stacked on top: the
    // identity is exact, so returning anything else would only add indirection.
    public List<E> reversed() {
        return this.base;
    }

    // --- indexed access ---

    public E get(int index) {
        return this.base.get(this.base.size() - 1 - index);
    }

    public E set(int index, E element) {
        this.chequearModificable();
        return this.base.set(this.base.size() - 1 - index, element);
    }

    public void add(int index, E element) {
        this.chequearModificable();
        this.base.add(this.base.size() - index, element);
    }

    public E remove(int index) {
        this.chequearModificable();
        return this.base.remove(this.base.size() - 1 - index);
    }

    // First in view order is last in the base, and `List` has no `lastIndexOf` to ask for it —
    // so this scans, in view order, and stops at the first hit.
    public int indexOf(Object o) {
        int n = this.base.size();
        for (int i = 0; i < n; i++) {
            E e = this.get(i);
            if (o == null) {
                if (e == null) {
                    return i;
                }
            } else if (o.equals(e)) {
                return i;
            }
        }
        return -1;
    }

    // --- Collection ---

    public int size() {
        return this.base.size();
    }

    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    // Membership does not depend on order, so it goes straight through.
    public boolean contains(Object o) {
        return this.base.contains(o);
    }

    // Appending to the view means prepending to the base.
    public boolean add(E e) {
        this.chequearModificable();
        this.base.add(0, e);
        return true;
    }

    // Removes the *first* match in view order, which is the last one in the base — hence the
    // detour through indexOf instead of delegating to base.remove(Object).
    public boolean remove(Object o) {
        this.chequearModificable();
        int i = this.indexOf(o);
        if (i < 0) {
            return false;
        }
        this.remove(i);
        return true;
    }

    public void clear() {
        this.chequearModificable();
        this.base.clear();
    }

    public Iterator<E> iterator() {
        return new ReverseOrderListItr<E>(this);
    }
}

// Reads the view by index, so it inherits both the reversal and the cost documented above.
final class ReverseOrderListItr<E> implements Iterator<E> {

    private final List<E> view;
    private int cursor;

    ReverseOrderListItr(List<E> view) {
        this.view = view;
    }

    public boolean hasNext() {
        return this.cursor < this.view.size();
    }

    public E next() {
        if (this.cursor >= this.view.size()) {
            throw new NoSuchElementException();
        }
        E e = this.view.get(this.cursor);
        this.cursor = this.cursor + 1;
        return e;
    }
}
