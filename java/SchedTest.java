import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

// H6 volume: ScheduledThreadPoolExecutor one-shot schedule(). Five tasks are scheduled with a small
// delay; each does one guarded increment and counts down a latch. main awaits the latch (so all
// five have provably run — deterministic, not timing-dependent), then shuts down → counter == 5.
class SchedTask implements Runnable {
    int[] counter;
    ReentrantLock lock;
    CountDownLatch latch;

    public void run() {
        lock.lock();
        try {
            counter[0] = counter[0] + 1;
        } finally {
            lock.unlock();
        }
        latch.countDown();
    }
}

public class SchedTest {
    static int run() {
        ScheduledThreadPoolExecutor sched = new ScheduledThreadPoolExecutor(1);
        int[] counter = new int[1];
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            SchedTask t = new SchedTask();
            t.counter = counter;
            t.lock = lock;
            t.latch = latch;
            sched.schedule(t, 5, TimeUnit.MILLISECONDS); // 5 ms delay → exercises the delayed heap
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
        }
        sched.shutdown();
        try {
            sched.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        return counter[0]; // 5
    }
}
