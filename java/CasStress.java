import java.util.concurrent.atomic.AtomicInteger;

// H5 — CAS under contention: three threads each incrementAndGet a SHARED AtomicInteger 1000 times.
// The retry loop over the native compareAndSet must lose no updates → 3000. In os mode the CAS is
// serialized on the write lock (an invokevirtual escalates), so it's correct; green/os-gil agree.
class CasWorker extends Thread {
    AtomicInteger counter; // set by the driver before start()

    public void run() {
        for (int i = 0; i < 1000; i++) {
            this.counter.incrementAndGet();
        }
    }
}

public class CasStress {
    static int run() {
        AtomicInteger counter = new AtomicInteger(0);
        CasWorker a = new CasWorker();
        CasWorker b = new CasWorker();
        CasWorker c = new CasWorker();
        a.counter = counter;
        b.counter = counter;
        c.counter = counter;
        a.start();
        b.start();
        c.start();
        try {
            a.join();
            b.join();
            c.join();
        } catch (InterruptedException e) {
        }
        return counter.get(); // 3000
    }
}
