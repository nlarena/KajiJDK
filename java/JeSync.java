// F3 group 5 — **`monitorenter` as a permanent deopt**, and the shape that pays for it.
//
// A monitor acquire is a scheduler operation and will never be an instruction at this tier. The
// question is only what to do with a method that contains one, and the answer is *not* to refuse it:
// a deopt names an instruction that has not run, so everything **before** the lock still runs
// natively. `loopThenSync` is that shape, on purpose and at both ends:
//
//  - its loop is ordinary compilable arithmetic over an `int[]`, and it runs in native code;
//  - the code inside the `synchronized` block is deliberately **outside** the subset (an
//    `invokevirtual`), which is the whole point — the scan stops at the `monitorenter`, so what
//    comes after does not have to be compilable at all. Before group 5 this method was refused at
//    its `monitorenter` and its loop was interpreted.
//
// `syncSimple` is the same shape with a body that *is* in the subset, so the only reason it stops
// is the monitor itself; it also has an observable, non-idempotent `putstatic` inside the lock, so
// a `monitorenter` that quietly fell through would apply it in native code as well as in the
// interpreted resume.
//
// `syncMethod` is the other exclusion, and it is a different one: `ACC_SYNCHRONIZED` is a flag on
// the method, not an opcode in its body. Nothing here can turn it into a deopt — there is no
// bytecode for the interpreter to re-execute — so it is excluded structurally, at the interpreter's
// dispatch point, and it never reaches the JIT at all.
public class JeSync {
    static int total;
    static final String NAME = "jesync";
    static final Object LOCK = new Object();

    // Loop natively, then lock. The scan stops at the `monitorenter`; `NAME.length()` is an
    // `invokevirtual` and is never looked at.
    static int loopThenSync(int[] xs, int n, Object lock) {
        int s = 0;
        for (int i = 0; i < n; i++) {
            s = (s + xs[i] * 3) & 0xFFFF;
        }
        synchronized (lock) {
            total = (total + s + NAME.length()) & 0xFFFF;
        }
        return s;
    }

    // The same, with a lock body that is entirely inside the subset and observably non-idempotent.
    static int syncSimple(int[] xs, int n, Object lock) {
        int s = 0;
        for (int i = 0; i < n; i++) {
            s = (s + xs[i]) & 0xFFFF;
        }
        synchronized (lock) {
            total = (total + 1) & 0xFFFF;
        }
        return (s + total) & 0xFFFF;
    }

    // Excluded by the **flag**, not by an opcode: its body is pure subset arithmetic.
    static synchronized int syncMethod(int a, int b) {
        return (a * 31 + b) & 0xFFFF;
    }

    public static int run() {
        int[] xs = new int[64];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = i * 5 + 1;
        }
        int acc = 0;
        for (int i = 0; i < 3000; i++) {
            acc = (acc + loopThenSync(xs, 64, LOCK)) & 0xFFFFF;
            acc = (acc + syncSimple(xs, 64, LOCK)) & 0xFFFFF;
            acc = (acc + syncMethod(i, acc)) & 0xFFFFF;
        }
        return (acc + total) & 0xFFFFF;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
