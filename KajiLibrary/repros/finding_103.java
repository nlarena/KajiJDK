// Finding #103 — missing int→long widening (`i2l`) on assignment and call arguments.
// An `int` value (literal or expression) used where a `long` is required must be widened
// with `i2l` (JLS §5.1.2). The frozen javac omits it: only an explicit `long` literal
// (`5L`) loads correctly (`ldc2_w`). The lenient interpreter masks it for arithmetic, but
// any consumer that distinguishes int from long (a native, the real JVM verifier) breaks.
public class finding_103 {
    static Object lock = new Object();
    static void demo() throws InterruptedException {
        long t = 5;         // emitted: iconst_5; lstore_0   (should be: iconst_5; i2l; lstore_0)
        lock.wait(5);       // emitted: iconst_5; invokevirtual wait:(J)V   (missing i2l)
        lock.wait(t);       // t itself is an int-in-a-long-slot, so this passes an int too
        lock.wait(5L);      // CORRECT: ldc2_w 5L; invokevirtual wait:(J)V
    }
}
