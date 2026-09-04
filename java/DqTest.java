import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

// H6 volume: DelayQueue. Ten items get ABSOLUTE expirations `base + (id+1)*gap` from a single base
// time — so the expiration order is *exactly* by id regardless of any jitter between the puts (the
// earlier per-construction-nanoTime version was flaky: a GC pause between puts could reorder the
// tight 3 ms deltas). Put in REVERSE order; take() releases them earliest-expiration first (a timed
// wait until each is due) → ascending id (0..9) → 10. green ≡ os-gil ≡ os.
class DqItem implements Delayed {
    long expire; // absolute nanoTime deadline
    int id;

    DqItem(int id, long expire) {
        this.id = id;
        this.expire = expire;
    }

    // Lleva la unidad porque `Delayed.getDelay` la lleva: el que pregunta elige en que escala quiere
    // la respuesta. El plazo se mide en nanosegundos, asi que se convierte desde ahi.
    public long getDelay(TimeUnit unit) {
        return unit.convert(expire - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public int compareTo(Delayed o) {
        DqItem d = (DqItem) o;
        if (expire < d.expire) {
            return -1;
        }
        if (expire > d.expire) {
            return 1;
        }
        return 0;
    }
}

public class DqTest {
    static int run() {
        DelayQueue<DqItem> q = new DelayQueue<DqItem>();
        long base = System.nanoTime();
        long gap = 3 * 1000000L; // 3 ms in nanos
        for (int i = 9; i >= 0; i--) {
            q.put(new DqItem(i, base + (i + 1) * gap)); // expire strictly ordered by id, jitter-proof
        }
        int prev = -1;
        int count = 0;
        for (int k = 0; k < 10; k++) {
            try {
                DqItem x = q.take(); // blocks until each expires → id 0,1,...,9
                if (x.id <= prev) {
                    return -1;
                }
                prev = x.id;
                count = count + 1;
            } catch (InterruptedException e) {
            }
        }
        return count; // 10
    }
}
