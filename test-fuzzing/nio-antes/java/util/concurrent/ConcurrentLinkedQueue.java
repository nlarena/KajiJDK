package java.util.concurrent;

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

// An unbounded FIFO queue that never makes a caller wait. Unlike {@link LinkedBlockingQueue}
// there is no `take` and no capacity: `offer` always succeeds and `poll` returns null rather
// than blocking on an empty queue. That is the point — it is the hand-off structure for code
// that has something else to do when there is nothing to consume, and it cannot deadlock
// because no thread ever parks in it.
//
// The JDK builds it from a Michael–Scott lock-free linked list driven by CAS. Here the same
// linked list is guarded by the intrinsic monitor of a private `sync` object: on a runtime
// that interleaves threads between opcodes the two are observably identical, and the monitor
// version is the one you can read. What the lock-free original buys is scalability under
// contention, not different semantics.
//
// Single-exit style throughout (finding #105).
public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, Serializable {

    private final Object sync = new Object();
    // First and last live nodes; both null exactly when the queue is empty.
    private ClqNode<E> head;
    private ClqNode<E> tail;
    private int count;

    public ConcurrentLinkedQueue() {
    }

    // A queue holding the elements of `c`, in the order its iterator returns them. Each goes in
    // through offer(), so a null element in `c` is rejected here rather than becoming an element
    // that poll() would later report as "empty".
    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            offer(it.next());
        }
    }

    // Always succeeds: the queue is unbounded, so there is no "full" to report.
    public boolean offer(E e) {
        if (e == null) {
            // Null is the sentinel `poll`/`peek` return for "empty", so it cannot also be an
            // element — the same reason every java.util.concurrent queue rejects it.
            throw new NullPointerException();
        }
        synchronized (sync) {
            ClqNode<E> node = new ClqNode<E>(e);
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

    public E poll() {
        E e;
        synchronized (sync) {
            ClqNode<E> h = head;
            if (h == null) {
                e = null;
            } else {
                e = h.item;
                // Clear the item but leave `next` intact: an iterator parked on this node
                // must still be able to walk off it. See ClqItr.
                h.item = null;
                head = h.next;
                if (head == null) {
                    tail = null;
                }
                count--;
            }
        }
        return e;
    }

    public E peek() {
        E e;
        synchronized (sync) {
            ClqNode<E> h = head;
            if (h == null) {
                e = null;
            } else {
                e = h.item;
            }
        }
        return e;
    }

    // Exact, and O(1), because the count is maintained under the monitor. The JDK's is O(n)
    // and explicitly approximate — it has to walk the list, and the list can change while it
    // does. Worth knowing when porting code that calls size() in a loop.
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

    public boolean contains(Object o) {
        boolean found = false;
        synchronized (sync) {
            ClqNode<E> n = head;
            while (n != null) {
                if (n.item != null && eq(o, n.item)) {
                    found = true;
                }
                n = n.next;
            }
        }
        return found;
    }

    // Remove the first occurrence. The node is unlinked from the chain, but again its `next`
    // is left alone so any iterator standing on it can still move forward.
    public boolean remove(Object o) {
        boolean removed = false;
        synchronized (sync) {
            ClqNode<E> prev = null;
            ClqNode<E> n = head;
            while (n != null && !removed) {
                if (n.item != null && eq(o, n.item)) {
                    n.item = null;
                    if (prev == null) {
                        head = n.next;
                    } else {
                        prev.next = n.next;
                    }
                    if (tail == n) {
                        tail = prev;
                    }
                    count--;
                    removed = true;
                }
                prev = n;
                n = n.next;
            }
        }
        return removed;
    }

    public void clear() {
        synchronized (sync) {
            head = null;
            tail = null;
            count = 0;
        }
    }

    // Weakly consistent, exactly like the JDK's: it walks the live chain instead of a copy,
    // so it sees elements added after it started and never throws
    // ConcurrentModificationException. The cost is that it offers no snapshot guarantee —
    // which is the honest trade for a queue no one is allowed to lock.
    public Iterator<E> iterator() {
        ClqNode<E> start;
        synchronized (sync) {
            start = head;
        }
        return new ClqItr<E>(start);
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
    }
}

// A singly-linked cell. `item` is cleared when the element leaves the queue, which is how a
// live-chain iterator learns to skip it; `next` is never cleared, so a node that has been
// unlinked is still a valid stepping stone out of the dead region.
final class ClqNode<E> {

    E item;
    ClqNode<E> next;

    ClqNode(E item) {
        this.item = item;
    }
}

// The weakly-consistent iterator. It prefetches the next live element so that `hasNext` and
// `next` agree even if the element is polled away in between — without the prefetch, a
// `hasNext()` that returned true could be followed by a `next()` with nothing to hand back.
final class ClqItr<E> implements Iterator<E> {

    private ClqNode<E> cursor;
    private E nextItem;

    ClqItr(ClqNode<E> start) {
        advance(start);
    }

    // Park the cursor on the first node from `n` onward that still holds an element.
    private void advance(ClqNode<E> n) {
        E found = null;
        while (n != null && found == null) {
            found = n.item;
            if (found == null) {
                n = n.next;
            }
        }
        cursor = n;
        nextItem = found;
    }

    public boolean hasNext() {
        return nextItem != null;
    }

    public E next() {
        if (nextItem == null) {
            throw new NoSuchElementException();
        }
        E e = nextItem;
        advance(cursor.next);
        return e;
    }

}
