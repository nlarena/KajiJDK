import java.util.concurrent.atomic.AtomicInteger;

// H5 — CAS: single-threaded exercise of AtomicInteger built on the native compareAndSet.
public class Cas {
    static int run() {
        AtomicInteger a = new AtomicInteger(10);
        if (!a.compareAndSet(10, 20)) return -1;   // succeeds
        if (a.compareAndSet(99, 0)) return -2;      // fails (value is 20, not 99)
        if (a.get() != 20) return -3;
        if (a.incrementAndGet() != 21) return -4;
        if (a.getAndAdd(4) != 21) return -5;
        if (a.get() != 25) return -6;
        return a.addAndGet(5); // 30
    }
}
