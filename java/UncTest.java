// A7 #2 (JVMS §2.10): an uncaught exception terminates the THREAD, not the VM. The worker throws
// a RuntimeException nobody catches; the VM must print "Exception in thread ..." + the stack trace
// to the console and mark the worker TERMINATED (waking joiners) — before the fix the whole VM
// panicked. Main joins, checks the worker died and that main itself keeps running normally.
// Deterministic → green ≡ os-gil ≡ os = 42.
class UncBomb implements Runnable {
    public void run() {
        throw new RuntimeException("uncaught in worker"); // never caught
    }
}

public class UncTest {
    static int run() {
        Thread t = new Thread(new UncBomb());
        t.start();
        try {
            t.join(); // must return (worker terminated), not hang or crash the VM
        } catch (InterruptedException e) {
        }
        int score = 0;
        if (t.getState() == Thread.State.TERMINATED) {
            score += 40; // the worker died cleanly
        }
        score += 2; // main is still alive and computing
        return score; // 42
    }
}
