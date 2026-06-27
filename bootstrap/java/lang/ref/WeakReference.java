package java.lang.ref;

// A weak reference: cleared by the GC as soon as the referent is not strongly
// reachable. The canonical "cache that doesn't prevent collection" reference.
public class WeakReference extends Reference {
    public WeakReference(Object referent) {
        super(referent, null);
    }

    public WeakReference(Object referent, ReferenceQueue queue) {
        super(referent, queue);
    }
}
