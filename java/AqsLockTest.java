import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// H6: a non-reentrant lock built on AbstractQueuedSynchronizer (the AQS javadoc's own example),
// compiled against the real AQS but run on ours. Three workers do 100 guarded non-atomic
// increments each → 300 iff acquire/release truly serialise (state CAS + park/unpark queue).
class TestMutex extends AbstractQueuedSynchronizer {
    protected boolean tryAcquire(int arg) {
        return compareAndSetState(0, 1);
    }

    protected boolean tryRelease(int arg) {
        setState(0);
        return true;
    }

    void lock() {
        acquire(1);
    }

    void unlock() {
        release(1);
    }
}

class AqsWorker extends Thread {
    TestMutex lock;
    int[] shared;

    public void run() {
        for (int i = 0; i < 100; i++) {
            this.lock.lock();
            this.shared[0] = this.shared[0] + 1;
            this.lock.unlock();
        }
    }
}

public class AqsLockTest {
    static int run() throws InterruptedException {
        TestMutex lock = new TestMutex();
        int[] shared = new int[1];
        AqsWorker a = new AqsWorker();
        AqsWorker b = new AqsWorker();
        AqsWorker c = new AqsWorker();
        a.lock = lock; a.shared = shared;
        b.lock = lock; b.shared = shared;
        c.lock = lock; c.shared = shared;
        a.start(); b.start(); c.start();
        a.join(); b.join(); c.join();
        return shared[0]; // 300
    }
}
