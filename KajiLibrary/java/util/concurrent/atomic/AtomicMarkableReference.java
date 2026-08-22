package java.util.concurrent.atomic;

// A reference paired with a mark bit, updatable atomically together. The JDK keeps
// the (reference, mark) couple in one immutable holder so a CAS swaps both at once;
// on the single carrier two fields under the monitor give the same all-or-nothing
// update.
public class AtomicMarkableReference<V> {

    private V reference;
    private boolean mark;

    public AtomicMarkableReference(V initialRef, boolean initialMark) {
        reference = initialRef;
        mark = initialMark;
    }

    public V getReference() {
        return reference;
    }

    public boolean isMarked() {
        return mark;
    }

    public synchronized V get(boolean[] markHolder) {
        markHolder[0] = mark;
        return reference;
    }

    public synchronized boolean compareAndSet(V expectedReference, V newReference,
                                              boolean expectedMark, boolean newMark) {
        if (reference == expectedReference && mark == expectedMark) {
            reference = newReference;
            mark = newMark;
            return true;
        }
        return false;
    }

    public boolean weakCompareAndSet(V expectedReference, V newReference,
                                     boolean expectedMark, boolean newMark) {
        return compareAndSet(expectedReference, newReference, expectedMark, newMark);
    }

    public synchronized void set(V newReference, boolean newMark) {
        reference = newReference;
        mark = newMark;
    }

    public synchronized boolean attemptMark(V expectedReference, boolean newMark) {
        if (reference == expectedReference) {
            mark = newMark;
            return true;
        }
        return false;
    }
}
