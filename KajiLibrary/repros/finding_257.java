/**
 * A `finally` block is emitted ONCE and shared, so when a `catch` clause completes normally it
 * falls into the EXCEPTIONAL copy of the finally — which starts by storing a pending throwable
 * that is not on the stack. The frame underflows.
 *
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_257.java
 *   bin/jvm.exe -v KajiLibrary/repros/finding_257.class
 *   bin/run-headless.exe KajiLibrary/repros/finding_257.class atrapaYLimpia
 *
 * Emitted for `atrapaYLimpia`, with the catch body starting at 14:
 *
 *     14: astore_0            <- catch: store the caught exception
 *     15: ...catch body...
 *     23: astore_1            <- and now it FALLS INTO the exceptional finally, which
 *                                pops a throwable that nobody pushed
 *     24: ...finally body...
 *     34: athrow              <- so a caught exception would be rethrown
 *     35: ...code after the statement, unreachable from the catch...
 *
 * What javac emits instead is TWO copies of the finally body: one inlined at the end of the
 * catch, followed by a jump past the handler, and one in the handler itself.
 *
 * The controls below isolate it: every other combination works. Only "the catch actually
 * fires, and there is a finally" is broken.
 *
 *   normal            try/finally, nothing thrown            -> 11
 *   conSalto          try/finally with a return in the try   -> 1
 *   soloCatch         try/catch, no finally                  -> 1
 *   finallyConThrow   try/finally, exception propagating out -> 11
 *   sinFallo          try/catch/finally, nothing thrown      -> 11
 *   atrapaYLimpia     try/catch/finally, exception CAUGHT    -> panics
 */
public class finding_257 {

    static int trace;

    /** try/finally, normal path. */
    public static int normal() {
        finding_257.trace = 0;
        try {
            finding_257.trace = finding_257.trace + 1;
        } finally {
            finding_257.trace = finding_257.trace + 10;
        }
        return finding_257.trace;
    }

    /** try/finally with an early return inside the try. */
    public static int conSalto() {
        finding_257.trace = 0;
        try {
            return 1;
        } finally {
            finding_257.trace = finding_257.trace + 10;
        }
    }

    /** try/catch and no finally. */
    public static int soloCatch() {
        finding_257.trace = 0;
        try {
            throw new IllegalStateException("x");
        } catch (IllegalStateException e) {
            finding_257.trace = finding_257.trace + 1;
        }
        return finding_257.trace;
    }

    /** try/finally whose try throws, caught by the CALLER: the finally still runs. */
    public static int finallyConThrow() {
        finding_257.trace = 0;
        try {
            finding_257.boom();
        } catch (IllegalStateException e) {
            finding_257.trace = finding_257.trace + 1;
        }
        return finding_257.trace;
    }

    static void boom() {
        try {
            throw new IllegalStateException("x");
        } finally {
            finding_257.trace = finding_257.trace + 10;
        }
    }

    /** try/catch/finally with nothing thrown -- the catch never runs, and it works. */
    public static int sinFallo() {
        finding_257.trace = 0;
        try {
            finding_257.trace = finding_257.trace + 1;
        } catch (IllegalStateException e) {
            finding_257.trace = finding_257.trace + 100;
        } finally {
            finding_257.trace = finding_257.trace + 10;
        }
        return finding_257.trace;
    }

    /** The broken one: the catch fires and there is a finally after it. */
    public static int atrapaYLimpia() {
        finding_257.trace = 0;
        try {
            throw new IllegalStateException("x");
        } catch (IllegalStateException e) {
            finding_257.trace = finding_257.trace + 1;
        } finally {
            finding_257.trace = finding_257.trace + 10;
        }
        return finding_257.trace;
    }
}
