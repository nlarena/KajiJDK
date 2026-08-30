package java.lang.ref;

import java.lang.ref.Reference; // redundante (mismo paquete); workaround del class-finder, ver WeakReference

// KajiLibrary's java.lang.ref.ReferenceQueue — the queue References are enqueued onto once
// their referent dies. A singly-linked stack threaded through Reference.next: the GC
// pushes cleared references, the program polls them off.
//
// Generic in the referent type `T`, as in the reference; the internal list (`head`, `poll0`) stays
// raw because the GC threads it by field name. The public accessors narrow to
// `Reference<? extends T>`, matching the JDK — the wildcard erases away, so the descriptors are
// unchanged.
public class ReferenceQueue<T> {

    Reference head;   // top of the pending list; the GC links newly-cleared references here

    // The unsynchronised pop, shared by poll and remove (which already hold the lock). Private, so
    // it is invisible to the surface accounting — as it is in the reference.
    private Reference poll0() {
        if (this.head == null) {
            return null;
        }
        Reference r = this.head;
        this.head = r.next;
        r.next = null;
        return r;
    }

    // Removes and returns the next pending reference, or null if the queue is empty.
    public Reference<? extends T> poll() {
        synchronized (this) {
            return poll0();
        }
    }

    // Blocks until a reference is available and returns it.
    public Reference<? extends T> remove() throws InterruptedException {
        return remove(0);
    }

    // Blocks up to {@code timeout} milliseconds (0 = indefinitely) for a reference, then returns
    // it, or null on timeout. The wait is taken in bounded slices and re-polls each time: the GC
    // links a cleared reference onto {@link #head} directly, without taking this monitor or
    // notifying it, so a plain unbounded wait could sleep through a GC-driven enqueue. A manual
    // {@link Reference#enqueue()} does notify, and wakes the waiter at once.
    public Reference<? extends T> remove(long timeout) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout value");
        }
        synchronized (this) {
            Reference r = poll0();
            if (r != null) {
                return r;
            }
            long deadline = timeout == 0 ? 0 : System.currentTimeMillis() + timeout;
            while (true) {
                long slice = 100;
                if (timeout != 0) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        return null;
                    }
                    if (remaining < slice) {
                        slice = remaining;
                    }
                }
                wait(slice);
                r = poll0();
                if (r != null) {
                    return r;
                }
            }
        }
    }
}
