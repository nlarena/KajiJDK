package java.lang.ref;

// Minimal java.lang.ref.Reference — the root of the reference types. Its `referent`
// is the **weak** field: the GC does not keep an object alive through it, and when the
// referent dies the GC clears this field (so get() returns null) and, if a queue was
// given, links this Reference into that queue. Raw (no generics) for our model.
public abstract class Reference {
    Object referent;        // the weak target — offset 8 (first field); GC-managed
    Reference next;         // link in the ReferenceQueue's pending list
    ReferenceQueue queue;   // the queue to enqueue onto when cleared (or null)

    Reference(Object referent, ReferenceQueue queue) {
        this.referent = referent;
        this.queue = queue;
    }

    public Object get() {
        return referent;
    }

    public void clear() {
        referent = null;
    }
}
