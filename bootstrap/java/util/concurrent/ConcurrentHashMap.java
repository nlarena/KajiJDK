package java.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

// Minimal java.util.concurrent.ConcurrentHashMap — a thread-safe hash map by **separate chaining**
// (a fixed bucket array of linked `Node`s) with **lock striping**: the buckets are partitioned
// across a small set of `ReentrantLock`s, so operations on buckets in different stripes proceed in
// parallel (unlike a single-lock synchronized map). `put`/`get`/`remove` hash the key, pick the
// bucket, and lock only that bucket's stripe. Size is an `AtomicInteger` (incremented across
// stripes). Keys use their own `hashCode()`/`equals()` (virtual dispatch). (Simplified vs. the JDK:
// **fixed capacity — no resize**, and reads also take the stripe lock rather than being lock-free;
// null keys/values aren't supported.)
public class ConcurrentHashMap<K, V> {
    private static final int BUCKETS = 64; // power of two, fixed (no resize)
    private static final int STRIPES = 16; // power of two lock stripes

    private static final class Node {
        final Object key;
        final int hash;
        Object value;
        Node next;

        Node(Object key, int hash, Object value, Node next) {
            this.key = key;
            this.hash = hash;
            this.value = value;
            this.next = next;
        }
    }

    private final Node[] table = new Node[BUCKETS];
    private final ReentrantLock[] locks = new ReentrantLock[STRIPES];
    private final AtomicInteger count = new AtomicInteger(0);

    public ConcurrentHashMap() {
        for (int i = 0; i < STRIPES; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    // Spread the high bits down, like the JDK — a weak hashCode still scatters across buckets.
    private static int spread(int h) {
        return h ^ (h >>> 16);
    }

    private ReentrantLock lockFor(int bucket) {
        return locks[bucket & (STRIPES - 1)];
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        int h = spread(key.hashCode());
        int b = h & (BUCKETS - 1);
        ReentrantLock lock = lockFor(b);
        lock.lock();
        try {
            Node node = table[b];
            while (node != null) {
                if (node.hash == h && node.key.equals(key)) {
                    Object old = node.value;
                    node.value = value; // replace the value for an existing key
                    return (V) old;
                }
                node = node.next;
            }
            table[b] = new Node(key, h, value, table[b]); // prepend a new entry
            count.incrementAndGet();
            return null;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        int h = spread(key.hashCode());
        int b = h & (BUCKETS - 1);
        ReentrantLock lock = lockFor(b);
        lock.lock();
        try {
            Node node = table[b];
            while (node != null) {
                if (node.hash == h && node.key.equals(key)) {
                    return (V) node.value;
                }
                node = node.next;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        int h = spread(key.hashCode());
        int b = h & (BUCKETS - 1);
        ReentrantLock lock = lockFor(b);
        lock.lock();
        try {
            Node prev = null;
            Node node = table[b];
            while (node != null) {
                if (node.hash == h && node.key.equals(key)) {
                    if (prev == null) {
                        table[b] = node.next;
                    } else {
                        prev.next = node.next;
                    }
                    count.decrementAndGet();
                    return (V) node.value;
                }
                prev = node;
                node = node.next;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public int size() {
        return count.get();
    }
}
