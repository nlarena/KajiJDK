package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

// The growable array from Java 1.0 — {@link ArrayList} with a lock. Same backing `Object[]`,
// same amortized growth, same O(1) indexing; the difference is that every public operation
// here takes the object's own monitor, and that it drags along the 1.0 method names
// (addElement/elementAt/removeElementAt) that the 1.2 collections replaced with add/get/remove.
//
// The lock is why you should not reach for it. Locking each *operation* is not the same as
// locking each *transaction*: `if (!v.contains(x)) v.add(x)` is two atomic steps with a gap in
// between, so a caller that cares still has to synchronize externally — at which point the
// per-method lock is pure cost. That is the whole argument for ArrayList plus explicit
// synchronization, and the reason Vector survives only in code too old to change.
//
// One extra knob over ArrayList: `capacityIncrement`. Zero (the default) means "double when
// full", the standard amortized-O(1) growth; a positive value means "grow by exactly this
// many", which is linear growth and therefore O(n) amortized per add — a 1.0-era tuning
// mistake preserved for compatibility.
//
// A note on how this file is written: our javac accepts the `synchronized` *method modifier*
// but does not emit ACC_SYNCHRONIZED for it, so the methods below use an explicit
// `synchronized (this)` block instead — identical semantics, and exactly the monitor a
// synchronized method would take. Each such block assigns to a local and returns after the
// block, because a `return` from inside one leaks the monitor (finding #105).
//
// Subset of the JDK's: the bulk operations (addAll/removeAll/retainAll), subList, the
// ListIterator, clone and serialization are not modelled.
public class Vector<E> extends AbstractList<E> implements List<E>, RandomAccess {

    // Protected, as in the JDK: these three *are* Vector's public surface for a subclass —
    // {@link Stack} reads them directly rather than going through the locked accessors.
    protected Object[] elementData;

    protected int elementCount;

    protected int capacityIncrement;

    public Vector() {
        elementData = new Object[10];
        capacityIncrement = 0;
    }

    public Vector(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity");
        }
        elementData = new Object[initialCapacity];
        capacityIncrement = 0;
    }

    public Vector(int initialCapacity, int capacityIncrement) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity");
        }
        elementData = new Object[initialCapacity];
        this.capacityIncrement = capacityIncrement;
    }

    public Vector(Collection<? extends E> c) {
        elementData = new Object[10];
        capacityIncrement = 0;
        // The wildcard is widened to E by a cast so the iterator's element type is a plain
        // type variable — a capture-converted `Iterator<capture-of ? extends E>` is more
        // than our javac's inference handles.
        Collection<E> src = (Collection<E>) c;
        Iterator<E> it = src.iterator();
        while (it.hasNext()) {
            addUnlocked(it.next());
        }
    }

    // --- unsynchronized internals ---------------------------------------------------
    //
    // The real work lives here, lock-free. The public methods below are a thin locked shell
    // over these, which keeps the locking visible in one place instead of tangled with the
    // array bookkeeping.

    // Make room for at least `min` elements. Doubling by default; `capacityIncrement` slots
    // at a time when the caller asked for that.
    private void growTo(int min) {
        if (min > elementData.length) {
            int bigger = elementData.length;
            if (capacityIncrement > 0) {
                bigger = bigger + capacityIncrement;
            } else {
                bigger = bigger * 2;
            }
            if (bigger < min) {
                bigger = min;
            }
            Object[] copy = new Object[bigger];
            for (int i = 0; i < elementCount; i++) {
                copy[i] = elementData[i];
            }
            elementData = copy;
        }
    }

    private void addUnlocked(E e) {
        growTo(elementCount + 1);
        elementData[elementCount] = e;
        elementCount = elementCount + 1;
    }

    private void insertUnlocked(int index, E e) {
        if (index < 0 || index > elementCount) {
            throw new ArrayIndexOutOfBoundsException();
        }
        growTo(elementCount + 1);
        for (int i = elementCount; i > index; i--) {
            elementData[i] = elementData[i - 1];
        }
        elementData[index] = e;
        elementCount = elementCount + 1;
    }

    private Object removeUnlocked(int index) {
        if (index < 0 || index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Object old = elementData[index];
        for (int i = index; i < elementCount - 1; i++) {
            elementData[i] = elementData[i + 1];
        }
        elementCount = elementCount - 1;
        // Drop the duplicated tail reference, or the vector keeps the object alive.
        elementData[elementCount] = null;
        return old;
    }

    // Equality that tolerates nulls. A boolean-valued ternary is rejected by our javac
    // (finding #109), so this is spelled out as an if/else.
    private static boolean eq(Object a, Object b) {
        boolean same;
        if (a == null) {
            same = b == null;
        } else {
            same = a.equals(b);
        }
        return same;
    }

    private int indexUnlocked(Object o, int from) {
        int found = -1;
        for (int i = from; i < elementCount; i++) {
            if (found < 0 && eq(o, elementData[i])) {
                found = i;
            }
        }
        return found;
    }

    // Package-private, unlocked: the iterator and the enumeration read through these.
    Object elementUnlocked(int index) {
        return elementData[index];
    }

    int countUnlocked() {
        return elementCount;
    }

    // --- capacity -------------------------------------------------------------------

    public void copyInto(Object[] anArray) {
        synchronized (this) {
            for (int i = 0; i < elementCount; i++) {
                anArray[i] = elementData[i];
            }
        }
    }

    // Give back the unused tail. Worth calling once a vector has stopped growing; pointless
    // before that, since the next add will just grow it again.
    public void trimToSize() {
        synchronized (this) {
            if (elementCount < elementData.length) {
                Object[] copy = new Object[elementCount];
                for (int i = 0; i < elementCount; i++) {
                    copy[i] = elementData[i];
                }
                elementData = copy;
            }
        }
    }

    public void ensureCapacity(int minCapacity) {
        synchronized (this) {
            growTo(minCapacity);
        }
    }

    // Resize the *logical* length: truncating drops the tail, extending pads with null.
    // Nothing in the 1.2 collections does this — a List's length is a consequence of what
    // it holds, not something you set.
    public void setSize(int newSize) {
        synchronized (this) {
            if (newSize > elementCount) {
                growTo(newSize);
            } else {
                for (int i = newSize; i < elementCount; i++) {
                    elementData[i] = null;
                }
            }
            elementCount = newSize;
        }
    }

    public int capacity() {
        int c;
        synchronized (this) {
            c = elementData.length;
        }
        return c;
    }

    // --- Collection / List ----------------------------------------------------------

    public int size() {
        int n;
        synchronized (this) {
            n = elementCount;
        }
        return n;
    }

    public boolean isEmpty() {
        boolean empty;
        synchronized (this) {
            empty = elementCount == 0;
        }
        return empty;
    }

    public boolean contains(Object o) {
        boolean found;
        synchronized (this) {
            found = indexUnlocked(o, 0) >= 0;
        }
        return found;
    }

    public int indexOf(Object o) {
        int i;
        synchronized (this) {
            i = indexUnlocked(o, 0);
        }
        return i;
    }

    public int indexOf(Object o, int index) {
        int i;
        synchronized (this) {
            i = indexUnlocked(o, index);
        }
        return i;
    }

    public int lastIndexOf(Object o) {
        int found;
        synchronized (this) {
            found = -1;
            for (int i = elementCount - 1; i >= 0; i--) {
                if (found < 0 && eq(o, elementData[i])) {
                    found = i;
                }
            }
        }
        return found;
    }

    public int lastIndexOf(Object o, int index) {
        int found;
        synchronized (this) {
            if (index >= elementCount) {
                throw new IndexOutOfBoundsException();
            }
            found = -1;
            for (int i = index; i >= 0; i--) {
                if (found < 0 && eq(o, elementData[i])) {
                    found = i;
                }
            }
        }
        return found;
    }

    public E get(int index) {
        Object e;
        synchronized (this) {
            if (index < 0 || index >= elementCount) {
                throw new ArrayIndexOutOfBoundsException();
            }
            e = elementData[index];
        }
        return (E) e;
    }

    public E set(int index, E element) {
        Object old;
        synchronized (this) {
            if (index < 0 || index >= elementCount) {
                throw new ArrayIndexOutOfBoundsException();
            }
            old = elementData[index];
            elementData[index] = element;
        }
        return (E) old;
    }

    public boolean add(E e) {
        synchronized (this) {
            addUnlocked(e);
        }
        return true;
    }

    public void add(int index, E element) {
        synchronized (this) {
            insertUnlocked(index, element);
        }
    }

    public E remove(int index) {
        Object old;
        synchronized (this) {
            old = removeUnlocked(index);
        }
        return (E) old;
    }

    public boolean remove(Object o) {
        boolean removed;
        synchronized (this) {
            int i = indexUnlocked(o, 0);
            removed = i >= 0;
            if (removed) {
                removeUnlocked(i);
            }
        }
        return removed;
    }

    public void clear() {
        synchronized (this) {
            for (int i = 0; i < elementCount; i++) {
                elementData[i] = null;
            }
            elementCount = 0;
        }
    }

    public Object[] toArray() {
        Object[] copy;
        synchronized (this) {
            copy = new Object[elementCount];
            for (int i = 0; i < elementCount; i++) {
                copy[i] = elementData[i];
            }
        }
        return copy;
    }

    // --- the 1.0 names --------------------------------------------------------------
    //
    // Every one of these has an exact 1.2 equivalent above. They are kept because a decade
    // of code calls them, and they are a useful reminder of what the collections framework
    // actually bought: one vocabulary instead of one per class.

    public E elementAt(int index) {
        return get(index);
    }

    public E firstElement() {
        Object e;
        synchronized (this) {
            if (elementCount == 0) {
                throw new NoSuchElementException();
            }
            e = elementData[0];
        }
        return (E) e;
    }

    public E lastElement() {
        Object e;
        synchronized (this) {
            if (elementCount == 0) {
                throw new NoSuchElementException();
            }
            e = elementData[elementCount - 1];
        }
        return (E) e;
    }

    public void setElementAt(E obj, int index) {
        set(index, obj);
    }

    public void removeElementAt(int index) {
        synchronized (this) {
            removeUnlocked(index);
        }
    }

    public void insertElementAt(E obj, int index) {
        synchronized (this) {
            insertUnlocked(index, obj);
        }
    }

    public void addElement(E obj) {
        synchronized (this) {
            addUnlocked(obj);
        }
    }

    public boolean removeElement(Object obj) {
        return remove(obj);
    }

    public void removeAllElements() {
        clear();
    }

    public Enumeration<E> elements() {
        return new VectorEnumerator<E>(this);
    }

    public Iterator<E> iterator() {
        return new VectorItr<E>(this);
    }

    // --- equality -------------------------------------------------------------------

    // List equality: same length, equal elements in the same order — so a Vector can equal
    // an ArrayList, which is the point of defining it on the interface's terms.
    public boolean equals(Object o) {
        boolean same;
        if (o == this) {
            same = true;
        } else if (!(o instanceof List)) {
            same = false;
        } else {
            List<E> other = (List<E>) o;
            synchronized (this) {
                if (other.size() != elementCount) {
                    same = false;
                } else {
                    same = true;
                    for (int i = 0; i < elementCount; i++) {
                        Object mine = elementData[i];
                        Object theirs = other.get(i);
                        if (!eq(mine, theirs)) {
                            same = false;
                        }
                    }
                }
            }
        }
        return same;
    }

    // The order-sensitive companion of that equality: 31 is odd and prime, so the running
    // multiply neither loses bits nor collapses distinct prefixes.
    public int hashCode() {
        int h;
        synchronized (this) {
            h = 1;
            for (int i = 0; i < elementCount; i++) {
                Object e = elementData[i];
                int eh = 0;
                if (e != null) {
                    eh = e.hashCode();
                }
                h = 31 * h + eh;
            }
        }
        return h;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        synchronized (this) {
            b.append('[');
            for (int i = 0; i < elementCount; i++) {
                if (i > 0) {
                    b.append(',');
                    b.append(' ');
                }
                Object e = elementData[i];
                if (e == null) {
                    b.append("null");
                } else {
                    b.append(e.toString());
                }
            }
            b.append(']');
        }
        return b.toString();
    }
}

// Vector's iterator. Top-level package-private rather than nested, since a nested class inside
// a *generic* class is miscompiled (finding #13).
//
// Deliberately *not* locked: a lock per `next()` would still not make a traversal atomic, so
// it would buy nothing but the illusion of safety. The JDK's is the same shape — it grabs the
// lock only to avoid tearing a single read, and still tells the caller to synchronize on the
// vector for the whole loop.
final class VectorItr<E> implements Iterator<E> {

    private final Vector<E> vector;
    private int cursor;

    VectorItr(Vector<E> vector) {
        this.vector = vector;
    }

    public boolean hasNext() {
        return cursor < vector.countUnlocked();
    }

    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        E e = (E) vector.elementUnlocked(cursor);
        cursor = cursor + 1;
        return e;
    }
}

// The same walk wearing the 1.0 interface. Two names for one cursor is exactly the kind of
// duplication {@link Iterator} was introduced to end.
final class VectorEnumerator<E> implements Enumeration<E> {

    private final Vector<E> vector;
    private int cursor;

    VectorEnumerator(Vector<E> vector) {
        this.vector = vector;
    }

    public boolean hasMoreElements() {
        return cursor < vector.countUnlocked();
    }

    public E nextElement() {
        if (cursor >= vector.countUnlocked()) {
            throw new NoSuchElementException();
        }
        E e = (E) vector.elementUnlocked(cursor);
        cursor = cursor + 1;
        return e;
    }
}
