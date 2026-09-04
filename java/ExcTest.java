// A6 loose ends: Throwable message. `deep` recurses and throws a RuntimeException with a detail
// message several frames down; the catch reads it back. getMessage() returns the message and
// toString() renders "pkg.Class: message" (VM reads the runtime class name). Deterministic score
// (10 for the message + 20 for toString) → green ≡ os-gil ≡ os = 30.
//
// NOT a stack trace test, despite the name. This header used to claim the VM captured a backtrace
// at throw time so printStackTrace() had frames to print. That is false: this VM has no stack
// introspection at all, and getStackTrace() returns 0 frames both for a Throwable that was merely
// constructed and for one that was thrown and caught (repro: scratchpad/zz347/Pila.java and
// Pila2.java). The printStackTrace() below prints the header line and nothing else. The score never
// depended on it -- 30 is 10 + 20 -- which is why the wrong claim survived unnoticed.
public class ExcTest {
    static int deep(int n) {
        if (n == 0) {
            throw new RuntimeException("boom");
        }
        return deep(n - 1);
    }

    static int run() {
        int score = 0;
        try {
            deep(3); // throws four frames below run()
        } catch (RuntimeException e) {
            String m = e.getMessage();
            if ("boom".equals(m)) {
                score += 10;
            }
            String s = e.toString();
            if ("java.lang.RuntimeException: boom".equals(s)) {
                score += 20;
            }
            e.printStackTrace(); // side effect: prints the header line only -- there are no frames
        }
        return score; // 30
    }
}
