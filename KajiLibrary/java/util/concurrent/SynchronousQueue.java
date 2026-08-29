package java.util.concurrent;

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;

// A blocking queue of capacity **zero**. Not "very small" — zero: there is no slot for an
// element to sit in, so every {@link #put} must be met by a {@link #take} and neither side
// completes until both are present. It is a *hand-off point* wearing a queue's interface.
//
// That is the whole lesson of the class, and the API states it bluntly: {@link #size} is
// always 0, {@link #isEmpty} always true, {@link #peek} always null, {@link #iterator}
// always empty, {@link #contains} always false, {@link #clear} a no-op. There is nothing to
// look at because nothing is ever *stored* — an element exists only during the instant of
// transfer, in the hands of the two threads making it.
//
// The point of a zero capacity is that it makes the hand-off itself the synchronization. A
// buffered queue decouples producer from consumer: the producer runs ahead and the queue
// absorbs the difference, so the producer learns nothing about the consumer's progress. Here
// the producer cannot proceed until a consumer has actually taken the item, so it is a
// rendezvous — an {@link Exchanger} in one direction, or a {@link CyclicBarrier} of two
// parties that also carries a value. It is exactly what a cached thread pool wants for its
// work queue: an offer that fails means "no idle worker is standing by", which is the signal
// to start another thread.
//
// The JDK uses a lock-free dual stack/queue of parked nodes. Here one monitor guards a
// single-item hand-off slot: a putter parks on the slot until its item is consumed, a taker
// parks until one appears. `putSeq` numbers the hand-offs, so a putter waits for **its own**
// item to be taken rather than merely for the slot to look empty — without it, a second
// putter refilling the slot would fool the first into thinking its transfer had completed.
//
// Subset: the `fair` constructor flag is accepted and ignored (our waiters are woken by
// notifyAll and re-check, so ordering is the scheduler's), and drainTo / toArray /
// containsAll are absent, as they are package-wide.
//
// Single-exit style throughout (finding #105).
public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {

    private final Object sync = new Object();
    // The element currently being handed over, and whether there is one. `item != null` would
    // do, but the explicit flag keeps the states readable.
    private Object item;
    private boolean hasItem;
    // Hand-offs completed plus the one in flight — the ticket a putter waits on.
    private long putSeq;
    // Takers parked in take()/poll(timeout) right now. The untimed offer() consults this:
    // it may only succeed if somebody is already waiting to receive.
    private int waitingTakers;

    public SynchronousQueue() {
    }

    public SynchronousQueue(boolean fair) {
    }

    // Put the item on the table and wake whoever is waiting. Caller holds sync and has
    // checked the table is free. Returns the ticket identifying this hand-off.
    private long place(E e) {
        item = e;
        hasItem = true;
        putSeq = putSeq + 1L;
        sync.notifyAll();
        return putSeq;
    }

    // Is the hand-off with the given ticket still waiting to be consumed? Caller holds sync.
    private boolean stillMine(long ticket) {
        boolean mine;
        if (hasItem) {
            mine = putSeq == ticket;
        } else {
            mine = false;
        }
        return mine;
    }

    // Consume the item on the table. Caller holds sync and has checked hasItem.
    private E consume() {
        E e = (E) item;
        item = null;
        hasItem = false;
        sync.notifyAll();
        return e;
    }

    // Hand `e` to a consumer, blocking until one takes it. Returning is the *proof* that a
    // taker received it — which is the guarantee no buffered queue can give.
    public void put(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        synchronized (sync) {
            // One hand-off at a time: wait for the table to clear first.
            while (hasItem) {
                sync.wait();
            }
            long ticket = place(e);
            while (stillMine(ticket)) {
                sync.wait();
            }
        }
    }

    // Insert only if a consumer is **already** waiting; otherwise report false at once. This
    // never blocks, which is what makes it the "is anyone free?" probe a thread pool uses.
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        boolean handed;
        synchronized (sync) {
            if (waitingTakers > 0 && !hasItem) {
                // A parked taker re-checks the slot on waking and will consume this, so the
                // transfer is committed even though we return before it physically happens.
                place(e);
                handed = true;
            } else {
                handed = false;
            }
        }
        return handed;
    }

    // Offer the item and wait up to the timeout for a consumer. If none arrives the item is
    // **retracted** — it was never in a queue, so there is nothing to leave behind.
    public boolean offer(E e, long timeout, TimeUnit unit) {
        if (e == null) {
            throw new NullPointerException();
        }
        boolean handed;
        synchronized (sync) {
            long ms = unit.toMillis(timeout);
            if (hasItem && ms > 0L) {
                // Somebody else's hand-off is in flight; give it a chance to finish.
                sync.wait(ms);
            }
            if (hasItem) {
                handed = false;
            } else {
                long ticket = place(e);
                if (ms > 0L && stillMine(ticket)) {
                    sync.wait(ms);
                }
                if (stillMine(ticket)) {
                    // Nobody came: take the item back off the table. Note there is
                    // deliberately **no** notifyAll here. We only got to place() because the
                    // table was free, and a putter can only be parked while the table is
                    // *occupied* — so at this instant the wait-set holds takers only, and a
                    // taker would have consumed the item rather than leave it for us to
                    // retract. Retracting therefore enables nobody's predicate.
                    item = null;
                    hasItem = false;
                    handed = false;
                } else {
                    handed = true;
                }
            }
        }
        return handed;
    }

    // Receive an item, blocking until a producer offers one.
    public E take() {
        E e;
        synchronized (sync) {
            waitingTakers = waitingTakers + 1;
            // Announce ourselves: this is what lets a non-blocking offer() succeed.
            sync.notifyAll();
            while (!hasItem) {
                sync.wait();
            }
            e = consume();
            waitingTakers = waitingTakers - 1;
        }
        return e;
    }

    // Take an item only if a producer is already blocked offering one; null otherwise.
    public E poll() {
        E e;
        synchronized (sync) {
            if (hasItem) {
                e = consume();
            } else {
                e = null;
            }
        }
        return e;
    }

    // Wait up to the timeout for a producer to appear; null if none does.
    public E poll(long timeout, TimeUnit unit) {
        E e;
        synchronized (sync) {
            waitingTakers = waitingTakers + 1;
            sync.notifyAll();
            if (!hasItem) {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    sync.wait(ms);
                }
            }
            // Re-checked after the wait, so an offer() that landed while we were parked (and
            // counted on us to receive it) is still honoured.
            if (hasItem) {
                e = consume();
            } else {
                e = null;
            }
            waitingTakers = waitingTakers - 1;
        }
        return e;
    }

    // --- everything below is the "there is no container" half of the contract ---

    // Always null: an element is never *held*, only passed from hand to hand.
    public E peek() {
        return null;
    }

    // Always 0, by definition of a zero-capacity queue.
    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    // Always 0 too — and note that this does *not* mean "full". remainingCapacity is the
    // number of elements that can be inserted without blocking, and here that is none.
    public int remainingCapacity() {
        return 0;
    }

    public void clear() {
    }

    public boolean contains(Object o) {
        return false;
    }

    public boolean remove(Object o) {
        return false;
    }

    public Iterator<E> iterator() {
        return new EmptyItr<E>();
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, 0);
    }
}

// The iterator over nothing. Top-level and package-private rather than nested, since a
// nested class inside a *generic* class is miscompiled (finding #13).
final class EmptyItr<E> implements Iterator<E> {

    public boolean hasNext() {
        return false;
    }

    public E next() {
        throw new NoSuchElementException();
    }

}
