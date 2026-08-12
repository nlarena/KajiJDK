import java.util.concurrent.Semaphore;

// H6: a binary Semaphore(1) used as a mutex. Three workers each do 100 NON-atomic increments of a
// shared int, guarded by the permit — only one holds it at a time, so no update is lost → 300.
class SemWorker extends Thread {
    Semaphore sem;
    int[] shared; // shared[0] is the counter, protected by the semaphore

    public void run() {
        for (int i = 0; i < 100; i++) {
            try {
                this.sem.acquire();
            } catch (InterruptedException e) {
            }
            this.shared[0] = this.shared[0] + 1; // non-atomic; the permit serializes it
            this.sem.release();
        }
    }
}

public class SemTest {
    static int run() {
        Semaphore sem = new Semaphore(1);
        int[] shared = new int[1];
        SemWorker a = new SemWorker();
        SemWorker b = new SemWorker();
        SemWorker c = new SemWorker();
        a.sem = sem; a.shared = shared;
        b.sem = sem; b.shared = shared;
        c.sem = sem; c.shared = shared;
        a.start();
        b.start();
        c.start();
        try {
            a.join();
            b.join();
            c.join();
        } catch (InterruptedException e) {
        }
        return shared[0]; // 300
    }
}
