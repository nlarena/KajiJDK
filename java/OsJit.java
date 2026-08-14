// Differential workload for the F3 JIT, **step 3** — dimension: on-stack replacement.
//
// Everything here is a loop, because a loop is the only thing step 3 added: the back-edge counter
// that notices one, the entry points a compiled method gains at its headers, and the safepoint
// poll it leaves through. Step 2's workloads (JtOps, JtSem, JtLoop) all reach native code through
// a *call*; not one of these does — several of them are entered exactly once.
//
// Every method is a pure function of its int arguments, so each answer is a fact about arithmetic
// and about nothing else, and the test can assert it against a real `java OsJit` run. `run()`
// itself is full of `invokestatic` and is never compiled.
public class OsJit {

    // ---- The shape the whole step exists for -------------------------------------------------
    // Entered once, loops 300 000 times inside. An invocation counter can never see this method
    // get hot; a back-edge counter sees it after 32 iterations. This is `BmLoop`'s shape.
    static int longLoop() {
        int acc = 1;
        for (int i = 0; i < 300000; i++) {
            acc = acc + i;
            acc = acc ^ (acc >> 7);
            if ((i & 15) == 0) {
                acc = acc - 3;
            }
        }
        return acc & 0xFFFFF;
    }

    // ---- Leaving the loop from the middle -----------------------------------------------------
    // The `return` is *inside* the body, so the method's exit is not the loop's own exit edge.
    // An on-stack entry has to be able to finish through it — and the answer depends on exactly
    // how many iterations ran, so an entry that silently restarted at `acc = 0` would show.
    static int earlyExit(int limit) {
        int acc = 0;
        for (int i = 1; i < 1000000; i++) {
            acc = acc + i;
            if (acc > limit) {
                return acc * 2 - i;
            }
        }
        return -1;
    }

    // The same loop reached with the exit condition already false: it must fall straight through
    // without executing the body once, whichever engine runs it.
    static int neverEnters(int n) {
        int acc = 7;
        for (int i = n; i < 0; i++) {
            acc = acc * 3;
        }
        return acc;
    }

    // ---- Nested loops -------------------------------------------------------------------------
    // Two headers, so two entry points and two poll sites, and the outer one is reached with the
    // inner induction variable holding whatever the inner loop left. Getting the entry dispatch
    // wrong by one header answers a different number.
    static int nested(int rows, int cols) {
        int acc = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                acc = acc + (i ^ j);
                acc = acc & 0xFFFFFF;
            }
            acc = acc + i;
        }
        return acc;
    }

    // Three deep, with the innermost loop's trip count depending on both outer variables — so a
    // header entered with stale locals diverges immediately rather than by luck.
    static int triple(int n) {
        int acc = 1;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                for (int k = 0; k < j; k++) {
                    acc = (acc + i * j - k) & 0x3FFFFF;
                }
            }
        }
        return acc;
    }

    // ---- A loop that cannot be compiled at all ------------------------------------------------
    // `array[i]` is outside the subset, so this method is scanned once from its back-edge and
    // refused for good. It must keep being interpreted — and keep giving the same answer — while
    // its neighbours are compiled around it. This is the "no entry point here, carry on" path.
    static int uncompilable(int n) {
        int[] a = new int[16];
        int acc = 0;
        for (int i = 0; i < n; i++) {
            a[i & 15] = a[i & 15] + i;
            acc = acc + a[i & 15];
        }
        return acc & 0xFFFFF;
    }

    // ---- A loop whose body can deopt ----------------------------------------------------------
    // The divisor reaches zero on iteration 500, so native code gives up in the middle of the
    // loop. Nothing observable was written, so the interpreter simply carries on from the header
    // it was entered at, re-runs those iterations and throws a real ArithmeticException by the
    // ordinary path — which the caller catches.
    //
    // Split in two on purpose: a `try`/`catch` in the same method would put an `astore` of the
    // exception object in its body, and the whole method would be outside the subset and never
    // compiled — so the deopt path would never be reached at all.
    static int divLoop(int n) {
        int acc = 0;
        for (int i = 0; i < 100000; i++) {
            acc = acc + n / (i - 500);
        }
        return acc;
    }

    static int deopting(int n) {
        try {
            return divLoop(n);
        } catch (ArithmeticException e) {
            return 12345;
        }
    }

    static int run() {
        int acc = longLoop();
        acc = (acc + earlyExit(1000)) & 0xFFFFFF;
        acc = (acc + earlyExit(999999)) & 0xFFFFFF;
        acc = (acc + neverEnters(3)) & 0xFFFFFF;
        acc = (acc + nested(300, 300)) & 0xFFFFFF;
        acc = (acc + triple(120)) & 0xFFFFFF;
        acc = (acc + uncompilable(5000)) & 0xFFFFFF;
        acc = (acc + deopting(7)) & 0xFFFFFF;
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
