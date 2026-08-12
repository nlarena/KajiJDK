package java.lang.ref;

import java.lang.ref.ReferenceQueue;

// KajiLibrary's java.lang.ref.Reference — the root of the reference types. Its `referent`
// is the **weak** field: the GC does not keep an object alive through it, and when the
// referent dies the GC clears this field (so get() returns null) and, if a queue was
// given, links this Reference into that queue. Raw (no generics) to match KajiJDK's model.
public abstract class Reference {

    Object referent;        // the weak target — first field (offset 8); GC-managed
    Reference next;         // link in the ReferenceQueue's pending list
    ReferenceQueue queue;   // the queue to enqueue onto when cleared (or null)

    Reference(Object referent, ReferenceQueue queue) {
        this.referent = referent;
        this.queue = queue;
    }

    // The referent, or null once the GC has cleared it.
    public Object get() {
        return this.referent;
    }

    // Clears this reference (does not enqueue). After this, get() returns null.
    public void clear() {
        this.referent = null;
    }
}
