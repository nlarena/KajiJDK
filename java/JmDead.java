// F3 step 9 — **the top of the type lattice**, end to end through the whole VM.
//
// Until this step the JIT's type map merged by equality, so a local slot that arrived at a pc as
// an `int` down one edge and as a reference down another refused the entire method. That is not an
// exotic shape: it is what `javac` emits for **every object allocated inside a loop**, because a
// fresh frame fills every non-argument slot with `Value::Int(0)` and the back-edge brings the
// reference the last iteration stored. `BmField` is the workload it kept out for four steps.
//
// The three methods below are the three halves of the new rule that can be written in Java (the
// fourth — *reading* a conflicted slot — `javac` cannot emit at all, since definite assignment
// forbids it, so it is pinned at the compiler level in `compile_tests` instead):
//
//   `dead`    the merge itself, with the slot dead where the two edges meet;
//   `retyped` one slot doing duty as a reference in one scope and an `int` in the next, with the
//             second scope actually **read** — which is only possible because a `store` brings a
//             conflicted slot back down the lattice;
//   `guarded` a **deopt at a pc where a slot is conflicted**, which is the case the write-back has
//             to answer for: the interpreter is handed back a frame in which that slot was
//             deliberately left holding a stale value from the previous iteration.
//
// All three allocate hard enough to collect, which is the point: a rebuilt frame is a GC root the
// instant it is interpreted again, and a conflicted slot is one this design writes **nothing** to.
class JmCell {
    int a;

    JmCell(int a) {
        this.a = a;
    }
}

public class JmDead {
    // (1) The dead-slot merge. At the loop header `c`'s slot holds an `int` on the way in and a
    // reference across the back-edge; nothing reads it there, because the body's first act is to
    // store a new object into it.
    static int dead() {
        int acc = 0;
        for (int i = 0; i < 900; i++) {
            JmCell c = new JmCell(i);
            acc = (acc + c.a) & 0xFFFFF;
        }
        return acc;
    }

    // (2) Two disjoint scopes over one slot, and the second one is read. `javac` gives `c` and `t`
    // the same local index, so the slot is a reference in one arm and an `int` in the other, and
    // `Conflict` where the arms meet. Each arm's own `astore`/`istore` is what re-types it.
    static int retyped(int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            if ((i & 3) == 0) {
                JmCell c = new JmCell(i);
                acc = (acc + c.a) & 0xFFFFF;
            } else {
                int t = i * 3;
                acc = (acc + t) & 0xFFFFF;
            }
        }
        return acc;
    }

    // (3) The deopt. The bounds check on `xs[i]` is emitted **before** the object is stored into
    // `c`, so the resume site at that pc describes a frame in which `c` is exactly a slot the
    // write-back must say nothing about. Walking off the end of `xs` fails the check, native code
    // gives up, and the interpreter re-runs the `iaload` and throws — with `c` still holding the
    // previous iteration's object, which nobody reads and which the collector can see.
    static int guarded(int[] xs, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            JmCell c = new JmCell(xs[i]);
            acc = (acc + c.a) & 0xFFFFF;
        }
        return acc;
    }

    static int trapped() {
        int[] xs = new int[64];
        for (int i = 0; i < 64; i++) {
            xs[i] = i * i;
        }
        try {
            // 400 iterations over 64 elements: the first 64 run, and the 65th deopts and throws.
            return guarded(xs, 400);
        } catch (ArrayIndexOutOfBoundsException e) {
            return 7;
        }
    }

    public static int run() {
        int acc = dead();
        acc = (acc + retyped(600)) & 0xFFFFF;
        acc = (acc + trapped()) & 0xFFFFF;
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
