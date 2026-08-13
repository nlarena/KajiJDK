import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

// Exercises the two reference strengths added on top of WeakReference.
//
//   PhantomReference — get() is null ALWAYS (spec), even while the referent is strongly
//   reachable; the reference is enqueued once the referent becomes unreachable.
//   SoftReference    — the referent SURVIVES an ordinary (explicit) collection: our
//                      policy only clears soft referents on a pressure-triggered major.
//
// The score adds up to exactly 42 iff every one of the six observations holds.
// Own box class (no autoboxing) so the test is about references, not about Integer.
public class RfTest {
    // A *static* field is a GC root through the class mirror — the sharp, explicit way to
    // hold and then drop a strong reference (a stale local slot is not reliably cleared).
    static RfBox keep;

    static int run() {
        int score = 0;
        ReferenceQueue q = new ReferenceQueue();

        keep = new RfBox(7);
        PhantomReference pr = new PhantomReference(keep, q);
        // 1. Phantom get() is null even now, with the referent strongly reachable.
        if (pr.get() == null) {
            score += 10;
        }

        // A soft referent with no other root at all: only sr.referent points at it.
        SoftReference sr = new SoftReference(new RfBox(9));

        System.gc(); // an ORDINARY (explicit) major collection

        // 2. Still null — get() never hands the referent back.
        if (pr.get() == null) {
            score += 10;
        }
        // 3. NOT enqueued: `keep` still holds the referent strongly.
        if (q.poll() == null) {
            score += 6;
        }
        // 4. The soft referent survived: no memory pressure, so it was traced strongly.
        if (sr.get() != null) {
            score += 6;
        }

        keep = null; // drop the only strong reference to the RfBox(7)
        System.gc(); // now the phantom referent is unreachable

        // 5. Null, as always.
        if (pr.get() == null) {
            score += 5;
        }
        // 6. The phantom reference is now on its queue.
        if (q.poll() == pr) {
            score += 5;
        }

        return score; // 10 + 10 + 6 + 6 + 5 + 5 = 42
    }
}

// The payload. `v` exists only so the box is a real object with a field.
class RfBox {
    int v;

    RfBox(int v) {
        this.v = v;
    }
}
