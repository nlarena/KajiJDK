package java.util.concurrent;

// A one-shot gate: threads calling {@link #await} block until the count reaches zero,
// and once it does the latch stays open forever. The JDK expresses the count as an
// AbstractQueuedSynchronizer state word; KajiJDK keeps it as a field guarded by the
// intrinsic monitor of a private `sync` object, with `notifyAll` as the release.
//
// Style note (shared by every class in this package): methods are written **single-exit**
// — no `return` from inside a `synchronized` block, because the compiler emits no
// `monitorexit` on that path (finding #105). A `throw` inside the block is safe.
//
// The `throws InterruptedException` clauses the JDK declares are omitted throughout:
// KajiJDK has no thread interruption, the throws clause is not part of the descriptor
// (so the API gate is unaffected), and omitting it keeps callers free of dead catch
// blocks — see also finding #104.
public class CountDownLatch {

    private final Object sync = new Object();
    private int count;

    public CountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        this.count = count;
    }

    // Block until the count reaches zero. Returns at once if it already has.
    public void await() throws InterruptedException {
        synchronized (sync) {
            while (count > 0) {
                sync.wait();
            }
        }
    }

    // Block until the count reaches zero or the wait elapses; reports whether the count
    // reached zero. Best-effort: parks once for the whole timeout, then re-checks.
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        boolean opened;
        synchronized (sync) {
            if (count > 0) {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    sync.wait(ms);
                }
            }
            opened = count == 0;
        }
        return opened;
    }

    // Decrement the count, releasing every waiting thread when it reaches zero.
    public void countDown() {
        synchronized (sync) {
            if (count > 0) {
                count--;
                if (count == 0) {
                    sync.notifyAll();
                }
            }
        }
    }

    public long getCount() {
        long n;
        synchronized (sync) {
            // Explicit widening cast: an *implicit* int→long conversion is compiled
            // without the `i2l` the JVM needs (finding #103), so the caller would get an
            // int-tagged value and `lcmp` against it would trap.
            n = (long) count;
        }
        return n;
    }
}
