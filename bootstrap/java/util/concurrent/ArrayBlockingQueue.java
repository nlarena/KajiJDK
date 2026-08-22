package java.util.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Minimal java.util.concurrent.ArrayBlockingQueue — a **bounded**, FIFO blocking queue backed by a
// fixed array. The classic two-condition producer/consumer: one `ReentrantLock` guards the state,
// `notEmpty` parks takers while the queue is empty and `notFull` parks putters while it is full;
// each mutating op signals the opposite condition. `put`/`take` block; the circular `putIndex`/
// `takeIndex` give FIFO order without shifting elements. Sits entirely on the AQS-based
// `ReentrantLock` + `Condition` (H6 core), so it inherits their `green≡os-gil≡os` correctness.
public class ArrayBlockingQueue<E> {
    private final Object[] items;
    private int takeIndex;
    private int putIndex;
    private int count;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public ArrayBlockingQueue(int capacity) {
        this.items = new Object[capacity];
    }

    // Insert `e`, waiting on `notFull` while the queue is full. Wakes one waiting taker.
    public void put(E e) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await();
            }
            items[putIndex] = e;
            putIndex = putIndex + 1;
            if (putIndex == items.length) {
                putIndex = 0;
            }
            count = count + 1;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    // Remove and return the head, waiting on `notEmpty` while the queue is empty. Wakes one putter.
    @SuppressWarnings("unchecked")
    public E take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            Object x = items[takeIndex];
            items[takeIndex] = null;
            takeIndex = takeIndex + 1;
            if (takeIndex == items.length) {
                takeIndex = 0;
            }
            count = count - 1;
            notFull.signal();
            return (E) x;
        } finally {
            lock.unlock();
        }
    }

    // Current element count (under the lock, so it is a consistent snapshot).
    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    // Free slots remaining before `put` would block.
    public int remainingCapacity() {
        lock.lock();
        try {
            return items.length - count;
        } finally {
            lock.unlock();
        }
    }
}
