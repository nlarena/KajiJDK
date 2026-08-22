package java.lang.ref;

// A phantom reference: the weakest of the three, and the only one whose get() is
// specified to return **null always** — even while the referent is still strongly
// reachable. That is the point: a phantom reference can never resurrect its referent,
// it can only tell you (through its ReferenceQueue) that the referent became
// unreachable. A queue is therefore mandatory — a phantom with no queue is useless.
//
// The inherited `referent` field still holds the object, because that is how the GC
// detects the death; the override below just refuses to hand it back.
public class PhantomReference extends Reference {
    public PhantomReference(Object referent, ReferenceQueue queue) {
        super(referent, queue);
    }

    // Always null, by spec (§java.lang.ref.PhantomReference#get).
    public Object get() {
        return null;
    }
}
