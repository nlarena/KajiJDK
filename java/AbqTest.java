import java.util.concurrent.ArrayBlockingQueue;

// H6 volume: bounded producer/consumer over an ArrayBlockingQueue(capacity=4). Three producers each
// `put` 100 tokens (plain Objects — no autoboxing, which java.lang.Integer here doesn't provide);
// `main` `take`s all 300 and counts them. The small capacity forces real blocking on BOTH sides —
// `put` waits while full, `take` waits while empty — exercising the two Conditions (notFull/notEmpty)
// and the ReentrantLock under real parallelism. Deterministic count = 300, so green ≡ os-gil ≡ os
// must agree (a lost/duplicated token or a missed wake would miscount).
class AbqProducer extends Thread {
    ArrayBlockingQueue<Object> q;

    public void run() {
        for (int i = 0; i < 100; i++) {
            try {
                this.q.put(this); // a non-null token; the value doesn't matter, the count does
            } catch (InterruptedException e) {
            }
        }
    }
}

public class AbqTest {
    static int run() {
        ArrayBlockingQueue<Object> q = new ArrayBlockingQueue<>(4);
        AbqProducer a = new AbqProducer();
        AbqProducer b = new AbqProducer();
        AbqProducer c = new AbqProducer();
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
