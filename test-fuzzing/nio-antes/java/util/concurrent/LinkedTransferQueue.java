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

// An unbounded queue that can also act as a rendezvous. Used through `put`/`offer` it is an
// ordinary linked FIFO that never blocks a producer; used through `transfer` the producer
// waits until a consumer has actually *taken* the element. One structure, two disciplines,
// and the producer picks per call.
//
// The mechanism is one flag per node. `taken` is set by whichever consumer removes the
// element, and a transferring producer waits on it; a producer that merely put the element
// there never looks at it. That is why `transfer` and `put` can share a single chain
// instead of needing the JDK's dual-mode queue of matched and unmatched nodes.
//
// `waitingConsumers` is kept so `tryTransfer` can answer immediately: with nobody waiting
// there is no one to hand to, so it declines rather than enqueueing. It is also what
// `hasWaitingConsumer` and `getWaitingConsumerCount` report — an estimate, as the JDK says,
// since the count can change the instant the monitor is released.
//
// Everything is guarded by one monitor. No `throws InterruptedException`: restating a
// compiled superinterface's throws clause is rejected (finding #104), and the descriptor is
// the same without it.
public class LinkedTransferQueue<E> extends AbstractQueue<E> implements TransferQueue<E>, Serializable {

    private final Object lock = new Object();
    private LtqNode<E> head;
    private LtqNode<E> tail;
    private int count;
    private int waitingConsumers;

    public LinkedTransferQueue() {
    }

    public LinkedTransferQueue(Collection<? extends E> c) {
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            E e = it.next();
            if (e == null) {
                throw new NullPointerException();
            }
            synchronized (lock) {
                enqueue(e);
            }
        }
    }

    // ------------------------------------------------------------ chain, lock held

    private LtqNode<E> enqueue(E e) {
        LtqNode<E> node = new LtqNode<E>(e);
        node.prev = tail;
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        count = count + 1;
        lock.notifyAll();
        return node;
    }

    private void unlink(LtqNode<E> node) {
        LtqNode<E> p = node.prev;
        LtqNode<E> n = node.next;
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

    // Remove the head and mark it taken, which is what releases a waiting transferrer.
    private E dequeue() {
        E value = null;
        if (head != null) {
            LtqNode<E> node = head;
            value = node.item;
            unlink(node);
            node.taken = true;
            lock.notifyAll();
        }
        return value;
    }

    private void await(long millis) {
        try {
            if (millis <= 0L) {
                lock.wait();
            } else {
                lock.wait(millis);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("interrupted while waiting on the queue");
        }
    }

    // ------------------------------------------------------------ producing

    // Unbounded: there is never a reason to block a producer that is not transferring.
    public void put(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            enqueue(e);
        }
    }

    public boolean offer(E e) {
        put(e);
        return true;
    }

    public boolean offer(E e, long timeout, TimeUnit unit) {
        put(e);
        return true;
    }

    public boolean add(E e) {
        put(e);
        return true;
    }

    // ------------------------------------------------------------ transferring

    // Hand off only if somebody is already waiting; otherwise decline without enqueueing.
    public boolean tryTransfer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        boolean handed = false;
        synchronized (lock) {
            if (waitingConsumers > 0) {
                LtqNode<E> node = enqueue(e);
                while (!node.taken) {
                    await(0L);
                }
                handed = true;
            }
        }
        return handed;
    }

    // Enqueue and wait for a consumer, however long it takes.
    public void transfer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            LtqNode<E> node = enqueue(e);
            while (!node.taken) {
                await(0L);
            }
        }
    }

    // Enqueue and wait up to the timeout; on expiry the element is pulled back out, so a
    // failed tryTransfer leaves the queue exactly as it found it.
    public boolean tryTransfer(E e, long timeout, TimeUnit unit) {
        if (e == null) {
            throw new NullPointerException();
        }
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        boolean handed;
        synchronized (lock) {
            LtqNode<E> node = enqueue(e);
            long remaining = millis;
            while (!node.taken && remaining > 0L) {
                await(remaining);
                remaining = deadline - System.currentTimeMillis();
            }
            handed = node.taken;
            if (!handed) {
                unlink(node);
            }
        }
        return handed;
    }

    // ------------------------------------------------------------ consuming

    public E take() {
        E value;
        synchronized (lock) {
            waitingConsumers = waitingConsumers + 1;
            lock.notifyAll();
            value = dequeue();
            while (value == null) {
                await(0L);
                value = dequeue();
            }
            waitingConsumers = waitingConsumers - 1;
        }
        return value;
    }

    public E poll() {
        E value;
        synchronized (lock) {
            value = dequeue();
        }
        return value;
    }

    public E poll(long timeout, TimeUnit unit) {
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        E value;
        synchronized (lock) {
            waitingConsumers = waitingConsumers + 1;
            lock.notifyAll();
            value = dequeue();
            long remaining = millis;
            while (value == null && remaining > 0L) {
                await(remaining);
                value = dequeue();
                remaining = deadline - System.currentTimeMillis();
            }
            waitingConsumers = waitingConsumers - 1;
        }
        return value;
    }

    public E peek() {
        E value = null;
        synchronized (lock) {
            if (head != null) {
                value = head.item;
            }
        }
        return value;
    }

    // ------------------------------------------------------------ consumer census

    public boolean hasWaitingConsumer() {
        return getWaitingConsumerCount() > 0;
    }

    public int getWaitingConsumerCount() {
        int n;
        synchronized (lock) {
            n = waitingConsumers;
        }
        return n;
    }

    // ------------------------------------------------------------ Collection view

    public int size() {
        int n;
        synchronized (lock) {
            n = count;
        }
        return n;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    // Unbounded, so there is always room. Integer.MAX_VALUE is the JDK's way of saying so.
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    public boolean contains(Object o) {
        boolean found = false;
        if (o != null) {
            synchronized (lock) {
                LtqNode<E> p = head;
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

    public boolean remove(Object o) {
        boolean removed = false;
        if (o != null) {
            synchronized (lock) {
                LtqNode<E> p = head;
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

    public void clear() {
        synchronized (lock) {
            head = null;
            tail = null;
            count = 0;
            lock.notifyAll();
        }
    }

    public int drainTo(Collection<? super E> c) {
        return drainInto(c, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        return drainInto(c, maxElements);
    }

    // Shared body over a raw Collection: handing a `Collection<? super E>` parameter to
    // another `Collection<? super E>` parameter is rejected here, so the capture is dropped
    // at this one boundary.
    private int drainInto(Collection sink, int maxElements) {
        if (sink == this) {
            throw new IllegalArgumentException("cannot drain a queue into itself");
        }
        int moved = 0;
        synchronized (lock) {
            while (moved < maxElements && head != null) {
                E value = dequeue();
                sink.add(value);
                moved = moved + 1;
            }
        }
        return moved;
    }

    public boolean removeIf(Predicate<? super E> filter) {
        boolean changed = false;
        synchronized (lock) {
            LtqNode<E> p = head;
            while (p != null) {
                LtqNode<E> next = p.next;
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
            LtqNode<E> p = head;
            while (p != null) {
                LtqNode<E> next = p.next;
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
            LtqNode<E> p = head;
            while (p != null) {
                LtqNode<E> next = p.next;
                if (!c.contains(p.item)) {
                    unlink(p);
                    changed = true;
                }
                p = next;
            }
        }
        return changed;
    }

    public Object[] toArray() {
        Object[] a;
        synchronized (lock) {
            a = new Object[count];
            LtqNode<E> p = head;
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

    // Walks a snapshot, so it is weakly consistent: never a ConcurrentModificationException,
    // never the same element twice.
    public Iterator<E> iterator() {
        return new LtqItr<E>(toArray());
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
    }
}

// A queued element plus the one bit that makes a transfer possible: whether a consumer has
// taken it.
final class LtqNode<E> {

    final E item;
    LtqNode<E> prev;
    LtqNode<E> next;
    boolean taken;

    LtqNode(E item) {
        this.item = item;
    }
}

final class LtqItr<E> implements Iterator<E> {

    private final Object[] items;
    private int cursor;

    LtqItr(Object[] items) {
        this.items = items;
    }

    public boolean hasNext() {
        return cursor < items.length;
    }

    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Object value = items[cursor];
        cursor = cursor + 1;
        return (E) value;
    }

}
