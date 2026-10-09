package java.util;

// The wrappers Collections's three families return: unmodifiableX, synchronizedX and checkedX.
// Package-private, because the contract only promises the interface back.
//
// The JDK has one class per family and per interface -- UnmodifiableList, SynchronizedList,
// CheckedList, and so on for each of the eight. Here there is a single family with three switches,
// because the three do exactly the same thing (delegate) and differ only in what they do BEFORE:
//
//   readOnly   the mutators throw UnsupportedOperationException instead of delegating
//   type       add/set validate the element's class and throw ClassCastException on the spot
//   (lock)     everything goes through `this`'s monitor
//
// About the lock one has to be clear: **every** wrapper takes the monitor, not only synchronizedX's.
// It is a monitor nobody else looks at in the read-only ones, so the cost is an uncontended
// monitorenter, and in exchange each method does not have to be written twice. For the one that DOES
// synchronise, the monitor is the wrapper itself, which is what the JDK documents: whoever needs to
// walk the whole thing has to do `synchronized (list) { ... }` themselves, because an iterator cannot
// be protected from the inside.
//
// What checkedX brings is not obvious until the hole it plugs is seen: with erased generics, a
// `List<String>` passed as a raw `List` accepts an Integer without complaint, and the
// ClassCastException turns up much later, at the `get`, far from whoever caused it. `checkedList`
// moves the error to the moment of the `add`.
class GuardedCollection<E> implements Collection<E> {

    final Collection<E> back;

    // The class each element coming in is required to be, or null if nothing is validated.
    final Class<E> type;

    final boolean readOnly;

    GuardedCollection(Collection<E> back, Class<E> type, boolean readOnly) {
        if (back == null) {
            throw new NullPointerException();
        }
        this.back = back;
        this.type = type;
        this.readOnly = readOnly;
    }

    // It cuts off any mutation if the wrapper is read-only.
    final void noWrite() {
        if (this.readOnly) {
            throw new UnsupportedOperationException();
        }
    }

    // It validates the class of the element coming in. It returns the same element so it can be
    // chained.
    final E check(E e) {
        if (this.type != null && e != null && !this.type.isInstance(e)) {
            throw new ClassCastException("Attempt to insert " + e.getClass().getName()
                    + " element into collection with element type " + this.type.getName());
        }
        return e;
    }

    // It validates a whole collection before inserting it, so as not to leave it half added.
    final Collection<E> checkAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int i = 0;
        while (i < a.length) {
            this.check((E) a[i]);
            i = i + 1;
        }
        return (Collection<E>) c;
    }

    public int size() {
        synchronized (this) {
            return this.back.size();
        }
    }

    public boolean isEmpty() {
        synchronized (this) {
            return this.back.isEmpty();
        }
    }

    public boolean contains(Object o) {
        synchronized (this) {
            return this.back.contains(o);
        }
    }

    public boolean containsAll(Collection<?> c) {
        synchronized (this) {
            return this.back.containsAll(c);
        }
    }

    public Object[] toArray() {
        synchronized (this) {
            return this.back.toArray();
        }
    }

    public <T> T[] toArray(T[] a) {
        synchronized (this) {
            return this.back.toArray(a);
        }
    }

    // The iterator is wrapped only when it has to be: if the wrapper is read-only, `remove()` has to
    // be plugged, or it would be the back door for modifying. In the other two cases the inner one is
    // returned as it is -- wrapping it would bring nothing, and for the synchronised one it would be
    // misleading: protecting each call separately does not make the whole walk safe.
    public Iterator<E> iterator() {
        synchronized (this) {
            if (this.readOnly) {
                return new GuardedItr<E>(this.back.iterator());
            }
            return this.back.iterator();
        }
    }

    public boolean add(E e) {
        this.noWrite();
        synchronized (this) {
            return this.back.add(this.check(e));
        }
    }

    public boolean remove(Object o) {
        this.noWrite();
        synchronized (this) {
            return this.back.remove(o);
        }
    }

    public boolean addAll(Collection<? extends E> c) {
        this.noWrite();
        synchronized (this) {
            return this.back.addAll(this.checkAll(c));
        }
    }

    public boolean removeAll(Collection<?> c) {
        this.noWrite();
        synchronized (this) {
            return this.back.removeAll(c);
        }
    }

    public boolean retainAll(Collection<?> c) {
        this.noWrite();
        synchronized (this) {
            return this.back.retainAll(c);
        }
    }

    public void clear() {
        this.noWrite();
        synchronized (this) {
            this.back.clear();
        }
    }

    // Collection's defaults that mutate also have to respect the lock: `removeIf` reaches
    // `iterator().remove()` or `remove(Object)`, and without this a read-only wrapper would fail with
    // the wrong exception -- or worse, would remove something before failing.
    public boolean removeIf(java.util.function.Predicate<? super E> filter) {
        this.noWrite();
        synchronized (this) {
            return this.back.removeIf(filter);
        }
    }

    public void forEach(java.util.function.Consumer<? super E> action) {
        synchronized (this) {
            this.back.forEach(action);
        }
    }

    public String toString() {
        synchronized (this) {
            return this.back.toString();
        }
    }
}

// The version with encounter order: it adds the two ends.
class GuardedSequencedCollection<E> extends GuardedCollection<E> implements SequencedCollection<E> {

    GuardedSequencedCollection(SequencedCollection<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final SequencedCollection<E> seq() {
        return (SequencedCollection<E>) this.back;
    }

    public SequencedCollection<E> reversed() {
        synchronized (this) {
            return new GuardedSequencedCollection<E>(this.seq().reversed(), this.type, this.readOnly);
        }
    }

    public void addFirst(E e) {
        this.noWrite();
        synchronized (this) {
            this.seq().addFirst(this.check(e));
        }
    }

    public void addLast(E e) {
        this.noWrite();
        synchronized (this) {
            this.seq().addLast(this.check(e));
        }
    }

    public E getFirst() {
        synchronized (this) {
            return this.seq().getFirst();
        }
    }

    public E getLast() {
        synchronized (this) {
            return this.seq().getLast();
        }
    }

    public E removeFirst() {
        this.noWrite();
        synchronized (this) {
            return this.seq().removeFirst();
        }
    }

    public E removeLast() {
        this.noWrite();
        synchronized (this) {
            return this.seq().removeLast();
        }
    }
}

// List's wrapper. `equals`/`hashCode` delegate because List defines them by content: a wrapped list
// has to go on being equal to the one inside, or `unmodifiableList(x).equals(x)` would give false and
// no `assertEquals` would pass.
class GuardedList<E> extends GuardedSequencedCollection<E> implements List<E> {

    GuardedList(List<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final List<E> list() {
        return (List<E>) this.back;
    }

    public E get(int index) {
        synchronized (this) {
            return this.list().get(index);
        }
    }

    public E set(int index, E element) {
        this.noWrite();
        synchronized (this) {
            return this.list().set(index, this.check(element));
        }
    }

    public void add(int index, E element) {
        this.noWrite();
        synchronized (this) {
            this.list().add(index, this.check(element));
        }
    }

    public E remove(int index) {
        this.noWrite();
        synchronized (this) {
            return this.list().remove(index);
        }
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        this.noWrite();
        synchronized (this) {
            return this.list().addAll(index, this.checkAll(c));
        }
    }

    public int indexOf(Object o) {
        synchronized (this) {
            return this.list().indexOf(o);
        }
    }

    public int lastIndexOf(Object o) {
        synchronized (this) {
            return this.list().lastIndexOf(o);
        }
    }

    public ListIterator<E> listIterator() {
        return this.listIterator(0);
    }

    public ListIterator<E> listIterator(int index) {
        synchronized (this) {
            return new GuardedLitr<E>(this.list().listIterator(index), this.type, this.readOnly);
        }
    }

    // The sublist is wrapped with the same guard, or it would be the hole through which to write
    // into a read-only list.
    public List<E> subList(int fromIndex, int toIndex) {
        synchronized (this) {
            return new GuardedList<E>(this.list().subList(fromIndex, toIndex), this.type, this.readOnly);
        }
    }

    public List<E> reversed() {
        synchronized (this) {
            return new GuardedList<E>(this.list().reversed(), this.type, this.readOnly);
        }
    }

    public void replaceAll(java.util.function.UnaryOperator<E> operator) {
        this.noWrite();
        synchronized (this) {
            this.list().replaceAll(operator);
        }
    }

    public void sort(Comparator<? super E> c) {
        this.noWrite();
        synchronized (this) {
            this.list().sort(c);
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        synchronized (this) {
            return this.back.equals(o);
        }
    }

    public int hashCode() {
        synchronized (this) {
            return this.back.hashCode();
        }
    }
}

class GuardedSet<E> extends GuardedCollection<E> implements Set<E> {

    GuardedSet(Set<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        synchronized (this) {
            return this.back.equals(o);
        }
    }

    public int hashCode() {
        synchronized (this) {
            return this.back.hashCode();
        }
    }
}

class GuardedSequencedSet<E> extends GuardedSequencedCollection<E> implements SequencedSet<E> {

    GuardedSequencedSet(SequencedSet<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    public SequencedSet<E> reversed() {
        synchronized (this) {
            SequencedSet<E> r = (SequencedSet<E>) this.seq().reversed();
            return new GuardedSequencedSet<E>(r, this.type, this.readOnly);
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        synchronized (this) {
            return this.back.equals(o);
        }
    }

    public int hashCode() {
        synchronized (this) {
            return this.back.hashCode();
        }
    }
}

// A SortedSet's three slices come back wrapped with the same guard, for the same reason as the
// sublist.
class GuardedSortedSet<E> extends GuardedSequencedSet<E> implements SortedSet<E> {

    GuardedSortedSet(SortedSet<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final SortedSet<E> sorted() {
        return (SortedSet<E>) this.back;
    }

    public Comparator<? super E> comparator() {
        synchronized (this) {
            return this.sorted().comparator();
        }
    }

    public SortedSet<E> subSet(E from, E to) {
        synchronized (this) {
            return new GuardedSortedSet<E>(this.sorted().subSet(from, to), this.type, this.readOnly);
        }
    }

    public SortedSet<E> headSet(E to) {
        synchronized (this) {
            return new GuardedSortedSet<E>(this.sorted().headSet(to), this.type, this.readOnly);
        }
    }

    public SortedSet<E> tailSet(E from) {
        synchronized (this) {
            return new GuardedSortedSet<E>(this.sorted().tailSet(from), this.type, this.readOnly);
        }
    }

    public E first() {
        synchronized (this) {
            return this.sorted().first();
        }
    }

    public E last() {
        synchronized (this) {
            return this.sorted().last();
        }
    }
}

class GuardedNavigableSet<E> extends GuardedSortedSet<E> implements NavigableSet<E> {

    GuardedNavigableSet(NavigableSet<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final NavigableSet<E> nav() {
        return (NavigableSet<E>) this.back;
    }

    public E lower(E e) {
        synchronized (this) {
            return this.nav().lower(e);
        }
    }

    public E floor(E e) {
        synchronized (this) {
            return this.nav().floor(e);
        }
    }

    public E ceiling(E e) {
        synchronized (this) {
            return this.nav().ceiling(e);
        }
    }

    public E higher(E e) {
        synchronized (this) {
            return this.nav().higher(e);
        }
    }

    public E pollFirst() {
        this.noWrite();
        synchronized (this) {
            return this.nav().pollFirst();
        }
    }

    public E pollLast() {
        this.noWrite();
        synchronized (this) {
            return this.nav().pollLast();
        }
    }

    public NavigableSet<E> descendingSet() {
        synchronized (this) {
            return new GuardedNavigableSet<E>(this.nav().descendingSet(), this.type, this.readOnly);
        }
    }

    public Iterator<E> descendingIterator() {
        synchronized (this) {
            if (this.readOnly) {
                return new GuardedItr<E>(this.nav().descendingIterator());
            }
            return this.nav().descendingIterator();
        }
    }

    public NavigableSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive) {
        synchronized (this) {
            NavigableSet<E> s = this.nav().subSet(from, fromInclusive, to, toInclusive);
            return new GuardedNavigableSet<E>(s, this.type, this.readOnly);
        }
    }

    public NavigableSet<E> headSet(E to, boolean inclusive) {
        synchronized (this) {
            return new GuardedNavigableSet<E>(this.nav().headSet(to, inclusive), this.type, this.readOnly);
        }
    }

    public NavigableSet<E> tailSet(E from, boolean inclusive) {
        synchronized (this) {
            return new GuardedNavigableSet<E>(this.nav().tailSet(from, inclusive), this.type, this.readOnly);
        }
    }
}

class GuardedQueue<E> extends GuardedCollection<E> implements Queue<E> {

    GuardedQueue(Queue<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final Queue<E> queue() {
        return (Queue<E>) this.back;
    }

    public boolean offer(E e) {
        this.noWrite();
        synchronized (this) {
            return this.queue().offer(this.check(e));
        }
    }

    // `poll` takes out, so it counts as a mutator even though its name does not say so.
    public E poll() {
        this.noWrite();
        synchronized (this) {
            return this.queue().poll();
        }
    }

    public E peek() {
        synchronized (this) {
            return this.queue().peek();
        }
    }
}

// The read-only iterator: the only thing that changes is that `remove()` refuses.
final class GuardedItr<E> implements Iterator<E> {

    private final Iterator<E> back;

    GuardedItr(Iterator<E> back) {
        this.back = back;
    }

    public boolean hasNext() {
        return this.back.hasNext();
    }

    public E next() {
        return this.back.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

// The wrapped ListIterator. Besides plugging the mutators where called for, it validates the type in
// `set` and in `add`: they are the other way into a list, and checkedList would not be worth much if
// it could be dodged by going through the iterator.
final class GuardedLitr<E> implements ListIterator<E> {

    private final ListIterator<E> back;
    private final Class<E> type;
    private final boolean readOnly;

    GuardedLitr(ListIterator<E> back, Class<E> type, boolean readOnly) {
        this.back = back;
        this.type = type;
        this.readOnly = readOnly;
    }

    private E check(E e) {
        if (this.readOnly) {
            throw new UnsupportedOperationException();
        }
        if (this.type != null && e != null && !this.type.isInstance(e)) {
            throw new ClassCastException("Attempt to insert " + e.getClass().getName()
                    + " element into collection with element type " + this.type.getName());
        }
        return e;
    }

    public boolean hasNext() {
        return this.back.hasNext();
    }

    public E next() {
        return this.back.next();
    }

    public boolean hasPrevious() {
        return this.back.hasPrevious();
    }

    public E previous() {
        return this.back.previous();
    }

    public int nextIndex() {
        return this.back.nextIndex();
    }

    public int previousIndex() {
        return this.back.previousIndex();
    }

    public void remove() {
        if (this.readOnly) {
            throw new UnsupportedOperationException();
        }
        this.back.remove();
    }

    public void set(E e) {
        this.back.set(this.check(e));
    }

    public void add(E e) {
        this.back.add(this.check(e));
    }
}
