package java.lang.ref;

import java.lang.ref.ReferenceQueue;

// KajiLibrary's java.lang.ref.Reference — the root of the reference types. Its `referent`
// is the **weak** field: the GC does not keep an object alive through it, and when the
// referent dies the GC clears this field (so get() returns null) and, if a queue was
// given, links this Reference into that queue.
//
// Generic in the referent type `T`, as in the reference — so `WeakReference<String>.get()`
// returns a `String`. The type variable is UNBOUNDED, so it erases to `Object`: the class-file
// descriptors are identical to a raw modelling (which is why the GC, which reads `referent` by
// field name as an `Object`, is untouched), while the generic signatures now match the JDK's and
// generic client code compiles against them.
public abstract class Reference<T> {

    Object referent;        // the weak target — first field (offset 8); GC-managed, kept as Object
    Reference next;         // link in the ReferenceQueue's pending list
    ReferenceQueue queue;   // the queue to enqueue onto when cleared (or null)

    Reference(T referent, ReferenceQueue<? super T> queue) {
        this.referent = referent;
        this.queue = queue;
    }

    // The referent, or null once the GC has cleared it. The cast is unchecked but safe: only a
    // `T` was ever stored (through the constructor), and the GC only ever writes null.
    public T get() {
        return (T) this.referent;
    }

    // Clears this reference (does not enqueue). After this, get() returns null.
    public void clear() {
        this.referent = null;
    }

    // Whether the referent is (still) this object, WITHOUT strengthening it. The reference identity
    // test is the whole of it. `final` as in the reference — a subclass must not weaken it.
    public final boolean refersTo(T obj) {
        return this.referent == obj;
    }

    // Adds this reference to its queue, if it has one and is not already there. Returns whether it
    // was actually enqueued. The GC does this on its own for a cleared referent (pushing straight
    // onto the queue's list); this is the manual path (and what {@link PhantomReference}-based
    // cleanup leans on). Membership is read off the queue's list rather than a private flag, so it
    // agrees with the GC's own enqueuing, which never touches such a flag.
    public boolean enqueue() {
        ReferenceQueue q = this.queue;
        if (q == null) {
            return false;
        }
        synchronized (q) {
            Reference r = q.head;
            while (r != null) {
                if (r == this) {
                    return false;   // already queued
                }
                r = r.next;
            }
            this.referent = null;
            this.next = q.head;
            q.head = this;
            q.notifyAll();
        }
        return true;
    }

    // Whether this reference currently sits in its queue's pending list. Deprecated in the
    // reference for being racy; kept for source compatibility.
    public boolean isEnqueued() {
        ReferenceQueue q = this.queue;
        if (q == null) {
            return false;
        }
        synchronized (q) {
            Reference r = q.head;
            while (r != null) {
                if (r == this) {
                    return true;
                }
                r = r.next;
            }
        }
        return false;
    }

    // A fence that keeps {@code ref} strongly reachable up to this call. KajiJDK's collector does
    // not hoist liveness analysis across a call boundary, so there is nothing to prevent here; the
    // method exists so code written against the reference compiles and behaves.
    public static void reachabilityFence(Object ref) {
    }
}
