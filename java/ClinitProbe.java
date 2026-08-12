// A6 loose end: a static initializer that throws. First active use of Boom triggers its <clinit>,
// which throws a RuntimeException. Per JVMS §5.5 the VM must (a) propagate the failure to the
// triggering code, (b) wrap the non-Error in ExceptionInInitializerError, and (c) leave Boom
// erroneous so a *second* active use throws NoClassDefFoundError. Score: 3 (wrapped) + 5 (NCDFE on
// re-use) = 8. Before the fix the failure was swallowed and the read returned 0. Deterministic →
// green ≡ os-gil ≡ os = 8.
public class ClinitProbe {
    static int run() {
        int score = 0;
        try {
            int x = Boom.VALUE; // triggers Boom.<clinit> → throws
            score += x; // unreached
        } catch (ExceptionInInitializerError e) {
            score += 3; // (a)+(b): failure propagated, wrapped as ExceptionInInitializerError
        }
        try {
            int y = Boom.VALUE; // second use of the now-erroneous class
            score += y; // unreached
        } catch (NoClassDefFoundError e) {
            score += 5; // (c): erroneous class → NoClassDefFoundError
        }
        return score; // 8
    }
}

class Boom {
    static final int VALUE = compute();

    static int compute() {
        if (VALUE >= 0) { // VALUE is still 0 (default) while <clinit> runs → always true
            throw new RuntimeException("init boom");
        }
        return 1;
    }
}
