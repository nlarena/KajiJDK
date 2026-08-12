import java.util.concurrent.locks.ReentrantReadWriteLock;

// H6 volume: ReentrantReadWriteLock. Three WRITERS each do 100 non-atomic increments of a shared
// int under the **write** lock — the count is exact (300) only if the write lock truly serialises
// them (no lost update). Two READERS spin reading the same int under the **read** lock, so the read
// path runs concurrently and its mutual exclusion against writers is exercised (a reader never
// observes a half-written value, and never deadlocks a writer). Deterministic 300 → green ≡ os-gil ≡ os.
class RwWriter extends Thread {
    ReentrantReadWriteLock rw;
    int[] shared;

    public void run() {
        for (int i = 0; i < 100; i++) {
            rw.writeLock().lock();
            try {
                shared[0] = shared[0] + 1; // non-atomic; the write lock serialises it
            } finally {
                rw.writeLock().unlock();
            }
        }
    }
}

class RwReader extends Thread {
    ReentrantReadWriteLock rw;
    int[] shared;
    int seen; // accumulates reads so the loop isn't optimised away; not part of the result

    public void run() {
        for (int i = 0; i < 100; i++) {
            rw.readLock().lock();
            try {
                seen = seen + shared[0];
            } finally {
                rw.readLock().unlock();
            }
        }
    }
}

public class RwLockTest {
    static int run() {
        ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
        int[] shared = new int[1];
        RwWriter w1 = new RwWriter();
        RwWriter w2 = new RwWriter();
        RwWriter w3 = new RwWriter();
        RwReader r1 = new RwReader();
        RwReader r2 = new RwReader();
        w1.rw = rw; w1.shared = shared;
        w2.rw = rw; w2.shared = shared;
        w3.rw = rw; w3.shared = shared;
        r1.rw = rw; r1.shared = shared;
        r2.rw = rw; r2.shared = shared;
        w1.start();
        w2.start();
        w3.start();
        r1.start();
        r2.start();
        try {
            w1.join();
            w2.join();
            w3.join();
            r1.join();
            r2.join();
        } catch (InterruptedException e) {
        }
        return shared[0]; // 300 — readers don't change it
    }
}
