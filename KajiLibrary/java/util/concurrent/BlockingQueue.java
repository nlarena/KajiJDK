package java.util.concurrent;

import java.util.Queue;

// A queue that additionally knows how to *wait*: {@link #put} blocks while the queue is
// full, {@link #take} blocks while it is empty. That pair is the whole producer/consumer
// pattern — no polling, no sleep loops, no lost hand-offs.
public interface BlockingQueue<E> extends Queue<E> {

    // Insert, waiting for space if the queue is full.
    void put(E e);

    // Remove and return the head, waiting for an element if the queue is empty.
    E take();

    // Insert, waiting up to the timeout for space; reports whether it was inserted.
    boolean offer(E e, long timeout, TimeUnit unit);

    // Remove and return the head, waiting up to the timeout; null if none arrived.
    E poll(long timeout, TimeUnit unit);

    // How many more elements this queue can accept without blocking.
    int remainingCapacity();
}
