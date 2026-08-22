package java.util.concurrent.atomic;

// A reference paired with an integer stamp, updatable atomically together — the
// classic guard against the ABA problem. Two fields under the monitor give the
// all-or-nothing update the JDK gets from a single immutable (reference, stamp) holder.
public class AtomicStampedReference<V> {

    private V reference;
    private int stamp;

    public AtomicStampedReference(V initialRef, int initialStamp) {
        reference = initialRef;
        stamp = initialStamp;
    }

    public V getReference() {
        return reference;
    }

    public int getStamp() {
        return stamp;
    }

    public synchronized V get(int[] stampHolder) {
        stampHolder[0] = stamp;
        return reference;
    }

    public synchronized boolean compareAndSet(V expectedReference, V newReference,
                                              int expectedStamp, int newStamp) {
        if (reference == expectedReference && stamp == expectedStamp) {
            reference = newReference;
            stamp = newStamp;
            return true;
        }
        return false;
    }

    public boolean weakCompareAndSet(V expectedReference, V newReference,
                                     int expectedStamp, int newStamp) {
        return compareAndSet(expectedReference, newReference, expectedStamp, newStamp);
    }

    public synchronized void set(V newReference, int newStamp) {
        reference = newReference;
        stamp = newStamp;
    }

    public synchronized boolean attemptStamp(V expectedReference, int newStamp) {
        if (reference == expectedReference) {
            stamp = newStamp;
            return true;
        }
        return false;
    }
}
