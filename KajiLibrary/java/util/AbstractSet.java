package java.util;

// The skeleton for sets. It adds no operations to {@link AbstractCollection} — a set differs
// from a collection by a *contract* (no duplicates), not by an interface — but it fixes the
// equality rule: two sets are equal when they hold the same elements, whatever their order or
// implementation, and a set hash is the sum of its element hashes so that rule survives it.
public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {

    protected AbstractSet() {
    }

    public boolean equals(Object o) {
        boolean same;
        if (o == this) {
            same = true;
        } else if (!(o instanceof Set)) {
            same = false;
        } else {
            Collection<E> other = (Collection<E>) o;
            if (other.size() != size()) {
                same = false;
            } else {
                same = true;
                Iterator<E> it = other.iterator();
                while (it.hasNext()) {
                    if (!contains(it.next())) {
                        same = false;
                    }
                }
            }
        }
        return same;
    }

    // Order-independent by construction: addition commutes, so two equal sets agree.
    public int hashCode() {
        int h = 0;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            Object e = it.next();
            if (e != null) {
                h = h + e.hashCode();
            }
        }
        return h;
    }
    // It removes from this set everything that is in `c`.
    //
    // **The smaller of the two** is walked, which is the optimisation the JDK makes here and which
    // changes the order of magnitude when one is much smaller: removing ten elements from a set of a
    // million has no reason to cost a million lookups.
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        if (this.size() > c.size()) {
            Iterator<?> it = c.iterator();
            while (it.hasNext()) {
                if (this.remove(it.next())) {
                    changed = true;
                }
            }
            return changed;
        }
        Object[] snapshot = this.toArray();
        int i = 0;
        while (i < snapshot.length) {
            if (c.contains(snapshot[i])) {
                if (this.remove(snapshot[i])) {
                    changed = true;
                }
            }
            i = i + 1;
        }
        return changed;
    }

}
