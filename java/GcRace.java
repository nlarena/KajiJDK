// Stress harness aimed at the rare os-parallel stale-reference bug. Maximises the exact shape that
// crashed: many threads (over-subscribing cores → heavy preemption), a relentless allocation storm
// (frequent minor GCs), and `main` holding references to all the workers *across* those GCs and
// then dispatching virtual calls on them (`join`, then a field read) — the point where a
// not-remapped reference surfaces as "could not resolve the receiver". Result is deterministic
// (== number of workers) so any miscount or crash is a real fault, catchable by JVM_GC_VERIFY.
public class GcRace {
    static int run() throws InterruptedException {
        GcWorker[] ws = new GcWorker[12];
        for (int i = 0; i < ws.length; i++) {
            ws[i] = new GcWorker();
            ws[i].start();
        }
        // Allocate garbage while the worker references are live in this frame + in `ws`: GCs fire
        // with those references in play, so a botched remap leaves a dangling worker pointer.
        int churn = 0;
        for (int i = 0; i < 3000; i++) {
            int[] junk = new int[4];
            churn += junk.length;
        }
        for (int i = 0; i < ws.length; i++) {
            ws[i].join(); // invokevirtual on each worker reference (the original crash site)
        }
        int total = 0;
        for (int i = 0; i < ws.length; i++) {
            total += ws[i].done; // dereference again after the GC storm
        }
        return total + (churn - churn); // churn cancels; keeps the loop from being optimised away
    }
}

class GcWorker extends Thread {
    int done;

    public void run() {
        int acc = 0;
        for (int i = 0; i < 800; i++) {
            int[] junk = new int[8];
            acc += junk.length;
        }
        this.done = 1 + (acc - acc);
    }
}
