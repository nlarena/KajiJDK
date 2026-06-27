import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

// Exercises java.lang.ref: a weakly-referenced object with NO strong reference dies on
// the next GC — the GC clears the WeakReference (get() → null) and enqueues it onto its
// ReferenceQueue. Result 11 iff both happened.
public class Weak {
    int v;
    Weak(int v) { this.v = v; }

    static int run() {
        ReferenceQueue q = new ReferenceQueue();
        // The Weak(42) is held ONLY by wr.referent (weak) — no strong root.
        WeakReference wr = new WeakReference(new Weak(42), q);
        System.gc();                              // referent unreachable → cleared + enqueued
        int cleared = (wr.get() == null) ? 1 : 0; // ifnull
        int enqueued = (q.poll() == wr) ? 1 : 0;  // if_acmp
        return cleared * 10 + enqueued;           // 11 iff both
    }
}
