import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

// H6: CountDownLatch — three workers block on await() until main counts the latch to zero, then
// each increments a shared AtomicInteger. After join the counter is 3 (all were released).
class LatchWorker extends Thread {
    CountDownLatch latch;
    AtomicInteger counter;

    public void run() {
        try {
            this.latch.await();
        } catch (InterruptedException e) {
        }
        this.counter.incrementAndGet();
    }
}

public class CdlTest {
    static int run() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);
        LatchWorker a = new LatchWorker();
        LatchWorker b = new LatchWorker();
        LatchWorker c = new LatchWorker();
        a.latch = latch; a.counter = counter;
        b.latch = latch; b.counter = counter;
        c.latch = latch; c.counter = counter;
        a.start();
        b.start();
        c.start();
        latch.countDown(); // releases all three
        a.join();
        b.join();
        c.join();
        return counter.get(); // 3
    }
}
