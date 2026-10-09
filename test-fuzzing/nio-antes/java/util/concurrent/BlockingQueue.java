package java.util.concurrent;

import java.util.Collection;
import java.util.Queue;

// A queue that additionally knows how to *wait*: {@link #put} blocks while the queue is
// full, {@link #take} blocks while it is empty. That pair is the whole producer/consumer
// pattern — no polling, no sleep loops, no lost hand-offs.
public interface BlockingQueue<E> extends Queue<E> {

    // Insert, waiting for space if the queue is full.
    void put(E e) throws InterruptedException;

    // Remove and return the head, waiting for an element if the queue is empty.
    E take() throws InterruptedException;

    // Insert, waiting up to the timeout for space; reports whether it was inserted.
    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    // Remove and return the head, waiting up to the timeout; null if none arrived.
    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    // How many more elements this queue can accept without blocking.
    int remainingCapacity();

    /**
     * Moves every element that is available right now into {@code c}, and reports how many moved.
     *
     * <p>This is not sugar over a {@code poll} loop. Draining takes the queue's monitor **once** for
     * the whole batch, so a consumer that wants "everything queued so far" gets a consistent
     * snapshot instead of a set of elements interleaved with whatever producers added in between.
     * It also never blocks: an empty queue drains zero elements rather than waiting.
     *
     * @throws IllegalArgumentException if {@code c} is this queue -- draining into itself would not
     *         terminate
     */
    int drainTo(Collection<? super E> c);

    // The bounded form: at most `maxElements` move. What a consumer with a fixed batch size wants.
    int drainTo(Collection<? super E> c, int maxElements);
}
