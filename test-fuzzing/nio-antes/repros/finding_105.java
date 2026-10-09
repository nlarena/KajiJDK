// Finding #105 — `monitorexit` not emitted on an early `return` inside a `synchronized` block.
// The reentrant fast path returns from inside the block; the compiler emits monitorenter but
// no matching monitorexit before that return, leaking the monitor (fall-through and exception
// exits ARE handled correctly). Synchronized *methods* are unaffected (VM releases on frame exit).
public class finding_105 {
    private final Object sync = new Object();
    private int count;
    void enter() {
        synchronized (sync) {
            if (count > 0) {
                count++;
                return;          // <-- no monitorexit emitted here → monitor leaks
            }
            count = 1;
        }                        // fall-through exit: monitorexit IS emitted
    }
}
