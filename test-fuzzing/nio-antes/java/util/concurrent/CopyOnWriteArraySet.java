package java.util.concurrent;

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;

// A set with the copy-on-write policy of {@link CopyOnWriteArrayList}, and in fact built on
// one: reads take no lock and walk a stable array, while every write copies. Uniqueness is
// enforced by scanning before inserting, so `add` is O(n) and so is `contains` — there is no
// hash table underneath.
//
// That sounds like a bad set until you look at what it is for: small collections read far
// more often than written, above all listener registries. There, a linear scan of a dozen
// elements costs less than a hash lookup's indirection, and readers never block or see a
// half-written structure. Past a few hundred elements this is the wrong class.
//
// Reusing CopyOnWriteArrayList (rather than reimplementing the copying) is what the JDK does
// too, and it is the honest design: the set is a *policy* — no duplicates — layered on a
// list that already has the concurrency story.
public class CopyOnWriteArraySet<E> extends AbstractSet<E> implements Serializable {

    // The whole state. `addIfAbsent` is the one operation that must be atomic with respect
    // to other writers, and the list already provides it under its own monitor — so this
    // class needs no lock of its own.
    private final CopyOnWriteArrayList<E> al = new CopyOnWriteArrayList<E>();

    public CopyOnWriteArraySet() {
    }

    // A set holding the distinct elements of `c`. Built through addAllAbsent rather than by copying
    // the array: `c` is a Collection, not a Set, so it may well contain duplicates, and dropping
    // them here is the whole difference between this constructor and the list's.
    public CopyOnWriteArraySet(Collection<? extends E> c) {
        al.addAllAbsent(c);
    }

    public int size() {
        return al.size();
    }

    public boolean isEmpty() {
        return al.isEmpty();
    }

    public boolean contains(Object o) {
        return al.contains(o);
    }

    // The set contract in one call: insert only if absent, atomically. Doing it as
    // `if (!contains(e)) add(e)` here would race — two threads could both see it absent.
    public boolean add(E e) {
        return al.addIfAbsent(e);
    }

    public boolean remove(Object o) {
        return al.remove(o);
    }

    public void clear() {
        al.clear();
    }

    // Snapshot iteration, inherited from the list: it reflects the set as of this call and
    // never throws ConcurrentModificationException.
    public Iterator<E> iterator() {
        return al.iterator();
    }


    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.DISTINCT | Spliterator.SIZED | Spliterator.SUBSIZED |
                        Spliterator.IMMUTABLE);
    }
}
