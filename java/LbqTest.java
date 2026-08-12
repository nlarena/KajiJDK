import java.util.concurrent.LinkedBlockingQueue;

// H6 volume: LinkedBlockingQueue (two-lock design). Three producers each `put` 100 tokens into an
// unbounded queue while `main` `take`s all 300 and counts them — producers (tail/putLock) and the
// consumer (head/takeLock) run in parallel without contending. A lost/duplicated node or a missed
// notEmpty wake would miscount. Deterministic 300 → green ≡ os-gil ≡ os.
class LbqProducer extends Thread {
    LinkedBlockingQueue<Object> q;

    public void run() {
        for (int i = 0; i < 100; i++) {
            try {
                this.q.put(this);
            } catch (InterruptedException e) {
            }
        }
    }
}

public class LbqTest {
    static int run() {
        LinkedBlockingQueue<Object> q = new LinkedBlockingQueue<Object>();
        LbqProducer a = new LbqProducer();
        LbqProducer b = new LbqProducer();
        LbqProducer c = new LbqProducer();
        a.q = q;
        b.q = q;
        c.q = q;
        a.start();
        b.start();
        c.start();
        int total = 0;
        for (int k = 0; k < 300; k++) {
            try {
                Object x = q.take();
                if (x != null) {
                    total = total + 1;
                }
            } catch (InterruptedException e) {
            }
        }
        try {
            a.join();
            b.join();
            c.join();
        } catch (InterruptedException e) {
        }
        return total; // 300
    }
}
