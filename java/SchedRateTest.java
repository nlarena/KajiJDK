import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

// H6 volume: ScheduledThreadPoolExecutor.scheduleAtFixedRate(). One periodic task fires repeatedly;
// it increments a counter but CAPS it at `max` (so the exact count is deterministic regardless of
// how many extra times it fires) and counts down a latch when it reaches max. main awaits the latch
// (proving the task ran ≥ max times → periodic scheduling works), then shuts down → counter == max.
class RateTask implements Runnable {
    int[] counter;
    int max;
    ReentrantLock lock;
    CountDownLatch latch;

    public void run() {
        lock.lock();
        try {
            if (counter[0] < max) {
                counter[0] = counter[0] + 1;
                if (counter[0] == max) {
                    latch.countDown();
                }
            }
        } finally {
            lock.unlock();
        }
    }
}

public class SchedRateTest {
    static int run() {
        ScheduledThreadPoolExecutor sched = new ScheduledThreadPoolExecutor(1);
        int[] counter = new int[1];
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch latch = new CountDownLatch(1);
        RateTask t = new RateTask();
        t.counter = counter;
        t.max = 10;
        t.lock = lock;
        t.latch = latch;
        sched.scheduleAtFixedRate(t, 0, 2); // initial 0, period 2 ms
        try {
            latch.await(); // returns once the task has fired 10 times
        } catch (InterruptedException e) {
        }
        sched.shutdown();
        try {
            sched.awaitTermination();
        } catch (InterruptedException e) {
        }
        return counter[0]; // 10 (capped)
    }
}
