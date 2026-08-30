package java.util;

// An immutable Set over a fixed array. Package-private: it is what Set.of(...) and Set.copyOf(...)
// hand back, and the contract only promises a Set.
//
// The elements are deduplicated and null-checked at construction, which is where the JDK does it
// too: Set.of("a", "a") is an IllegalArgumentException, not a one-element set. Getting that wrong
// would turn a caller's duplicate — usually a copy/paste bug in a literal — into silence.
//
// Lookup is a linear scan. That is a deliberate trade for the sizes these factories are used at:
// a handful of literal elements, where the hash of a HashSet costs more than the scan saves.
final class FixedSet<E> extends AbstractSet<E> implements Set<E> {

    private final Object[] items;

    // Wraps `items` as-is; the caller has already checked it. Every construction site here builds a
    // fresh array and drops it, so the array never escapes.
    private FixedSet(Object[] items) {
        this.items = items;
    }

    // Builds a set from the first `n` slots of `src`, rejecting nulls and duplicates.
    static <E> FixedSet<E> fromArray(Object[] src, int n) {
        Object[] out = new Object[n];
        int size = 0;
        int i = 0;
        while (i < n) {
            Object e = src[i];
            if (e == null) {
                throw new NullPointerException();
            }
            int j = 0;
            while (j < size) {
                if (out[j].equals(e)) {
                    throw new IllegalArgumentException("duplicate element: " + e);
                }
                j = j + 1;
            }
            out[size] = e;
            size = size + 1;
            i = i + 1;
        }
        return new FixedSet<E>(out);
    }

    // Same, but a duplicate is dropped instead of rejected — the rule Set.copyOf follows, since a
    // collection with repeats is a normal thing to copy from.
    static <E> FixedSet<E> dedup(Object[] src, int n) {
        Object[] out = new Object[n];
        int size = 0;
        int i = 0;
        while (i < n) {
            Object e = src[i];
            if (e == null) {
                throw new NullPointerException();
            }
            boolean seen = false;
            int j = 0;
            while (j < size) {
                if (out[j].equals(e)) {
                    seen = true;
                }
                j = j + 1;
            }
            if (!seen) {
                out[size] = e;
                size = size + 1;
            }
            i = i + 1;
        }
        Object[] exact = new Object[size];
        int k = 0;
        while (k < size) {
            exact[k] = out[k];
            k = k + 1;
        }
        return new FixedSet<E>(exact);
    }

    public int size() {
        return this.items.length;
    }

    public boolean isEmpty() {
        return this.items.length == 0;
    }

    public boolean contains(Object o) {
        int i = 0;
        while (i < this.items.length) {
            if (this.items[i].equals(o)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    public Iterator<E> iterator() {
        return new FixedListItr<E>(this.items);
    }

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    // Set equality is by contents, whatever the other implementation is (§Set).
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        Set<?> other = (Set<?>) o;
        if (other.size() != this.items.length) {
            return false;
        }
        int i = 0;
        while (i < this.items.length) {
            if (!other.contains(this.items[i])) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // The sum of the element hashes, order-independent, as Set specifies.
    public int hashCode() {
        int h = 0;
        int i = 0;
        while (i < this.items.length) {
            h = h + this.items[i].hashCode();
            i = i + 1;
        }
        return h;
    }

    public String toString() {
        String s = "[";
        int i = 0;
        while (i < this.items.length) {
            if (i > 0) {
                s = s + ", ";
            }
            s = s + this.items[i];
            i = i + 1;
        }
        return s + "]";
    }
}
