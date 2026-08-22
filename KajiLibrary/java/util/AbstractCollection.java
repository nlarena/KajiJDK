package java.util;

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
}
