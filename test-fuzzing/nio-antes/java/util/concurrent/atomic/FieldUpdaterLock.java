package java.util.concurrent.atomic;

// The one monitor every field updater takes. Package-private: not part of any contract, it exists
// only so the three `Atomic*FieldUpdater` classes can share a single critical section.
//
// Why one *global* lock and not the target object's own monitor. The updaters reach a field
// reflectively, and KajiJDK's VM offers no compare-and-swap seam on a reflective field (see the
// header of AtomicIntegerFieldUpdater), so the read-compare-write has to be made exclusive some
// other way. Locking `obj` itself would work but would be *observable*: user code that does
// `synchronized (obj) { obj.wait(); }` would block an updater that the real JDK would never block,
// and two mechanisms that the JDK keeps independent would start interfering. This lock is reachable
// from nowhere else, so nothing outside this package can contend for it, hold it, or wait on it.
//
// The cost is that all updater traffic serialises against all other updater traffic, even on
// unrelated fields. That is strictly *stronger* than the JDK's per-field CAS -- it forbids
// interleavings the JDK allows, and forbids none that the JDK forbids -- so no program can observe
// a result here that the JDK could not also produce. It is only slower, never wrong.
//
// Nothing that runs user code ever holds it: the compound operations (`getAndUpdate`,
// `accumulateAndGet`, ...) call the caller's function *outside* the critical section, in the retry
// loop, exactly as the JDK does. So a user function is free to touch another updater without
// deadlocking, and the monitor is reentrant anyway.
final class FieldUpdaterLock {

    static final Object MONITOR = new Object();

    private FieldUpdaterLock() {
    }
}
