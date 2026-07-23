// A worker waits on a lock; main interrupts it. The worker must catch InterruptedException
// out of wait() — and per JLS it re-acquires the monitor before the exception is seen, so
// the catch runs while holding the lock.
public class InterruptWait {
    static final Object lock = new Object();
    static volatile int stage;

    static int run() throws Exception {
        Waiter w = new Waiter();
        w.start();
        while (stage < 1) { Thread.yield(); }   // wait until w is inside wait()
        w.interrupt();
        w.join();
        return stage == 2 ? 42 : -stage;
    }
}

class Waiter extends Thread {
    public void run() {
        synchronized (InterruptWait.lock) {
            try {
                InterruptWait.stage = 1;
                InterruptWait.lock.wait();     // releases the lock; interrupt wakes it
                InterruptWait.stage = -99;
            } catch (InterruptedException e) {
                // We hold the lock again here (re-acquired before the throw).
                InterruptWait.stage = Thread.holdsLock(InterruptWait.lock) ? 2 : -50;
            }
        }
    }
}
