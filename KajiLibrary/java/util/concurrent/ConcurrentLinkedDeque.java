package java.util.concurrent;

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

// The double-ended sibling of {@link ConcurrentLinkedQueue}: unbounded, thread-safe, and
// never blocking. Both ends accept and yield elements, so one instance serves as a
// concurrent queue (push at the tail, take from the head) *or* a concurrent stack (push and
// take at the head) — which is why a work-stealing scheduler wants precisely this shape: an
// owner working one end while thieves raid the other.
//
// The JDK's version is a lock-free doubly linked list, and the hard part there is that a
// doubly linked node cannot be unlinked with a single CAS — it needs self-links, terminator
// nodes and a whole vocabulary of "logically deleted" states. Guarding the same list with
// one monitor makes unlinking a two-assignment affair; on a runtime that interleaves threads
// between opcodes the visible behaviour is the same, and the algorithm stays readable.
//
// Single-exit style throughout (finding #105).
public class ConcurrentLinkedDeque<E> extends AbstractCollection<E> implements Deque<E>, Serializable {

    private final Object sync = new Object();
    private CldNode<E> head;
    private CldNode<E> tail;
    private int count;

    public ConcurrentLinkedDeque() {
    }

    // A deque holding the elements of `c`, appended at the tail in iteration order -- so the first
    // element of `c` ends up at the head, which is what "same order" means for a deque.
    public ConcurrentLinkedDeque(Collection<? extends E> c) {
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            offerLast(it.next());
        }
    }

    // --- insertion ---

    public void addFirst(E e) {
        offerFirst(e);
    }

    public void addLast(E e) {
        offerLast(e);
    }

    // Unbounded, so an offer can never be refused; the boolean is there only because Deque
    // shares the signature with bounded implementations.
    public boolean offerFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        synchronized (sync) {
            CldNode<E> node = new CldNode<E>(e);
            node.next = head;
            if (head == null) {
                tail = node;
            } else {
                head.prev = node;
            }
            head = node;
            count++;
        }
        return true;
    }

    public boolean offerLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        synchronized (sync) {
            CldNode<E> node = new CldNode<E>(e);
            node.prev = tail;
            if (tail == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
            count++;
        }
        return true;
    }

    // --- removal ---

    public E pollFirst() {
        E e;
        synchronized (sync) {
            CldNode<E> h = head;
            if (h == null) {
                e = null;
            } else {
                e = h.item;
                unlink(h);
            }
        }
        return e;
    }

    public E pollLast() {
        E e;
        synchronized (sync) {
            CldNode<E> t = tail;
            if (t == null) {
                e = null;
            } else {
                e = t.item;
                unlink(t);
            }
        }
        return e;
    }

    // Drop `node` from the chain. Its own `prev`/`next` are deliberately left pointing into
    // the list: an iterator standing on a node that is concurrently removed must still be
    // able to step off it, which is what makes the traversal weakly consistent rather than
    // simply broken. Clearing `item` is what marks the node dead for those iterators.
    private void unlink(CldNode<E> node) {
        CldNode<E> p = node.prev;
        CldNode<E> n = node.next;
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
        node.item = null;
        count--;
    }

    public E removeFirst() {
        E e = pollFirst();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    public E removeLast() {
        E e = pollLast();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    // --- inspection ---

    public E peekFirst() {
        E e;
        synchronized (sync) {
            CldNode<E> h = head;
            if (h == null) {
                e = null;
            } else {
                e = h.item;
            }
        }
        return e;
    }

    public E peekLast() {
        E e;
        synchronized (sync) {
            CldNode<E> t = tail;
            if (t == null) {
                e = null;
            } else {
                e = t.item;
            }
        }
        return e;
    }

    public E getFirst() {
        E e = peekFirst();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    public E getLast() {
        E e = peekLast();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    // --- occurrence removal ---

    // Null-safe equality, as an if/else helper: a **boolean-valued** ternary is rejected by
    // our javac (finding #109).
    private static boolean eq(Object a, Object b) {
        boolean same;
        if (a == null) {
            same = b == null;
        } else {
            same = a.equals(b);
        }
        return same;
    }

    public boolean removeFirstOccurrence(Object o) {
        boolean removed = false;
        synchronized (sync) {
            CldNode<E> n = head;
            while (n != null && !removed) {
                if (n.item != null && eq(o, n.item)) {
                    unlink(n);
                    removed = true;
                }
                n = n.next;
            }
        }
        return removed;
    }

    public boolean removeLastOccurrence(Object o) {
        boolean removed = false;
        synchronized (sync) {
            CldNode<E> n = tail;
            while (n != null && !removed) {
                if (n.item != null && eq(o, n.item)) {
                    unlink(n);
                    removed = true;
                }
                n = n.prev;
            }
        }
        return removed;
    }

    // --- queue and stack views ---

    public boolean add(E e) {
        return offerLast(e);
    }

    public boolean offer(E e) {
        return offerLast(e);
    }

    public E poll() {
        return pollFirst();
    }

    public E peek() {
        return peekFirst();
    }

    public E remove() {
        return removeFirst();
    }

    public E element() {
        return getFirst();
    }

    public void push(E e) {
        addFirst(e);
    }

    public E pop() {
        return removeFirst();
    }

    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    // --- bulk queries ---

    public boolean contains(Object o) {
        boolean found = false;
        synchronized (sync) {
            CldNode<E> n = head;
            while (n != null) {
                if (n.item != null && eq(o, n.item)) {
                    found = true;
                }
                n = n.next;
            }
        }
        return found;
    }

    public int size() {
        int n;
        synchronized (sync) {
            n = count;
        }
        return n;
    }

    public boolean isEmpty() {
        boolean empty;
        synchronized (sync) {
            empty = head == null;
        }
        return empty;
    }

    public void clear() {
        synchronized (sync) {
            head = null;
            tail = null;
            count = 0;
        }
    }

    // --- traversal ---

    public Iterator<E> iterator() {
        CldNode<E> start;
        synchronized (sync) {
            start = head;
        }
        return new CldItr<E>(start, true);
    }

    public Iterator<E> descendingIterator() {
        CldNode<E> start;
        synchronized (sync) {
            start = tail;
        }
        return new CldItr<E>(start, false);
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
    }
}

// A doubly linked cell. `item` is cleared on removal (that is the "dead" marker an iterator
// reads); `prev`/`next` survive, so a cursor caught on a removed node can still move on.
final class CldNode<E> {

    E item;
    CldNode<E> prev;
    CldNode<E> next;

    CldNode(E item) {
        this.item = item;
    }
}

// One weakly-consistent cursor serving both directions — `forward` picks which link to
// follow. It prefetches the next live element so `hasNext` and `next` cannot disagree when
// a concurrent removal clears an item in between.
final class CldItr<E> implements Iterator<E> {

    private CldNode<E> cursor;
    private E nextItem;
    private final boolean forward;

    CldItr(CldNode<E> start, boolean forward) {
        this.forward = forward;
        advance(start);
    }

    // Park the cursor on the first node from `n` onward (in this iterator's direction) that
    // still holds an element.
    private void advance(CldNode<E> n) {
        E found = null;
        while (n != null && found == null) {
            found = n.item;
            if (found == null) {
                n = step(n);
            }
        }
        cursor = n;
        nextItem = found;
    }

    private CldNode<E> step(CldNode<E> n) {
        CldNode<E> next;
        if (forward) {
            next = n.next;
        } else {
            next = n.prev;
        }
        return next;
    }

    public boolean hasNext() {
        return nextItem != null;
    }

    public E next() {
        if (nextItem == null) {
            throw new NoSuchElementException();
        }
        E e = nextItem;
        advance(step(cursor));
        return e;
    }

}
