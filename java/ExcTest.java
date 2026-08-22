// A6 loose ends: Throwable message + stack trace. `deep` recurses and throws a RuntimeException
// with a detail message several frames down; the catch reads it back. getMessage() returns the
// message, toString() renders "pkg.Class: message" (VM reads the runtime class name), and the VM
// captured a backtrace at throw time so printStackTrace() has frames to print. Deterministic score
// (10 for the message + 20 for toString) → green ≡ os-gil ≡ os = 30.
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
            e.printStackTrace(); // side effect: prints the header + captured "\tat ..." frames
        }
        return score; // 30
    }
}
