import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicInteger;

// H6/AQS foundation: LockSupport.park/unpark. Three workers park-loop until a shared flag is set;
// main sets it and unparks each. The loop + permit make it correct under any ordering → 3.
class ParkWorker extends Thread {
    AtomicInteger flag;
    AtomicInteger counter;

    public void run() {
        while (this.flag.get() == 0) {
            LockSupport.park();
        }
        this.counter.incrementAndGet();
    }
}

public class ParkTest {
    static int run() throws InterruptedException {
        AtomicInteger flag = new AtomicInteger(0);
        AtomicInteger counter = new AtomicInteger(0);
        ParkWorker a = new ParkWorker();
        ParkWorker b = new ParkWorker();
        ParkWorker c = new ParkWorker();
        a.flag = flag; a.counter = counter;
        b.flag = flag; b.counter = counter;
        c.flag = flag; c.counter = counter;
        a.start(); b.start(); c.start();
        flag.set(1);
        LockSupport.unpark(a);
        LockSupport.unpark(b);
        LockSupport.unpark(c);
        a.join(); b.join(); c.join();
        return counter.get(); // 3
    }
}
