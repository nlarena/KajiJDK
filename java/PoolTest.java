import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

// H6 volume: a fixed ThreadPoolExecutor(4) runs 300 tasks that each do one guarded increment of a
// shared int. The pool must run *every* task exactly once on its 4 worker threads (no task lost or
// double-run) and the ReentrantLock serialises the increments → exactly 300. green ≡ os-gil ≡ os.
class IncTask implements Runnable {
    int[] counter;
    ReentrantLock lock;

    public void run() {
        lock.lock();
        try {
            counter[0] = counter[0] + 1;
        } finally {
            lock.unlock();
        }
    }
}

public class PoolTest {
    static int run() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        int[] counter = new int[1];
        ReentrantLock lock = new ReentrantLock();
        for (int i = 0; i < 300; i++) {
            IncTask t = new IncTask();
            t.counter = counter;
            t.lock = lock;
            pool.execute(t);
        }
        pool.shutdown();
        try {
            pool.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        return counter[0]; // 300
    }
}
