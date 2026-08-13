package java.lang.ref;

// A soft reference: like a WeakReference, but the GC keeps the referent alive **while
// there is room**, and only clears it when memory is tight. The canonical "cache that
// gives its memory back rather than blowing the heap" reference.
//
// Our policy is deliberately deterministic instead of heuristic (see `SoftPolicy` in
// src/jvm/interpreter/gc.rs): the *cause* of the collection decides. A major collection
// triggered by memory pressure — occupancy over the line, out of space, or the
// allocation-rate projection — traces the referent weakly, so it is cleared (get() →
// null) and enqueued exactly like a weak one. Any other major collection, including an
// explicit `System.gc()`, traces the referent as a **strong** edge: a soft referent
// always survives an ordinary collection.
public class SoftReference extends Reference {
    public SoftReference(Object referent) {
        super(referent, null);
    }

    public SoftReference(Object referent, ReferenceQueue queue) {
        super(referent, queue);
    }
}
