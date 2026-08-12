import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

// H6 volume: PriorityBlockingQueue. Three producers concurrently put 300 items with DISTINCT
// priorities (interleaved 0..299) into the min-heap; after they join, main takes all 300 — they
// must come out in strictly ascending priority order (the heap orders regardless of insertion order
// or which thread inserted). Returns 300 iff every take was larger than the last. green ≡ os-gil ≡ os.
class PqItem {
    int p;

    PqItem(int p) {
        this.p = p;
    }
}

class PqCmp implements Comparator<PqItem> {
    public int compare(PqItem a, PqItem b) {
        return a.p - b.p;
    }
}

class PqProducer extends Thread {
    PriorityBlockingQueue<PqItem> q;
    int id;

    public void run() {
        for (int j = 0; j < 100; j++) {
            q.put(new PqItem(id + j * 3)); // id=0→0,3,..297 · id=1→1,4,..298 · id=2→2,5,..299
        }
    }
}

public class PqTest {
    static int run() {
        PriorityBlockingQueue<PqItem> q = new PriorityBlockingQueue<PqItem>(16, new PqCmp());
        PqProducer a = new PqProducer();
        PqProducer b = new PqProducer();
        PqProducer c = new PqProducer();
        a.q = q; a.id = 0;
        b.q = q; b.id = 1;
        c.q = q; c.id = 2;
        a.start();
        b.start();
        c.start();
        try {
            a.join();
            b.join();
            c.join();
        } catch (InterruptedException e) {
        }
        int prev = -1;
        int count = 0;
        for (int k = 0; k < 300; k++) {
            try {
                PqItem x = q.take();
                if (x.p <= prev) {
                    return -1; // out of order
                }
                prev = x.p;
                count = count + 1;
            } catch (InterruptedException e) {
            }
        }
        return count; // 300
    }
}
