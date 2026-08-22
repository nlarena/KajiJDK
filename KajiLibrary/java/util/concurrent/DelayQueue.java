package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.PriorityQueue;

// A queue whose head is not merely the *smallest* element but the *soonest due* one — and
// which refuses to hand it over until it is actually due. Elements implement {@link Delayed},
// so each one knows how long remains before it becomes available, and the queue orders them
// by that remaining delay.
//
// Two rules define it, and the second is the interesting one:
//
//   1. Ordering: the head is the element with the smallest remaining delay. That is a
//      priority queue, and this class delegates it to {@link java.util.PriorityQueue}, whose
//      heap already does the work — Delayed extends Comparable<Delayed>, so the elements
//      order themselves.
//
//   2. Availability: {@link #poll} returns null and {@link #take} keeps waiting while the
//      head's delay has not elapsed, **even though the queue is not empty**. This is what
//      separates a DelayQueue from a PriorityBlockingQueue of due-times: {@link #size}
//      counts every element, expired or not, but only expired ones can come out.
//
// Together those make it a *scheduler*. A ScheduledThreadPoolExecutor is little more than a
// worker loop over take() on a DelayQueue of tasks that report their time-until-due; caches
// with expiry and connection pools with idle timeouts are the same shape.
//
// The waiting is the part worth reading. A taker facing an unexpired head does not spin and
// does not sleep blindly: it waits on the monitor for **exactly** the head's remaining delay,
// so it wakes when the element becomes due — and a notifyAll from an insert cuts the wait
// short, which is necessary because the new element may be due sooner than the one we were
// timing. The JDK adds a `leader` optimisation so that only one taker times the head while
// the rest wait indefinitely; we let every taker time it, which costs redundant wakeups but
// cannot miss a due element.
//
// Delays are read in NANOSECONDS, not milliseconds: a sub-millisecond delay truncated to
// millis would read as zero and the element would come out early. The unit is obtained via
// `TimeUnit.valueOf("NANOSECONDS")` rather than the constant — reading a static field of
// another compiled class traps at run time, but a static *method* is fine (finding #110), and
// the enum's generated valueOf resolves the constant inside TimeUnit itself.
//
// Single-exit style throughout (finding #105).
public class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {

    private final Object sync = new Object();
    // Ordered by remaining delay, which is what Delayed's compareTo is required to compare.
    private final PriorityQueue<E> heap;

    public DelayQueue() {
        heap = new PriorityQueue<E>();
    }

    public DelayQueue(Collection<? extends E> c) {
        heap = new PriorityQueue<E>(c);
    }

    // The unit delays are measured in here. A method rather than a constant, per #110 above.
    private static TimeUnit nanos() {
        return TimeUnit.valueOf("NANOSECONDS");
    }

    // Nanoseconds until the head becomes available: negative or zero means it is due now, and
    // a very large value stands in for "the queue is empty". Caller holds sync.
    //
    // The head is bound to a `Delayed` local before getDelay is called: invoking a method on a
    // receiver whose static type is a TYPE VARIABLE is silently deleted by our javac and the
    // argument returned in its place (finding #111), which here would have meant reading the
    // TimeUnit back as if it were a delay.
    private long headDelay() {
        long remaining;
        Delayed head = (Delayed) heap.peek();
        if (head == null) {
            remaining = 9223372036854775807L;
        } else {
            remaining = head.getDelay(nanos());
        }
        return remaining;
    }

    // Always succeeds — the queue is unbounded, exactly like PriorityBlockingQueue. The
    // notifyAll matters more than usual: the new element may be due sooner than the one every
    // parked taker is currently timing, so they must re-read the head.
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

    // Never blocks: there is no capacity limit, so a producer is only ever delayed by the
    // monitor itself.
    public void put(E e) {
        offer(e);
    }

    // The timeout is unreachable for the same reason; it is ignored.
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    // Wait until the head is due, then take it. An empty queue waits indefinitely; a
    // non-empty one waits out the head's remaining delay, re-reading the head after every
    // wake because an insert may have changed which element is next.
    public E take() {
        E e = null;
        synchronized (sync) {
            boolean taken = false;
            while (!taken) {
                long remaining = headDelay();
                if (remaining <= 0L) {
                    e = heap.poll();
                    taken = true;
                } else if (heap.size() == 0) {
                    sync.wait();
                } else {
                    // Round *up* to the next millisecond: rounding down would wake a hair
                    // early and spin through the remainder one wakeup at a time.
                    long ms = (remaining + 999999L) / 1000000L;
                    if (ms <= 0L) {
                        ms = 1L;
                    }
                    sync.wait(ms);
                }
            }
        }
        return e;
    }

    // The head if it is due **now**, null otherwise — including when the queue holds plenty
    // of elements that are all still pending. That asymmetry with size() is the class.
    public E poll() {
        E e;
        synchronized (sync) {
            if (headDelay() <= 0L) {
                e = heap.poll();
            } else {
                e = null;
            }
        }
        return e;
    }

    // Wait up to the timeout for the head to come due. Waits for whichever is shorter, the
    // caller's timeout or the head's remaining delay, then checks once more.
    public E poll(long timeout, TimeUnit unit) {
        E e;
        synchronized (sync) {
            long remaining = headDelay();
            if (remaining > 0L) {
                long waitMs = unit.toMillis(timeout);
                long dueMs = (remaining + 999999L) / 1000000L;
                if (dueMs < waitMs) {
                    waitMs = dueMs;
                }
                if (waitMs > 0L) {
                    sync.wait(waitMs);
                }
            }
            if (headDelay() <= 0L) {
                e = heap.poll();
            } else {
                e = null;
            }
        }
        return e;
    }

    // The head **whether or not it is due** — peek inspects the ordering, it does not enforce
    // availability. The JDK documents the same, and it is how you ask "what is next, and when".
    public E peek() {
        E e;
        synchronized (sync) {
            e = heap.peek();
        }
        return e;
    }

    // Counts every element, expired and unexpired alike: a DelayQueue that is not empty may
    // still have nothing to give.
    public int size() {
        int n;
        synchronized (sync) {
            n = heap.size();
        }
        return n;
    }

    // Unbounded. Spelled out rather than read from Integer.MAX_VALUE (finding #110).
    public int remainingCapacity() {
        return 2147483647;
    }

    // Removes regardless of whether the element has expired — the one operation that can take
    // an unexpired element out, and it is O(n), since a heap can only find its minimum.
    public boolean remove(Object o) {
        boolean removed;
        synchronized (sync) {
            removed = heap.remove(o);
            if (removed) {
                sync.notifyAll();
            }
        }
        return removed;
    }

    public void clear() {
        synchronized (sync) {
            heap.clear();
        }
    }

    // A snapshot in heap order, expired and unexpired elements together.
    public Iterator<E> iterator() {
        Object[] snapshot;
        synchronized (sync) {
            snapshot = new Object[heap.size()];
            Iterator<E> it = heap.iterator();
            int i = 0;
            while (it.hasNext()) {
                snapshot[i] = it.next();
                i = i + 1;
            }
        }
        return new AbqItr<E>(snapshot);
    }
}
