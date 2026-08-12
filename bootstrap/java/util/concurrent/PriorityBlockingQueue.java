package java.util.concurrent;

import java.util.Comparator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Minimal java.util.concurrent.PriorityBlockingQueue — an **unbounded** blocking queue whose
// elements come out in **priority order**, backed by a binary **min-heap** (array). One
// `ReentrantLock` guards the heap; a `notEmpty` Condition parks takers while it is empty (there is
// no `notFull` — it is unbounded, so `put` never blocks). Ordering is a supplied `Comparator`, or
// the elements' natural order (`Comparable`) if none is given. (Simplified vs. the JDK: no
// `Collection` bulk ops / `drainTo` / timed poll; growth doubles the array.)
public class PriorityBlockingQueue<E> {
    private Object[] heap;
    private int size;
    private final Comparator<E> comparator; // null → natural ordering (elements are Comparable)

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public PriorityBlockingQueue() {
        this(16, null);
    }

    public PriorityBlockingQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    public PriorityBlockingQueue(int initialCapacity, Comparator<E> comparator) {
        this.heap = new Object[initialCapacity < 1 ? 1 : initialCapacity];
        this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
    private int compare(Object a, Object b) {
        if (comparator != null) {
            return comparator.compare((E) a, (E) b);
        }
        return ((Comparable<E>) a).compareTo((E) b);
    }

    // Insert `e`. Unbounded, so this never blocks; it wakes one waiting taker.
    public void put(E e) {
        lock.lock();
        try {
            if (size == heap.length) {
                grow();
            }
            heap[size] = e;
            siftUp(size);
            size = size + 1;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    // Remove and return the least element, waiting while the queue is empty.
    @SuppressWarnings("unchecked")
    public E take() throws InterruptedException {
        lock.lock();
        try {
            while (size == 0) {
                notEmpty.await();
            }
            Object top = heap[0];
            size = size - 1;
            heap[0] = heap[size];
            heap[size] = null;
            if (size > 0) {
                siftDown(0);
            }
            return (E) top;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }

    private void grow() {
        Object[] bigger = new Object[heap.length * 2];
        for (int i = 0; i < size; i++) {
            bigger[i] = heap[i];
        }
        heap = bigger;
    }

    private void siftUp(int k) {
        while (k > 0) {
            int parent = (k - 1) / 2;
            if (compare(heap[k], heap[parent]) >= 0) {
                break;
            }
            swap(k, parent);
            k = parent;
        }
    }

    private void siftDown(int k) {
        for (;;) {
            int left = 2 * k + 1;
            int right = 2 * k + 2;
            int smallest = k;
            if (left < size && compare(heap[left], heap[smallest]) < 0) {
                smallest = left;
            }
            if (right < size && compare(heap[right], heap[smallest]) < 0) {
                smallest = right;
            }
            if (smallest == k) {
                break;
            }
            swap(k, smallest);
            k = smallest;
        }
    }

    private void swap(int i, int j) {
        Object t = heap[i];
        heap[i] = heap[j];
        heap[j] = t;
    }
}
