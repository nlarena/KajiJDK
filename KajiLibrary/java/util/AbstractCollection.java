package java.util;

import java.lang.reflect.Array;

// The skeleton every collection is built on: give it `iterator()` and `size()` and it derives
// the rest. That is the whole idea of the abstract-skeleton classes — the JDK ships one per
// collection shape so an implementor writes the two or three genuinely new methods and
// inherits a dozen.
//
// Note on the mutators: the JDK implements `remove(Object)` and `clear()` by walking the
// iterator and calling `Iterator.remove()`. KajiLibrary's `Iterator` is the two-method subset
// (hasNext/next) with no `remove`, so those two refuse here instead of being derived — the
// honest consequence of the smaller interface, and the reason a concrete class still overrides
// them.
public abstract class AbstractCollection<E> implements Collection<E> {

    protected AbstractCollection() {
    }

    public abstract Iterator<E> iterator();

    public abstract int size();

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(Object o) {
        boolean found = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            Object e = it.next();
            if (o == null) {
                if (e == null) {
                    found = true;
                }
            } else if (o.equals(e)) {
                found = true;
            }
        }
        return found;
    }

    // Unsupported unless a subclass overrides it — a read-only collection is a valid one.
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    // "[a, b, c]" — the shape every collection prints in, derived once here.
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('[');
        Iterator<E> it = iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (!first) {
                b.append(',');
                b.append(' ');
            }
            first = false;
            Object e = it.next();
            if (e == null) {
                b.append("null");
            } else {
                b.append(e.toString());
            }
        }
        b.append(']');
        return b.toString();
    }
    // ---- the bulk operations -----------------------------------------------------------------
    //
    // All of them work over a **snapshot** (`toArray()`) and not over the live iterator. The JDK
    // writes them with `Iterator.remove()`, and that road is closed here: our `Iterator.remove()` is
    // the `default` that throws, and no concrete iterator in the library implements it. Taking the
    // snapshot first costs one extra pass and is correct with any iterator.

    // Every element of `c` is in this collection.
    public boolean containsAll(Collection<?> c) {
        Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            if (!this.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    // It adds all of `c`'s; it returns whether this collection changed.
    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            if (this.add(it.next())) {
                changed = true;
            }
        }
        return changed;
    }

    // It removes **every** occurrence of each element of `c`.
    //
    // The inner loop is not superfluous: `remove(Object)` takes out a single occurrence, and a list
    // can hold several of the same element. Without it, `removeAll` would leave duplicates behind.
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        Object[] snapshot = this.toArray();
        int i = 0;
        while (i < snapshot.length) {
            if (c.contains(snapshot[i])) {
                while (this.remove(snapshot[i])) {
                    changed = true;
                }
            }
            i = i + 1;
        }
        return changed;
    }

    // It keeps only the elements that are also in `c`.
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        Object[] snapshot = this.toArray();
        int i = 0;
        while (i < snapshot.length) {
            if (!c.contains(snapshot[i])) {
                while (this.remove(snapshot[i])) {
                    changed = true;
                }
            }
            i = i + 1;
        }
        return changed;
    }

    // The elements in a fresh array, in the iterator's order.
    public Object[] toArray() {
        Object[] out = new Object[this.size()];
        int i = 0;
        Iterator<E> it = this.iterator();
        while (it.hasNext() && i < out.length) {
            out[i] = it.next();
            i = i + 1;
        }
        return out;
    }

    // The elements in `a` if they fit, or in a fresh array **of the same runtime type** if not.
    //
    // That "of the same runtime type" is why the overload exists: the caller passes a `String[0]`
    // precisely in order to get back a `String[]` and not an `Object[]`. Reflection is needed to
    // create it, because the array's type is only known at run time.
    //
    // If `a` has room left over, the position after the last element is left null: it is how the
    // caller knows where what was copied ends when reusing a larger array.
    public <T> T[] toArray(T[] a) {
        int n = this.size();
        Object[] dest = a;
        if (a.length < n) {
            dest = (Object[]) Array.newInstance(a.getClass().getComponentType(), n);
        }
        int i = 0;
        Iterator<E> it = this.iterator();
        while (it.hasNext() && i < n) {
            dest[i] = it.next();
            i = i + 1;
        }
        if (dest.length > n) {
            dest[n] = null;
        }
        return (T[]) dest;
    }

}
