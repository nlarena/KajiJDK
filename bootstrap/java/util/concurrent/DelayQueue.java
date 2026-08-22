package java.util.concurrent;

// Minimal java.util.concurrent.DelayQueue — an **unbounded** blocking queue of `Delayed` elements
// that only releases one once its delay has elapsed, earliest-expiration first. A binary min-heap
// (ordered by the elements' `compareTo`, i.e. absolute expiration) lives under this object's
// monitor; `take` peeks the head and, if it isn't ready yet, does a **timed** `wait` for exactly its
// remaining delay — so it wakes right when the head expires, or earlier if a nearer element is put.
// (Simplified vs. the JDK: no leader/follower optimisation, no `poll(timeout)`/`drainTo`.)
public class DelayQueue<E extends Delayed> {
    private Object[] heap = new Object[16];
    private int size;

    public synchronized void put(E e) {
        if (size == heap.length) {
            grow();
        }
        heap[size] = e;
        siftUp(size);
        size = size + 1;
        notifyAll(); // a new (maybe earlier-expiring) head — wake a waiting taker to re-evaluate
    }

    @SuppressWarnings("unchecked")
    public synchronized E take() throws InterruptedException {
        for (;;) {
            if (size == 0) {
                wait();
                continue;
            }
            E head = (E) heap[0];
            long delayNs = head.getDelay();
            if (delayNs <= 0) {
                return poll();
            }
            long ms = delayNs / 1000000L;
            wait(ms <= 0 ? 1 : ms); // wait out the head's remaining delay (1 ms floor for sub-ms)
        }
    }

    public synchronized int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    private E poll() {
        E top = (E) heap[0];
        size = size - 1;
        heap[0] = heap[size];
        heap[size] = null;
        if (size > 0) {
            siftDown(0);
        }
        return top;
    }

    private int compare(Object a, Object b) {
        return ((Delayed) a).compareTo((Delayed) b);
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
