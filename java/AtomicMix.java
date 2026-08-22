import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

// H5 — AtomicLong (64-bit CAS) + AtomicReference (identity CAS + write barrier).
public class AtomicMix {
    static int run() {
        AtomicLong l = new AtomicLong(100L);
        if (!l.compareAndSet(100L, 200L)) return -1;
        if (l.incrementAndGet() != 201L) return -2;

        AtomicReference<String> r = new AtomicReference<String>("a");
        String a = r.get();
        if (!r.compareAndSet(a, "b")) return -3;      // identity match → succeeds
        if (r.compareAndSet("x", "c")) return -4;     // "x" != stored "b" by identity → fails
        if (!r.get().equals("b")) return -5;

        return (int) l.get() + r.get().length(); // 201 + 1 = 202
    }
}
