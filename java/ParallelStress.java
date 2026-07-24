// H3 1d stress / widening test: three worker threads each run a long *frame-local* compute
// loop — now exercising the **widened** fast-path set (int + long arithmetic, shifts,
// conversions, reference loads/stores and identity branches) — interleaved with allocation
// that pressures the heap, forcing the GC stop-the-world handshake to fire while siblings are
// mid lock-free run. The Rust harness runs it in green, os-gil and os and asserts all three
// agree (the oracle for the fast-path arms) and that os never hangs (the signal for races).
class PStressWorker extends Thread {
    int result;
    int base = 7;
    int[] data = { 10, 20, 30, 40, 50 };

    public void run() {
        int x = 0;
        long acc = 0;
        Object prev = null;
        for (int j = 0; j < 40; j++) {
            // Fast-path compute + read-path shared reads every iteration — all under the **read**
            // lock (W3), so the three workers do them concurrently: `this.base` (getfield),
            // `data.length` (arraylength) and `data[..]` (iaload). Hammers the read path, its
            // escalation, and the read/write/safepoint edges.
            for (int i = 0; i < 1000; i++) {
                x = x + i + this.base + this.data[i % this.data.length]; // getfield + arraylength + iaload
                acc = acc + (long) i;   // i2l, ladd
            }
            acc = (acc >> 1) << 1;      // lshr, lshl (clears the low bit; sums are even → no-op)
            Object o = new Object();    // new — shared opcode; pressures Eden → GC handshake
            if (o != prev) {            // if_acmpne — reference identity, frame-local
                x = x + 1;
            }
            prev = o;
            int[] junk = new int[500];  // shared alloc
            junk[0] = x;
        }
        this.result = x + (int) (acc >>> 4); // lushr, l2i
    }
}

public class ParallelStress {
    static int run() {
        PStressWorker a = new PStressWorker();
        PStressWorker b = new PStressWorker();
        PStressWorker c = new PStressWorker();
        a.start();
        b.start();
        c.start();
        try {
            a.join();
            b.join();
            c.join();
        } catch (InterruptedException e) {
        }
        return a.result + b.result + c.result;
    }
}
