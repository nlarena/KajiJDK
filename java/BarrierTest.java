import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

// H6: CyclicBarrier(3). Each worker increments `before`, waits at the barrier, then checks that
// `before` == 3 — which holds only if the barrier truly held everyone until all three arrived.
// Each success bumps `after`; the barrier works iff after == 3.
class BarrierWorker extends Thread {
    CyclicBarrier barrier;
    AtomicInteger before;
    AtomicInteger after;

    public void run() {
        this.before.incrementAndGet();
        try {
            this.barrier.await();
        } catch (Exception e) {
        }
        if (this.before.get() == 3) {
            this.after.incrementAndGet();
        }
    }
}

public class BarrierTest {
    static int run() throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(3);
        AtomicInteger before = new AtomicInteger(0);
        AtomicInteger after = new AtomicInteger(0);
        BarrierWorker a = new BarrierWorker();
        BarrierWorker b = new BarrierWorker();
        BarrierWorker c = new BarrierWorker();
        a.barrier = barrier; a.before = before; a.after = after;
        b.barrier = barrier; b.before = before; b.after = after;
        c.barrier = barrier; c.before = before; c.after = after;
        a.start(); b.start(); c.start();
        a.join(); b.join(); c.join();
        return after.get(); // 3
    }
}
