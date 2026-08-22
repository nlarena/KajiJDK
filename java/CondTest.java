import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

// H6: Condition on a ReentrantLock. Three workers lock, then `await()` (releasing the lock and
// blocking) until `ready` is set. Main sets ready and `signalAll()`s under the lock; each worker
// re-acquires the lock, sees ready, leaves, and increments → 3. (The `while(!ready)` guard makes it
// robust to a worker not yet waiting when the signal fires — it just sees ready and skips await.)
class CondWorker extends Thread {
    ReentrantLock lock;
    Condition cond;
    boolean[] ready;
    AtomicInteger counter;

    public void run() {
        this.lock.lock();
        try {
            while (!this.ready[0]) {
                this.cond.await();
            }
        } catch (InterruptedException e) {
        } finally {
            this.lock.unlock();
        }
        this.counter.incrementAndGet();
    }
}

public class CondTest {
    static int run() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Condition cond = lock.newCondition();
        boolean[] ready = new boolean[1];
        AtomicInteger counter = new AtomicInteger(0);
        CondWorker a = new CondWorker();
        CondWorker b = new CondWorker();
        CondWorker c = new CondWorker();
        a.lock = lock; a.cond = cond; a.ready = ready; a.counter = counter;
        b.lock = lock; b.cond = cond; b.ready = ready; b.counter = counter;
        c.lock = lock; c.cond = cond; c.ready = ready; c.counter = counter;
        a.start(); b.start(); c.start();
        lock.lock();
        try {
            ready[0] = true;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
        a.join(); b.join(); c.join();
        return counter.get(); // 3
    }
}
