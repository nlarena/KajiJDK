import java.util.concurrent.CountDownLatch;

// A6 loose ends: Thread peripherals. Four OS threads each store their own value into the SAME
// ThreadLocal (id*7), spin to interleave, then read it back — each must still see its own value
// with zero cross-contamination (proves per-thread isolation). Sum of 0+7+14+21 = 42. Also
// exercises priority/daemon on a fresh thread (getter/setter + range validation + the
// can't-set-daemon-after-start rule is implied by using an unstarted thread). Any API misbehavior
// sabotages the result to -1. Deterministic → green ≡ os-gil ≡ os = 42.
public class TlTest {
    static final ThreadLocal<TlBox> LOCAL = new ThreadLocal<TlBox>();

    static final class Worker implements Runnable {
        final int id;
        int result;
        final CountDownLatch done;

        Worker(int id, CountDownLatch done) {
            this.id = id;
            this.done = done;
        }

        public void run() {
            LOCAL.set(new TlBox(id * 7));
            for (int i = 0; i < 1000; i++) {
                Thread.yield(); // interleave with the other threads before reading back
            }
            result = LOCAL.get().v; // must be id*7 despite the others setting theirs
            done.countDown();
        }
    }

    static int run() {
        int n = 4;
        CountDownLatch done = new CountDownLatch(n);
        Worker[] ws = new Worker[n];
        for (int i = 0; i < n; i++) {
            ws[i] = new Worker(i, done);
            new Thread(ws[i]).start();
        }
        try {
            done.await();
        } catch (InterruptedException e) {
        }
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += ws[i].result; // 0 + 7 + 14 + 21 = 42
        }

        // Exercise priority + daemon on a fresh (unstarted) thread.
        Thread probe = new Thread();
        probe.setPriority(Thread.MIN_PRIORITY);
        probe.setDaemon(true);
        if (probe.getPriority() != Thread.MIN_PRIORITY || !probe.isDaemon()) {
            sum = -1; // getters/setters misbehaved
        }
        boolean threw = false;
        try {
            probe.setPriority(99); // out of [1,10] → IllegalArgumentException
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        if (!threw) {
            sum = -1; // range validation missing
        }
        return sum; // 42
    }
}

class TlBox {
    int v;

    TlBox(int v) {
        this.v = v;
    }
}
