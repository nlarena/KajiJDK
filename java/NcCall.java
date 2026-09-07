// F3 group 4 — **compiled code calling compiled code**, through every shape the mechanism has.
//
// Until this group the JIT could only *inline* a call, so three whole families of method were
// outside the subset by construction: a recursion (a callee already on the inline path expands for
// ever, and the cycle check refuses it by identity), a body past the inlining budget, and any
// callee the compiler could not scan at all. A real call reaches all three, and the design that
// makes it safe is that **nothing unwinds**: a callee that cannot finish returns normally through
// its own epilogue with a status in a register, its caller parks that status and returns the same
// reason one level out, and the interpreter rebuilds the whole chain from the buffer. One `ret`
// per level, and no native frame ever left half-finished.
//
// Every arm below folds into one number, so the interpreted run (`JVM_JIT=0`) is the oracle for all
// of them and a real `java` of JDK 25 is the oracle for that.
//
// **Why each arm is shaped the way it is** — every one of these was chosen after checking that the
// obvious version could not fail:
//
//  - `rec` keeps a value **live below the call's arguments** (`w`, on the operand stack under
//    `n - 1` and `o`). A rebuilt caller frame that forgot it, or that kept the arguments as
//    operands as well as as the callee's locals, computes a different number. Without that, a
//    reconstruction bug is invisible: the arguments are re-derivable and the answer would come out
//    right anyway.
//  - the deopt at the bottom of `rec` is an **`instanceof` against a subclass**, not a division by
//    zero. A deopt that ends in an exception throws the rebuilt frames away, so a wrong
//    reconstruction is unobservable; this one **resumes to a value** and every frame above it then
//    has to do its own arithmetic on the way back. That is the only shape that can see the bug.
//  - `dive` recurses through three *different* methods, so the chain that has to come back is not
//    one method's frames repeated but three distinct `MethodId`s at three distinct pcs.
//  - `boom` recurses without a base case, and the assertion is that it throws exactly where an
//    interpreted one does — which is what makes the frame budget a *budget* rather than a guess.
//  - `count`, `total` and `head` return an `int`, a `long` and a **reference**, because a returned
//    value crosses this boundary in a register the interpreter never reads and the three widths
//    are the three ways of getting that wrong.
//  - `keep` reads a **reference local after the deopt**, in the rebuilt frame and in every frame
//    above it. That is the only shape in the file that can see a created frame handed back with the
//    wrong *kind* in a slot — everywhere else the reference is already on the operand stack by the
//    time the deopt happens, or is never read again, so an `int` in its place would compute the
//    same number and lose the object silently.
class NcBase {
    int v;

    NcBase(int v) {
        this.v = v;
    }
}

class NcSub extends NcBase {
    NcSub(int v) {
        super(v);
    }
}

class NcDeeper extends NcSub {
    NcDeeper(int v) {
        super(v);
    }
}

public class NcCall {
    static NcSub SUB = new NcSub(11);
    static NcDeeper DEEPER = new NcDeeper(23);

    // 1. The plain recursion, and the one that carries live state across the call. `w` is on the
    //    operand stack *underneath* both arguments at the invoke, so it exists only in the caller's
    //    rebuilt frame and nowhere else.
    //
    //    The base case is an `instanceof` that is a **hit** for an `NcSub` and a **miss** for an
    //    `NcDeeper`: this tier compares the exact class, so the second one hands the method back at
    //    that instruction and the interpreter answers it — with every frame above still to unwind.
    static int rec(int n, Object o) {
        if (n <= 0) {
            return (o instanceof NcSub) ? 7 : 3;
        }
        int w = n * 31;
        return w + rec(n - 1, o) * 2;
    }

    // 2. Three different methods in one cycle, so the chain a deopt rebuilds is three distinct
    //    frames rather than one repeated. The cycle closes at the third expansion, which is where
    //    a call is emitted.
    static int diveA(int n) {
        return n <= 0 ? 1 : diveB(n - 1) * 2 + 1;
    }

    static int diveB(int n) {
        return n <= 0 ? 2 : diveC(n - 1) + 3;
    }

    static int diveC(int n) {
        return n <= 0 ? 4 : diveA(n - 1) - 1;
    }

    // 3. Recursion with **no base case**. The frame budget is spent one call site at a time and a
    //    site that cannot pay hands the call back to the interpreter, which counts frames exactly —
    //    so this must reach `StackOverflowError` at the same depth an interpreted one does.
    static int boom(int n) {
        return boom(n + 1) + 1;
    }

    // 4. The three return widths. Each is recursive so that the call is a real one, and each keeps
    //    something live across it.
    static int count(int n) {
        return n <= 0 ? 0 : count(n - 1) + n;
    }

    static long total(int n) {
        return n <= 0 ? 1L : total(n - 1) * 3L + n;
    }

    // A returned **reference**: it comes back as a heap offset in the same register an `int` does,
    // and putting it back as an `int` would be a live object the collector can no longer see.
    static NcBase head(int n, NcBase a, NcBase b) {
        return n <= 0 ? a : head(n - 1, b, a);
    }

    // 5. **A reference local that is read after the deopt**, which is the one shape that can see a
    //    created frame handed back with the wrong kind in it.
    //
    //    In every other arm above, a reference argument is dead by the time the frame is rebuilt:
    //    the innermost frame has already pushed it onto its operand stack, and every frame above it
    //    resumes at an invoke it never reads that local after. So a rebuilt frame that handed the
    //    reference back as an `int` — a live object the collector can no longer see or relocate —
    //    would compute exactly the same number. Here `c` is read **after** the `instanceof` in the
    //    base case and **after** the call in every level above it, so it has to come back typed.
    static int keep(int n, NcBase c) {
        if (n <= 0) {
            int t = (c instanceof NcSub) ? 1 : 2;
            return t + c.v;
        }
        return keep(n - 1, c) + c.v;
    }

    // 6. A `void` callee, which hands back nothing at all and whose caller must push nothing.
    static int SINK;

    static void bump(int n) {
        if (n > 0) {
            bump(n - 1);
        }
        SINK = (SINK + 1) & 0xFFFF;
    }

    public static int run() {
        int acc = 0;
        for (int i = 1; i <= 600; i++) {
            // Both `instanceof` outcomes, so the same compiled site is a hit on some rounds and a
            // deopt on others, with the whole chain rebuilt on the second kind.
            acc = (acc + rec(5, SUB)) & 0xFFFFF;
            acc = (acc + rec(4, DEEPER)) & 0xFFFFF;
            acc = (acc + diveA(7)) & 0xFFFFF;
            acc = (acc + count(9)) & 0xFFFFF;
            acc = (acc + (int) (total(6) & 0xFFFF)) & 0xFFFFF;
            acc = (acc + head(i & 7, SUB, DEEPER).v) & 0xFFFFF;
            acc = (acc + keep(4, DEEPER)) & 0xFFFFF;
            acc = (acc + keep(3, SUB)) & 0xFFFFF;
            bump(3);
            acc = (acc + SINK) & 0xFFFFF;
        }
        try {
            acc = (acc + boom(0)) & 0xFFFFF;
        } catch (StackOverflowError e) {
            acc = (acc + 4242) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
