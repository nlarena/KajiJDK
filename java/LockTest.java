import java.util.concurrent.locks.ReentrantLock;

// H6: ReentrantLock as a mutex, taken **reentrantly** (locked twice, unlocked twice). Three workers
// each do 100 guarded non-atomic increments of a shared int → 300 iff the lock serialises them.
class LockWorker extends Thread {
    ReentrantLock lock;
    int[] shared;

    public void run() {
        for (int i = 0; i < 100; i++) {
            this.lock.lock();
            this.lock.lock(); // reentrant
            this.shared[0] = this.shared[0] + 1;
            this.lock.unlock();
            this.lock.unlock();
        }
    }
}

public class LockTest {
    static int run() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        int[] shared = new int[1];
        LockWorker a = new LockWorker();
        LockWorker b = new LockWorker();
        LockWorker c = new LockWorker();
        a.lock = lock; a.shared = shared;
        b.lock = lock; b.shared = shared;
        c.lock = lock; c.shared = shared;
        a.start(); b.start(); c.start();
        a.join(); b.join(); c.join();
        return shared[0]; // 300
    }
}
