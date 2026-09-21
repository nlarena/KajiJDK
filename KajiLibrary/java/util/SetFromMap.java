package java.util;

// The small views and adapters Collections returns: a Set resting on a Map, a Queue resting on a
// Deque, the two Enumerations and the reversed comparator. Package-private.

// A Set backed by a Map<E, Boolean>.
//
// It looks like a detour until one sees what it exists for: the JDK has IdentityHashMap and
// ConcurrentHashMap but has no IdentityHashSet nor ConcurrentHashSet, and instead of duplicating each
// map implementation in a set version, it exposes this. `newSetFromMap(new IdentityHashMap())` gives
// a set that compares by identity; `newSetFromMap(new ConcurrentHashMap())`, a thread-safe one. A Set
// is nothing more than a Map that does not care about the values.
//
// The map has to arrive **empty** and nobody else can touch it: if it has keys from before, the set
// is born with elements nobody added; if somebody writes to it from outside, the set changes
// unasked.
class SetFromMap<E> implements Set<E> {

    final Map<E, Boolean> m;

    // The key view, which is where almost all of the set's behaviour lives.
    private final Set<E> keys;

    SetFromMap(Map<E, Boolean> map) {
        if (!map.isEmpty()) {
            throw new IllegalArgumentException("Map is non-empty");
        }
        this.m = map;
        this.keys = map.keySet();
    }

    public int size() {
        return this.m.size();
    }

    public boolean isEmpty() {
        return this.m.isEmpty();
    }

    public boolean contains(Object o) {
        return this.m.containsKey(o);
    }

    // `put` returns the previous value, or null if the key was not there: that is exactly what `add`
    // has to report.
    public boolean add(E e) {
        return this.m.put(e, Boolean.TRUE) == null;
    }

    public boolean remove(Object o) {
        return this.m.remove(o) != null;
    }

    public void clear() {
        this.m.clear();
    }

    public Iterator<E> iterator() {
        return this.keys.iterator();
    }

    public Object[] toArray() {
        return this.keys.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return this.keys.toArray(a);
    }

    public boolean containsAll(Collection<?> c) {
        return this.keys.containsAll(c);
    }

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

    public boolean removeAll(Collection<?> c) {
        return this.keys.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return this.keys.retainAll(c);
    }

    public boolean equals(Object o) {
        return o == this || this.keys.equals(o);
    }

    public int hashCode() {
        return this.keys.hashCode();
    }

    public String toString() {
        return this.keys.toString();
    }
}

// The same idea over a SequencedMap, which keeps the encounter order and can therefore be
// reversed.
final class SequencedSetFromMap<E> extends SetFromMap<E> implements SequencedSet<E> {

    SequencedSetFromMap(SequencedMap<E, Boolean> map) {
        super(map);
    }

    public SequencedSet<E> reversed() {
        return new SequencedSetFromMap<E>(((SequencedMap<E, Boolean>) this.m).reversed());
    }

    public void addFirst(E e) {
        ((SequencedMap<E, Boolean>) this.m).putFirst(e, Boolean.TRUE);
    }

    public void addLast(E e) {
        ((SequencedMap<E, Boolean>) this.m).putLast(e, Boolean.TRUE);
    }

    public E getFirst() {
        return ((SequencedMap<E, Boolean>) this.m).firstEntry().getKey();
    }

    public E getLast() {
        return ((SequencedMap<E, Boolean>) this.m).lastEntry().getKey();
    }

    public E removeFirst() {
        return ((SequencedMap<E, Boolean>) this.m).pollFirstEntry().getKey();
    }

    public E removeLast() {
        return ((SequencedMap<E, Boolean>) this.m).pollLastEntry().getKey();
    }
}

// A Queue that takes out where it puts in: a stack with a queue's face.
//
// It serves to hand a stack to code written against Queue without that code noticing. The only thing
// that changes from the Deque behind is that the three queue operations point at the same end -- the
// front -- instead of putting in at the back and taking out at the front.
final class LifoQueue<E> implements Queue<E> {

    private final Deque<E> back;

    LifoQueue(Deque<E> back) {
        if (back == null) {
            throw new NullPointerException();
        }
        this.back = back;
    }

    public boolean add(E e) {
        this.back.addFirst(e);
        return true;
    }

    public boolean offer(E e) {
        return this.back.offerFirst(e);
    }

    public E poll() {
        return this.back.pollFirst();
    }

    public E peek() {
        return this.back.peekFirst();
    }

    public int size() {
        return this.back.size();
    }

    public boolean isEmpty() {
        return this.back.isEmpty();
    }

    public boolean contains(Object o) {
        return this.back.contains(o);
    }

    public boolean remove(Object o) {
        return this.back.remove(o);
    }

    public void clear() {
        this.back.clear();
    }

    public Iterator<E> iterator() {
        return this.back.iterator();
    }

    public Object[] toArray() {
        return this.back.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return this.back.toArray(a);
    }

    public boolean containsAll(Collection<?> c) {
        return this.back.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            this.back.addFirst(it.next());
            changed = true;
        }
        return changed;
    }

    public boolean removeAll(Collection<?> c) {
        return this.back.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return this.back.retainAll(c);
    }

    public String toString() {
        return this.back.toString();
    }
}

// The Enumeration that has nothing.
final class EmptyEnumeration<E> implements Enumeration<E> {

    public boolean hasMoreElements() {
        return false;
    }

    public E nextElement() {
        throw new NoSuchElementException();
    }
}

// An Enumeration over a snapshot of the collection. The snapshot is taken in the constructor on
// purpose: `Collections.enumeration` promises to walk what was there, and an Enumeration has no way
// of saying the collection changed in the meantime -- there is no equivalent of
// ConcurrentModificationException for it.
final class ArrayEnumeration<E> implements Enumeration<E> {

    private final Object[] items;
    private int cursor;

    ArrayEnumeration(Object[] items) {
        this.items = items;
        this.cursor = 0;
    }

    public boolean hasMoreElements() {
        return this.cursor < this.items.length;
    }

    public E nextElement() {
        if (this.cursor >= this.items.length) {
            throw new NoSuchElementException();
        }
        E e = (E) this.items[this.cursor];
        this.cursor = this.cursor + 1;
        return e;
    }
}

// The comparator that reverses another, or the natural order if none is passed.
//
// Reversing is `compare(b, a)` and not `-compare(a, b)`: the negation breaks with Integer.MIN_VALUE,
// which is its own negative, and a comparator that returns MIN_VALUE is odd but legal.
final class ReverseComparator<T> implements Comparator<T> {

    private final Comparator<T> inner;

    ReverseComparator(Comparator<T> inner) {
        this.inner = inner;
    }

    public int compare(T a, T b) {
        if (this.inner != null) {
            return this.inner.compare(b, a);
        }
        return ((Comparable<T>) b).compareTo(a);
    }
}
