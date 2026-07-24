// H4 (JMM) verification: **publication via a volatile flag**, end-to-end through the interpreter.
//
// The publisher writes a non-volatile payload (int + long) and then a *volatile* `ready` flag; the
// reader spins on `ready` and, once it sees it set, reads the payload. In the relaxed memory model
// the flag's volatile write is a **Release** and the spinning read an **Acquire**, so everything
// the publisher wrote before setting the flag *happens-before* the reader's payload reads — it can
// never observe the stale defaults (0 / 0L). It also carries a *volatile long* (`vlong`), which
// rides a real `AtomicU64` (8-aligned, no tearing).
//
// The Rust harness runs it in green / os-gil / os and asserts all three agree (the oracle) and that
// os doesn't hang: the reader's spin must terminate, which needs the flag write to become visible
// (it does — a volatile Release publishes, and even a plain Relaxed store propagates).
class VShared {
    int data;            // non-volatile int  (Relaxed) — published through the flag
    long bigData;        // non-volatile long (Relaxed) — published through the flag
    volatile long vlong; // volatile long     (AtomicU64 Acquire/Release, no tearing)
    volatile int ready;  // volatile flag     (Acquire/Release) — the publication gate
}

class VPublisher extends Thread {
    VShared s; // set by the driver before start()

    public void run() {
        this.s.data = 42;      // Relaxed
        this.s.bigData = 1000; // Relaxed
        this.s.vlong = 777;    // Release (volatile long → AtomicU64)
        this.s.ready = 1;      // Release (volatile flag) — publishes everything above
    }
}

class VReader extends Thread {
    VShared s; // set by the driver before start()
    int observed;
    long observedBig;
    long observedV;

    public void run() {
        while (this.s.ready == 0) { // spin on the volatile flag (Acquire)
        }
        // The Acquire saw the Release, so these reads see the published values, never the defaults.
        this.observed = this.s.data;
        this.observedBig = this.s.bigData;
        this.observedV = this.s.vlong; // volatile long read (Acquire)
    }
}

public class VolatilePublish {
    static int run() {
        VShared shared = new VShared();
        VReader r = new VReader();
        VPublisher p = new VPublisher();
        r.s = shared;
        p.s = shared;
        r.start(); // reader spins first — exercises the Acquire spin path
        p.start();
        try {
            p.join();
            r.join();
        } catch (InterruptedException e) {
        }
        // 42 + 1000 + 777 = 1819 — deterministic once publication is honoured.
        return r.observed + (int) r.observedBig + (int) r.observedV;
    }
}
