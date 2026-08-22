package java.util.concurrent.locks;

// Minimal java.util.concurrent.locks.LockSupport — the low-level block/wake primitive AQS builds
// on. `park()` blocks until `unpark(thread)` (or a permit left by an earlier unpark); the VM
// intercepts these in `invokestatic` (scheduler ops). The `blocker` overload is for diagnostics
// and is ignored. Permit semantics: an unpark before a park isn't lost.
public class LockSupport {
    private LockSupport() {
    }

    public static native void park();

    public static native void park(Object blocker);

    public static native void unpark(Thread thread);
}
