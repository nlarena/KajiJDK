package java.util.concurrent;

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

import java.io.Serializable;
import java.util.Iterator;

// A bounded blocking queue over a fixed circular array: producers park when it fills,
// consumers park when it drains, and one monitor coordinates both. This is the classic
// producer/consumer buffer — the bound is the point, since it applies backpressure instead
// of letting a fast producer exhaust memory.
//
// The JDK splits the waiting into two Conditions (notFull / notEmpty) over one lock; here a
// single monitor with notifyAll serves both roles: a waiter re-checks its own predicate on
// wake, so waking the other camp too is merely a spurious wakeup.
//
// Single-exit style throughout (finding #105).
public class ArrayBlockingQueue<E> implements BlockingQueue<E>, Serializable {

    private final Object sync = new Object();
    private final Object[] items;
    // Index of the next element to hand out, and of the next free slot.
    private int takeIndex;
    private int putIndex;
    private int count;

    public ArrayBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        this.items = new Object[capacity];
    }

    public ArrayBlockingQueue(int capacity, boolean fair) {
        this(capacity);
    }

    // Append at putIndex, wrapping around. Caller holds sync and has checked for space.
    private void enqueue(E e) {
        items[putIndex] = e;
        putIndex++;
        if (putIndex == items.length) {
            putIndex = 0;
        }
        count++;
        sync.notifyAll();
    }

    // Take from takeIndex, wrapping around. Caller holds sync and has checked for an item.
    private E dequeue() {
        E e = (E) items[takeIndex];
        items[takeIndex] = null;
        takeIndex++;
        if (takeIndex == items.length) {
            takeIndex = 0;
        }
        count--;
        sync.notifyAll();
        return e;
    }

    public void put(E e) {
        synchronized (sync) {
            while (count == items.length) {
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
            if (count == items.length) {
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
            if (count == items.length) {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    sync.wait(ms);
                }
            }
            if (count == items.length) {
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
            e = count == 0 ? null : (E) items[takeIndex];
        }
        return e;
    }

    // Collection.add: unlike offer, a full queue is an error rather than a false return.
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
            free = items.length - count;
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
            int i = takeIndex;
            for (int seen = 0; seen < count; seen++) {
                Object e = items[i];
                if (eq(o, e)) {
                    found = true;
                }
                i++;
                if (i == items.length) {
                    i = 0;
                }
            }
        }
        return found;
    }

    public boolean remove(Object o) {
        boolean removed = false;
        synchronized (sync) {
            int i = takeIndex;
            for (int seen = 0; seen < count; seen++) {
                Object e = items[i];
                if (!removed && eq(o, e)) {
                    // Compact the tail down over the removed slot, then drop the last one.
                    int from = i;
                    for (int rest = seen + 1; rest < count; rest++) {
                        int next = from + 1;
                        if (next == items.length) {
                            next = 0;
                        }
                        items[from] = items[next];
                        from = next;
                    }
                    items[from] = null;
                    putIndex = from;
                    count--;
                    removed = true;
                }
                i++;
                if (i == items.length) {
                    i = 0;
                }
            }
            if (removed) {
                sync.notifyAll();
            }
        }
        return removed;
    }

    public void clear() {
        synchronized (sync) {
            for (int i = 0; i < items.length; i++) {
                items[i] = null;
            }
            takeIndex = 0;
            putIndex = 0;
            count = 0;
            sync.notifyAll();
        }
    }

    // Iterates a snapshot taken under the monitor, so it can neither tear nor block writers.
    public Iterator<E> iterator() {
        Object[] snapshot;
        synchronized (sync) {
            snapshot = new Object[count];
            int i = takeIndex;
            for (int seen = 0; seen < count; seen++) {
                snapshot[seen] = items[i];
                i++;
                if (i == items.length) {
                    i = 0;
                }
            }
        }
        return new AbqItr<E>(snapshot);
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
    }
}

// Snapshot iterator for ArrayBlockingQueue: it walks the array copied out under the
// monitor, so it neither tears nor holds the queue locked while the caller iterates.
final class AbqItr<E> implements Iterator<E> {

    private final Object[] snapshot;
    private int cursor;

    AbqItr(Object[] snapshot) {
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
