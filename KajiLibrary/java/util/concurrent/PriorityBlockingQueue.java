package java.util.concurrent;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

// An **unbounded** blocking queue whose head is the smallest element rather than the oldest.
// Two ideas meet here, and it is worth being precise about which half does what:
//
//   - the *priority* half is a binary min-heap, and it is already written: {@link
//     java.util.PriorityQueue} holds the array, the sift-up and the sift-down. This class
//     delegates to one rather than re-deriving the heap, so the O(log n) insert/extract and
//     the ordering rules (natural {@link Comparable} order, or a {@link Comparator} handed
//     to the constructor) are exactly that class's, tested once.
//
//   - the *blocking* half is this class's own: a monitor around the heap, and a {@link #take}
//     that parks while the heap is empty instead of returning null.
//
// Unbounded is the other half of the name, and it has a consequence that surprises people:
// **the producer side never blocks**. {@link #put} is just an offer, {@link #offer} always
// returns true, the timed offer ignores its timeout, and {@link #remainingCapacity} answers
// Integer.MAX_VALUE forever. Only {@link #take} can wait. That means this queue applies no
// backpressure at all — a producer outrunning its consumers grows the heap until memory runs
// out, which is precisely the failure {@link ArrayBlockingQueue}'s bound exists to prevent.
// You choose this queue when *order of service* matters more than flow control.
//
// The JDK guards its heap with a ReentrantLock and one notEmpty Condition; here one monitor
// serves both roles, with notifyAll waking the takers.
//
// Iteration returns a snapshot in *heap* order, not priority order — the same caveat
// PriorityQueue carries, for the same reason: the heap only promises that a parent precedes
// its children, and reading the elements in sorted order means draining the queue.
//
// Single-exit style throughout (finding #105).
public class PriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {

    private final Object sync = new Object();
    // The heap. All the ordering logic lives in here; this class only adds the waiting.
    private final PriorityQueue<E> heap;

    public PriorityBlockingQueue() {
        heap = new PriorityQueue<E>();
    }

    public PriorityBlockingQueue(int initialCapacity) {
        heap = new PriorityQueue<E>(initialCapacity);
    }

    public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
        heap = new PriorityQueue<E>(initialCapacity, comparator);
    }

    public PriorityBlockingQueue(Collection<? extends E> c) {
        heap = new PriorityQueue<E>(c);
    }

    // The ordering rule in force, or null when elements order themselves.
    public Comparator<? super E> comparator() {
        Comparator<? super E> cmp;
        synchronized (sync) {
            cmp = heap.comparator();
        }
        return cmp;
    }

    // Always succeeds: there is no capacity to run out of.
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        synchronized (sync) {
            heap.offer(e);
            sync.notifyAll();
        }
        return true;
    }

    // Never blocks, despite the name — the queue is unbounded, so there is nothing to wait
    // for. Present because BlockingQueue demands it.
    public void put(E e) {
        offer(e);
    }

    // Same: the timeout is unreachable, so it is ignored and the answer is always true.
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    // Remove and return the smallest element, waiting while the queue is empty. This is the
    // one operation that can block.
    public E take() {
        E e;
        synchronized (sync) {
            while (heap.size() == 0) {
                sync.wait();
            }
            e = heap.poll();
        }
        return e;
    }

    public E poll() {
        E e;
        synchronized (sync) {
            e = heap.poll();
        }
        return e;
    }

    public E poll(long timeout, TimeUnit unit) {
        E e;
        synchronized (sync) {
            if (heap.size() == 0) {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    sync.wait(ms);
                }
            }
            e = heap.poll();
        }
        return e;
    }

    public E peek() {
        E e;
        synchronized (sync) {
            e = heap.peek();
        }
        return e;
    }

    public int size() {
        int n;
        synchronized (sync) {
            n = heap.size();
        }
        return n;
    }

    // Unbounded, so this is a constant. Spelled out rather than read from Integer.MAX_VALUE:
    // reading a static field of another compiled class traps at run time (finding #110).
    public int remainingCapacity() {
        return 2147483647;
    }

    public boolean contains(Object o) {
        boolean found;
        synchronized (sync) {
            found = heap.contains(o);
        }
        return found;
    }

    // O(n): the heap can find its minimum in constant time but an arbitrary element only by
    // scanning. PriorityQueue does the scan and the local repair afterwards.
    public boolean remove(Object o) {
        boolean removed;
        synchronized (sync) {
            removed = heap.remove(o);
        }
        return removed;
    }

    public void clear() {
        synchronized (sync) {
            heap.clear();
        }
    }

    // A snapshot taken under the monitor, so iterating neither tears nor blocks producers.
    // The order is the heap's array order — see the class comment.
    public Iterator<E> iterator() {
        Object[] snapshot;
        synchronized (sync) {
            snapshot = new Object[heap.size()];
            // Bound to a local: chaining a call whose intermediate returns an INTERFACE
            // (`heap.iterator().hasNext()`) is silently dropped (finding #108).
            Iterator<E> it = heap.iterator();
            int i = 0;
            while (it.hasNext()) {
                snapshot[i] = it.next();
                i = i + 1;
            }
        }
        // AbqItr is this package's snapshot iterator, written for ArrayBlockingQueue; the
        // semantics are identical, so it is reused rather than duplicated.
        return new AbqItr<E>(snapshot);
    }
}
