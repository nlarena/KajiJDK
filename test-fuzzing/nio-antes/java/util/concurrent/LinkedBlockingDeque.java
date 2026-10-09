package java.util.concurrent;

import java.util.Spliterator;
import java.util.Spliterators;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Predicate;

// An optionally bounded deque of linked nodes, where both ends block. Taking from an empty
// deque waits for an element; putting into a full one waits for room. That pairing is what
// makes it a hand-off channel between threads rather than a container you have to poll.
//
// The nodes are doubly linked so that either end is O(1), and the whole thing is guarded by
// one monitor. The JDK splits this into two locks (one per end) to let a producer and a
// consumer proceed in parallel; a single monitor is simpler, and correctness — not
// throughput — is what the surface promises. Waiters are woken with notifyAll, since a
// putter and a taker can be queued on the same monitor and only the right one may proceed.
//
// `capacity` is fixed at construction; the unbounded constructor uses Integer.MAX_VALUE,
// which is the JDK's own way of saying "effectively unbounded" without a second code path.
//
// No `throws InterruptedException` on the blocking methods: restating a compiled
// superinterface's throws clause is rejected (finding #104) and the descriptor is the same.
public class LinkedBlockingDeque<E> extends AbstractQueue<E> implements BlockingDeque<E>, Serializable {

    private final Object lock = new Object();
    private final int capacity;
    private LbdNode<E> head;
    private LbdNode<E> tail;
    private int count;

    public LinkedBlockingDeque() {
        this.capacity = Integer.MAX_VALUE;
    }

    public LinkedBlockingDeque(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public LinkedBlockingDeque(Collection<? extends E> c) {
        this.capacity = Integer.MAX_VALUE;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            E e = it.next();
            if (e == null) {
                throw new NullPointerException();
            }
            linkLast(e);
        }
    }

    // ------------------------------------------------------------ link/unlink, lock held

    private boolean linkFirst(E e) {
        boolean added = false;
        if (count < capacity) {
            LbdNode<E> node = new LbdNode<E>(e);
            node.next = head;
            if (head == null) {
                tail = node;
            } else {
                head.prev = node;
            }
            head = node;
            count = count + 1;
            lock.notifyAll();
            added = true;
        }
        return added;
    }

    private boolean linkLast(E e) {
        boolean added = false;
        if (count < capacity) {
            LbdNode<E> node = new LbdNode<E>(e);
            node.prev = tail;
            if (tail == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
            count = count + 1;
            lock.notifyAll();
            added = true;
        }
        return added;
    }

    private E unlinkFirst() {
        E value = null;
        if (head != null) {
            LbdNode<E> node = head;
            value = node.item;
            head = node.next;
            if (head == null) {
                tail = null;
            } else {
                head.prev = null;
            }
            node.next = null;
            count = count - 1;
            lock.notifyAll();
        }
        return value;
    }

    private E unlinkLast() {
        E value = null;
        if (tail != null) {
            LbdNode<E> node = tail;
            value = node.item;
            tail = node.prev;
            if (tail == null) {
                head = null;
            } else {
                tail.next = null;
            }
            node.prev = null;
            count = count - 1;
            lock.notifyAll();
        }
        return value;
    }

    // Splice a known node out of the chain. Used by the occurrence removers and by
    // removeIf/removeAll, which need to drop elements from the middle.
    private void unlink(LbdNode<E> node) {
        LbdNode<E> p = node.prev;
        LbdNode<E> n = node.next;
        if (p == null) {
            head = n;
        } else {
            p.next = n;
        }
        if (n == null) {
            tail = p;
        } else {
            n.prev = p;
        }
        node.prev = null;
        node.next = null;
        count = count - 1;
        lock.notifyAll();
    }

    // Wait for the monitor, translating an interrupt into an unchecked failure: the checked
    // form cannot be declared here (finding #104).
    private void await(long millis) {
        try {
            if (millis <= 0L) {
                lock.wait();
            } else {
                lock.wait(millis);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("interrupted while waiting on the deque");
        }
    }

    // ------------------------------------------------------------ head insertion

    public void addFirst(E e) {
        if (!offerFirst(e)) {
            throw new IllegalStateException("Deque full");
        }
    }

    public boolean offerFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        boolean added;
        synchronized (lock) {
            added = linkFirst(e);
        }
        return added;
    }

    public void putFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            while (!linkFirst(e)) {
                await(0L);
            }
        }
    }

    public boolean offerFirst(E e, long timeout, TimeUnit unit) {
        if (e == null) {
            throw new NullPointerException();
        }
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        boolean added;
        synchronized (lock) {
            added = linkFirst(e);
            long remaining = millis;
            while (!added && remaining > 0L) {
                await(remaining);
                added = linkFirst(e);
                remaining = deadline - System.currentTimeMillis();
            }
        }
        return added;
    }

    // ------------------------------------------------------------ tail insertion

    public void addLast(E e) {
        if (!offerLast(e)) {
            throw new IllegalStateException("Deque full");
        }
    }

    public boolean offerLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        boolean added;
        synchronized (lock) {
            added = linkLast(e);
        }
        return added;
    }

    public void putLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            while (!linkLast(e)) {
                await(0L);
            }
        }
    }

    public boolean offerLast(E e, long timeout, TimeUnit unit) {
        if (e == null) {
            throw new NullPointerException();
        }
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        boolean added;
        synchronized (lock) {
            added = linkLast(e);
            long remaining = millis;
            while (!added && remaining > 0L) {
                await(remaining);
                added = linkLast(e);
                remaining = deadline - System.currentTimeMillis();
            }
        }
        return added;
    }

    // ------------------------------------------------------------ head removal

    public E removeFirst() {
        E value = pollFirst();
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public E pollFirst() {
        E value;
        synchronized (lock) {
            value = unlinkFirst();
        }
        return value;
    }

    public E takeFirst() {
        E value;
        synchronized (lock) {
            value = unlinkFirst();
            while (value == null) {
                await(0L);
                value = unlinkFirst();
            }
        }
        return value;
    }

    public E pollFirst(long timeout, TimeUnit unit) {
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        E value;
        synchronized (lock) {
            value = unlinkFirst();
            long remaining = millis;
            while (value == null && remaining > 0L) {
                await(remaining);
                value = unlinkFirst();
                remaining = deadline - System.currentTimeMillis();
            }
        }
        return value;
    }

    // ------------------------------------------------------------ tail removal

    public E removeLast() {
        E value = pollLast();
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public E pollLast() {
        E value;
        synchronized (lock) {
            value = unlinkLast();
        }
        return value;
    }

    public E takeLast() {
        E value;
        synchronized (lock) {
            value = unlinkLast();
            while (value == null) {
                await(0L);
                value = unlinkLast();
            }
        }
        return value;
    }

    public E pollLast(long timeout, TimeUnit unit) {
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        E value;
        synchronized (lock) {
            value = unlinkLast();
            long remaining = millis;
            while (value == null && remaining > 0L) {
                await(remaining);
                value = unlinkLast();
                remaining = deadline - System.currentTimeMillis();
            }
        }
        return value;
    }

    // ------------------------------------------------------------ inspection

    public E getFirst() {
        E value = peekFirst();
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public E getLast() {
        E value = peekLast();
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public E peekFirst() {
        E value = null;
        synchronized (lock) {
            if (head != null) {
                value = head.item;
            }
        }
        return value;
    }

    public E peekLast() {
        E value = null;
        synchronized (lock) {
            if (tail != null) {
                value = tail.item;
            }
        }
        return value;
    }

    // ------------------------------------------------------------ occurrence removal

    public boolean removeFirstOccurrence(Object o) {
        boolean removed = false;
        if (o != null) {
            synchronized (lock) {
                LbdNode<E> p = head;
                while (p != null && !removed) {
                    if (o.equals(p.item)) {
                        unlink(p);
                        removed = true;
                    }
                    p = p.next;
                }
            }
        }
        return removed;
    }

    public boolean removeLastOccurrence(Object o) {
        boolean removed = false;
        if (o != null) {
            synchronized (lock) {
                LbdNode<E> p = tail;
                while (p != null && !removed) {
                    if (o.equals(p.item)) {
                        unlink(p);
                        removed = true;
                    }
                    p = p.prev;
                }
            }
        }
        return removed;
    }

    // ------------------------------------------------------------ Queue view: head out, tail in

    public boolean add(E e) {
        addLast(e);
        return true;
    }

    public boolean offer(E e) {
        return offerLast(e);
    }

    public void put(E e) {
        putLast(e);
    }

    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offerLast(e, timeout, unit);
    }

    public E remove() {
        return removeFirst();
    }

    public E poll() {
        return pollFirst();
    }

    public E take() {
        return takeFirst();
    }

    public E poll(long timeout, TimeUnit unit) {
        return pollFirst(timeout, unit);
    }

    public E element() {
        return getFirst();
    }

    public E peek() {
        return peekFirst();
    }

    // ------------------------------------------------------------ Deque-as-stack view

    public void push(E e) {
        addFirst(e);
    }

    public E pop() {
        return removeFirst();
    }

    // ------------------------------------------------------------ Collection view

    public int size() {
        int n;
        synchronized (lock) {
            n = count;
        }
        return n;
    }

    public int remainingCapacity() {
        int n;
        synchronized (lock) {
            n = capacity - count;
        }
        return n;
    }

    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    public boolean contains(Object o) {
        boolean found = false;
        if (o != null) {
            synchronized (lock) {
                LbdNode<E> p = head;
                while (p != null && !found) {
                    if (o.equals(p.item)) {
                        found = true;
                    }
                    p = p.next;
                }
            }
        }
        return found;
    }

    public boolean addAll(Collection<? extends E> c) {
        if (c == this) {
            throw new IllegalArgumentException("cannot add a deque to itself");
        }
        boolean changed = false;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            E e = it.next();
            if (add(e)) {
                changed = true;
            }
        }
        return changed;
    }

    public void clear() {
        synchronized (lock) {
            head = null;
            tail = null;
            count = 0;
            lock.notifyAll();
        }
    }

    public Object[] toArray() {
        Object[] a;
        synchronized (lock) {
            a = new Object[count];
            LbdNode<E> p = head;
            int i = 0;
            while (p != null) {
                a[i] = p.item;
                i = i + 1;
                p = p.next;
            }
        }
        return a;
    }

    public <T> T[] toArray(T[] a) {
        Object[] snapshot = toArray();
        T[] out = a;
        int n = snapshot.length;
        if (a.length < n) {
            out = (T[]) new Object[n];
        }
        int i = 0;
        while (i < n) {
            out[i] = (T) snapshot[i];
            i = i + 1;
        }
        if (a.length > n) {
            out[n] = null;
        }
        return out;
    }

    // Drains without blocking: whatever is there right now moves to `c`.
    public int drainTo(Collection<? super E> c) {
        return drainInto(c, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        return drainInto(c, maxElements);
    }

    // The shared body, over a raw Collection. Handing a `Collection<? super E>` parameter
    // straight to another `Collection<? super E>` parameter is rejected here — the captured
    // wildcard is not recognised as convertible to itself — so the capture is dropped at
    // this one boundary instead.
    private int drainInto(Collection sink, int maxElements) {
        if (sink == this) {
            throw new IllegalArgumentException("cannot drain a deque into itself");
        }
        int moved = 0;
        synchronized (lock) {
            while (moved < maxElements && head != null) {
                E value = unlinkFirst();
                sink.add(value);
                moved = moved + 1;
            }
        }
        return moved;
    }

    public boolean removeIf(Predicate<? super E> filter) {
        boolean changed = false;
        synchronized (lock) {
            LbdNode<E> p = head;
            while (p != null) {
                LbdNode<E> next = p.next;
                if (filter.test(p.item)) {
                    unlink(p);
                    changed = true;
                }
                p = next;
            }
        }
        return changed;
    }

    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        synchronized (lock) {
            LbdNode<E> p = head;
            while (p != null) {
                LbdNode<E> next = p.next;
                if (c.contains(p.item)) {
                    unlink(p);
                    changed = true;
                }
                p = next;
            }
        }
        return changed;
    }

    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        synchronized (lock) {
            LbdNode<E> p = head;
            while (p != null) {
                LbdNode<E> next = p.next;
                if (!c.contains(p.item)) {
                    unlink(p);
                    changed = true;
                }
                p = next;
            }
        }
        return changed;
    }

    public void forEach(Consumer<? super E> action) {
        Object[] snapshot = toArray();
        int i = 0;
        while (i < snapshot.length) {
            action.accept((E) snapshot[i]);
            i = i + 1;
        }
    }

    public String toString() {
        Object[] snapshot = toArray();
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (i < snapshot.length) {
            if (i > 0) {
                b.append(',');
                b.append(' ');
            }
            b.append(String.valueOf(snapshot[i]));
            i = i + 1;
        }
        b.append(']');
        return b.toString();
    }

    // Both iterators walk a snapshot, so they are weakly consistent by construction: they
    // never throw ConcurrentModificationException and never see an element twice.
    public Iterator<E> iterator() {
        return new LbdItr<E>(toArray(), true);
    }

    public Iterator<E> descendingIterator() {
        return new LbdItr<E>(toArray(), false);
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
    }
}

// A node of the doubly linked chain. Fields, not accessors: it is package-private plumbing.
final class LbdNode<E> {

    E item;
    LbdNode<E> prev;
    LbdNode<E> next;

    LbdNode(E item) {
        this.item = item;
    }
}

// Snapshot iterator, forwards or backwards depending on `ascending`. remove() is
// unsupported: on a snapshot it could not say which live node it meant.
final class LbdItr<E> implements Iterator<E> {

    private final Object[] items;
    private final boolean ascending;
    private int cursor;

    LbdItr(Object[] items, boolean ascending) {
        this.items = items;
        this.ascending = ascending;
        if (ascending) {
            this.cursor = 0;
        } else {
            this.cursor = items.length - 1;
        }
    }

    public boolean hasNext() {
        boolean more;
        if (ascending) {
            more = cursor < items.length;
        } else {
            more = cursor >= 0;
        }
        return more;
    }

    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Object value = items[cursor];
        if (ascending) {
            cursor = cursor + 1;
        } else {
            cursor = cursor - 1;
        }
        return (E) value;
    }

}
