package java.util.concurrent;

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

import java.io.Serializable;
import java.util.Iterator;

// An optionally-bounded blocking queue over a singly-linked list: unbounded by default, so
// producers never park for space, and a node is allocated per element instead of reserving
// a whole array up front. That is the trade against {@link ArrayBlockingQueue} — this one
// suits an unknown or bursty backlog, the array one a fixed budget.
//
// The JDK runs two locks (one at each end) so a producer and a consumer can work at the
// same time; here one monitor covers both ends, which on a runtime that interleaves threads
// between opcodes is observably the same.
//
// Single-exit style throughout (finding #105).
public class LinkedBlockingQueue<E> implements BlockingQueue<E>, Serializable {

    private final Object sync = new Object();
    private final int capacity;
    private LbqNode<E> head;
    private LbqNode<E> tail;
    private int count;

    // Unbounded — well, bounded by Integer.MAX_VALUE, as the JDK puts it. Written as the
    // literal rather than `Integer.MAX_VALUE`: reading a static field of a *classpath*
    // class compiles to `getfield` instead of `getstatic` (finding #110), which traps at
    // run time.
    public LinkedBlockingQueue() {
        this.capacity = 2147483647;
    }

    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        this.capacity = capacity;
    }

    // Link a node at the tail. Caller holds sync and has checked for space.
    private void enqueue(E e) {
        LbqNode<E> node = new LbqNode<E>(e);
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        count++;
        sync.notifyAll();
    }

    // Unlink the head node. Caller holds sync and has checked the queue is non-empty.
    private E dequeue() {
        LbqNode<E> node = head;
        head = node.next;
        if (head == null) {
            tail = null;
        }
        node.next = null;
        count--;
        sync.notifyAll();
        return node.item;
    }

    public void put(E e) {
        synchronized (sync) {
            while (count == capacity) {
                sync.wait();
            }
            enqueue(e);
        }
    }

    public E take() {
        E e;
        synchronized (sync) {
            while (count == 0) {
                sync.wait();
            }
            e = dequeue();
        }
        return e;
    }

    public boolean offer(E e) {
        boolean added;
        synchronized (sync) {
            if (count == capacity) {
                added = false;
            } else {
                enqueue(e);
                added = true;
            }
        }
        return added;
    }

    // Best-effort timed offer: park once for the whole timeout, then re-check.
    public boolean offer(E e, long timeout, TimeUnit unit) {
        boolean added;
        synchronized (sync) {
            if (count == capacity) {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    sync.wait(ms);
                }
            }
            if (count == capacity) {
                added = false;
            } else {
                enqueue(e);
                added = true;
            }
        }
        return added;
    }

    public E poll() {
        E e;
        synchronized (sync) {
            if (count == 0) {
                e = null;
            } else {
                e = dequeue();
            }
        }
        return e;
    }

    public E poll(long timeout, TimeUnit unit) {
        E e;
        synchronized (sync) {
            if (count == 0) {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    sync.wait(ms);
                }
            }
            if (count == 0) {
                e = null;
            } else {
                e = dequeue();
            }
        }
        return e;
    }

    public E peek() {
        E e;
        synchronized (sync) {
            e = head == null ? null : head.item;
        }
        return e;
    }

    public boolean add(E e) {
        boolean added = offer(e);
        if (!added) {
            throw new IllegalStateException("Queue full");
        }
        return true;
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
            empty = count == 0;
        }
        return empty;
    }

    public int remainingCapacity() {
        int free;
        synchronized (sync) {
            free = capacity - count;
        }
        return free;
    }

    // Null-safe equality. Written as a helper with an explicit if/else because a
    // **boolean-valued** ternary (`o == null ? e == null : o.equals(e)`) is rejected by our
    // javac with "operando no numérico" — finding #109. Int- and reference-valued ternaries
    // are fine, so only this shape needs the rewrite.
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
            LbqNode<E> n = head;
            while (n != null) {
                Object e = n.item;
                if (eq(o, e)) {
                    found = true;
                }
                n = n.next;
            }
        }
        return found;
    }

    public boolean remove(Object o) {
        boolean removed = false;
        synchronized (sync) {
            LbqNode<E> prev = null;
            LbqNode<E> n = head;
            while (n != null && !removed) {
                Object e = n.item;
                if (eq(o, e)) {
                    if (prev == null) {
                        head = n.next;
                    } else {
                        prev.next = n.next;
                    }
                    if (n == tail) {
                        tail = prev;
                    }
                    count--;
                    removed = true;
                }
                prev = n;
                n = n.next;
            }
            if (removed) {
                sync.notifyAll();
            }
        }
        return removed;
    }

    public void clear() {
        synchronized (sync) {
            head = null;
            tail = null;
            count = 0;
            sync.notifyAll();
        }
    }

    // Iterates a snapshot taken under the monitor (see ArrayBlockingQueue.iterator).
    public Iterator<E> iterator() {
        Object[] snapshot;
        synchronized (sync) {
            snapshot = new Object[count];
            LbqNode<E> n = head;
            int i = 0;
            while (n != null) {
                snapshot[i] = n.item;
                i++;
                n = n.next;
            }
        }
        return new LbqItr<E>(snapshot);
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
    }
}

// One link of the queue: its element and the next link.
final class LbqNode<E> {

    final E item;
    LbqNode<E> next;

    LbqNode(E item) {
        this.item = item;
    }
}

// Snapshot iterator for LinkedBlockingQueue.
final class LbqItr<E> implements Iterator<E> {

    private final Object[] snapshot;
    private int cursor;

    LbqItr(Object[] snapshot) {
        this.snapshot = snapshot;
    }

    public boolean hasNext() {
        return cursor < snapshot.length;
    }

    public E next() {
        E e = (E) snapshot[cursor];
        cursor++;
        return e;
    }

}
