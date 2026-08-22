// F3 group 5 — **`athrow` in compiled code**, which is a deopt and nothing else.
//
// The claim under test is that native code contains no exception handling whatsoever: an `athrow`
// hands the method back at its own pc with the exception left on the reconstructed operand stack,
// and the *interpreter* re-executes that very instruction and does the real unwinding — handler
// tables, monitors, backtrace. Three shapes, each of which fails differently if that is wrong:
//
//  - `caughtHere` throws and catches in the **same method**, so the handler that fires is the one
//    belonging to the frame the deopt rebuilt (the `JdWrite.guarded` shape, one step earlier in the
//    instruction). A method with an exception table compiles but gets no OSR and no polls; this is
//    the test that an *exit* through such a method is nevertheless fine.
//  - `propagates` throws with **nothing in this method to catch it**, so the exception has to cross
//    a frame boundary that a deopt has just rebuilt. `catcher` catches it one frame up, and is
//    itself compiled — when `propagates` is inlined into it, the two frames the deopt hands back
//    are exactly the two frames the throw must travel between.
//  - `deepThrow` throws with the operand stacks of **both** frames live: five `int`s and a
//    reference in the caller, and the exception itself in the callee. If the spill named the wrong
//    register, or wrote the exception back as an `int`, the `catch` sees something else — or the
//    collector sees a pointer made of arithmetic.
//
// Two choices here are about **detectability** rather than about coverage, and both were made
// after checking that the obvious version could not fail:
//
//  - the exception is an `IllegalArgumentException` and every `catch` names that type. With a plain
//    `RuntimeException` a deopt that handed the interpreter `null` instead of the exception would
//    throw a `NullPointerException` — which *is* a `RuntimeException`, so the same handler would
//    run, the same number would come out, and the bug would be invisible. A narrower type makes
//    the substitution escape the method. (It has to be one of the classes this VM ships in
//    `boot/java/lang/`, which is why it is not the more obvious `IllegalStateException`.)
//  - the two outer `catch` arms compare the caught object's **identity** against the field it came
//    from. Type alone does not distinguish "the exception" from "some other object of that type",
//    and a spill that named the wrong register would produce exactly that.
//
// Every arm folds into one score, so the interpreted run (`JVM_JIT=0`) is the oracle for all of
// them, and the real `java` of JDK 25 is the oracle for that.
class JeCell {
    int v;
    IllegalArgumentException boom;

    JeCell(int v) {
        this.v = v;
        this.boom = new IllegalArgumentException("je");
    }
}

public class JeThrow {
    // 1. Thrown and caught in the very frame the deopt rebuilt. `throw c.boom` is
    //    `aload; getfield; athrow` — no allocation, so the throw itself is three subset opcodes.
    static int caughtHere(JeCell c, int k) {
        int s;
        try {
            s = c.v + 1;
            if (k == 0) {
                throw c.boom;
            }
            s = s + 100 / k;
        } catch (IllegalArgumentException e) {
            s = -1;
        }
        return s;
    }

    // 2. Nothing here catches: the exception leaves this method entirely.
    static int propagates(JeCell c, int k) {
        if (k == 0) {
            throw c.boom;
        }
        return c.v + k;
    }

    // ...and this is the frame that catches it, one level up.
    static int catcher(JeCell c, int k) {
        try {
            return propagates(c, k);
        } catch (IllegalArgumentException e) {
            return -7;
        }
    }

    // 3. The throw, deliberately shaped so that inlining it leaves the caller's operand stack live.
    static int bang(JeCell cell, int k) {
        if (k == 0) {
            throw cell.boom;
        }
        return k * 2;
    }

    static int use(JeCell c, int t) {
        return c.v + t;
    }

    // `y` is pushed as an argument of `use` *before* `bang` runs, so at the athrow the caller frame
    // holds five cached `int`s and a **reference** on its operand stack, and the callee frame holds
    // the exception. Both have to come back typed correctly.
    static int deepThrow(int a, int b, int c, int d, int e, JeCell x, JeCell y, int k) {
        return a + (b + (c + (d + (e + use(y, bang(x, k))))));
    }

    public static int run() {
        JeCell p = new JeCell(5);
        JeCell q = new JeCell(9);
        int acc = 0;
        for (int i = 1; i <= 4000; i++) {
            // Every seventh call throws, and the same method's own handler catches.
            acc = (acc + caughtHere(p, i % 7)) & 0xFFFFF;
            // Every fifth call throws one frame down and is caught here.
            acc = (acc + catcher(q, i % 5)) & 0xFFFFF;

            int a = i & 0x3F;
            int b = (i >> 2) & 0x1F;
            int c = (i * 7) & 0xFF;
            int d = (i & 0x0F) + 1;
            int e = (i >> 5) & 0x3F;
            // Every eleventh call throws with both operand stacks live. The catch checks the
            // exception's **identity**, not just its type: the object that comes out has to be the
            // very one the field held, which is what a spill of the wrong register would break
            // without changing which handler runs.
            try {
                acc = (acc + deepThrow(a, b, c, d, e, p, q, i % 11)) & 0xFFFFF;
            } catch (IllegalArgumentException ex) {
                acc = (acc + (ex == p.boom ? 3 : 1000)) & 0xFFFFF;
            }
            // Every thirteenth call throws into an interpreted frame.
            try {
                acc = (acc + propagates(p, i % 13)) & 0xFFFFF;
            } catch (IllegalArgumentException ex) {
                acc = (acc + (ex == p.boom ? 5 : 2000)) & 0xFFFFF;
            }
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
