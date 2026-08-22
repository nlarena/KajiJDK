// A7 #8: Class.getName()/getSimpleName() — minimal reflection. getClass() hands back the
// mirror; getName() reads the class name off it in JDK format (dotted binary name), so this
// unpackaged class reports "GnTest" and the `String.class` literal reports "java.lang.String";
// getSimpleName() yields the trailing segment. Deterministic score 10 + 12 + 20 →
// green ≡ os-gil ≡ os = 42.
public class GnTest {
    static int run() {
        int score = 0;
        String n = new GnTest().getClass().getName();
        if ("GnTest".equals(n)) {
            score += 10;
        }
        String s = String.class.getName();
        if ("java.lang.String".equals(s)) {
            score += 12;
        }
        String p = new GnTest().getClass().getSimpleName();
        if ("GnTest".equals(p)) {
            score += 20;
        }
        return score; // 42
    }
}
