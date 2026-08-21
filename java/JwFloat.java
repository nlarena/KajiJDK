// Differential workload for the F3 JIT — dimension: **`float` and `double`**, and the four places
// IEEE-754 does something no integer type does.
//
// The `long` file (`JwSem`) is about a *wider* integer. This one is about a different kind of
// number, and every check below is a case where the value compares equal to something it is not,
// or where a plausible translation is one prefix byte away from the right instruction:
//
//   * **single is not double** — `1.0f/3.0f` and `1.0/3.0` differ, and `divss` and `divsd` differ
//     by one byte, so a mixed-up prefix computes the double's answer and looks fine;
//   * **NaN is unordered** — `fcmpg` and `fcmpl` exist only to differ here, and everything else
//     about them is identical;
//   * **zero is signed** — `-0.0 == 0.0` is true, so the only way to see the difference is to
//     divide by it, and `fneg` is defined as a sign-bit flip precisely so that it can produce one;
//   * **division does not throw** — `1.0f/0.0f` is infinity, not an `ArithmeticException`, so a
//     guard copied over from the integer arms would deopt on a perfectly ordinary value.
//
// One bit per observation: score = 524287 means all nineteen held. No `java.lang` constant is used
// (NaN and the infinities are computed), so nothing here depends on a class being initialised.
public class JwFloat {
    static float fadd(float a, float b) {
        return a + b;
    }

    static float fsub(float a, float b) {
        return a - b;
    }

    static float fmul(float a, float b) {
        return a * b;
    }

    static float fdiv(float a, float b) {
        return a / b;
    }

    static float fneg(float a) {
        return -a;
    }

    static float frem(float a, float b) {
        return a % b;
    }

    static double dadd(double a, double b) {
        return a + b;
    }

    static double dsub(double a, double b) {
        return a - b;
    }

    static double dmul(double a, double b) {
        return a * b;
    }

    static double ddiv(double a, double b) {
        return a / b;
    }

    static double dneg(double a) {
        return -a;
    }

    static double drem(double a, double b) {
        return a % b;
    }

    // `javac` compiles each of these to a compare opcode and a branch. Which of `fcmpl`/`fcmpg` it
    // picks depends on the direction of the test, which is exactly why both must be right.
    static int less(float a, float b) {
        return a < b ? 1 : 0;
    }

    static int greater(float a, float b) {
        return a > b ? 1 : 0;
    }

    static int dless(double a, double b) {
        return a < b ? 1 : 0;
    }

    static int dgreater(double a, double b) {
        return a > b ? 1 : 0;
    }

    static float widenInt(int a) {
        return a;
    }

    static double widenIntD(int a) {
        return a;
    }

    static float widenLong(long a) {
        return a;
    }

    static double widenLongD(long a) {
        return a;
    }

    static double toDouble(float a) {
        return a;
    }

    static float toFloat(double a) {
        return (float) a;
    }

    // A mix of widths in one method: a `float` local between two `double`s, so any off-by-one in
    // the category-2 slot arithmetic lands on a live variable.
    static double mixed(double a, float b, double c) {
        double t = a + c;
        float u = b + 1.0f;
        return t * u;
    }

    static double warm() {
        double t = 0;
        for (int i = 0; i < 64; i++) {
            t += fadd(i, 1.0f) + fsub(i, 3.0f) + fmul(i, 2.0f) + fdiv(i, 4.0f) + fneg(i);
            t += dadd(i, 1.0) + dsub(i, 3.0) + dmul(i, 2.0) + ddiv(i, 4.0) + dneg(i);
            t += less(i, 32.0f) + greater(i, 32.0f) + dless(i, 32.0) + dgreater(i, 32.0);
            t += widenInt(i) + widenIntD(i) + widenLong(i) + widenLongD(i);
            t += toDouble(i) + toFloat(i) + mixed(i, i, i);
            t += frem(i, 3.0f) + drem(i, 3.0);
        }
        return t;
    }

    static int run() {
        int score = (int) (warm() * 0);
        float fnan = fdiv(0.0f, 0.0f);
        float finf = fdiv(1.0f, 0.0f);
        double dnan = ddiv(0.0, 0.0);
        double dinf = ddiv(1.0, 0.0);

        // --- ordinary arithmetic, at the precision the type actually has --------------------
        if (fadd(1.5f, 2.25f) == 3.75f) { score += 1; }
        if (fsub(1.5f, 2.25f) == -0.75f) { score += 2; }
        if (fmul(1.5f, 2.0f) == 3.0f) { score += 4; }
        if (dadd(1.5, 2.25) == 3.75) { score += 8; }
        if (dmul(1.5, 2.0) == 3.0) { score += 16; }
        // **Single precision really is single.** These two disagree, and `divss`/`divsd` differ by
        // one prefix byte — so a mix-up computes the double's value and every equality above holds.
        if ((double) fdiv(1.0f, 3.0f) != ddiv(1.0, 3.0)) { score += 32; }
        if (fadd(16777216.0f, 1.0f) == 16777216.0f) { score += 64; }
        if (dadd(16777216.0, 1.0) == 16777217.0) { score += 128; }

        // --- division does not throw ---------------------------------------------------------
        if (finf > 0.0f && fdiv(-1.0f, 0.0f) < 0.0f) { score += 256; }
        if (fnan != fnan) { score += 512; }
        if (dinf > 0.0 && dnan != dnan) { score += 1024; }

        // --- NaN is unordered, and that is all `fcmpl` and `fcmpg` disagree about --------------
        // Both of these must be false, whichever compare opcode `javac` chose.
        if (less(fnan, 1.0f) == 0 && greater(fnan, 1.0f) == 0) { score += 2048; }
        if (less(1.0f, fnan) == 0 && greater(1.0f, fnan) == 0) { score += 4096; }
        if (dless(dnan, 1.0) == 0 && dgreater(dnan, 1.0) == 0) { score += 8192; }
        // ...while the ordered cases still work.
        if (less(1.0f, 2.0f) == 1 && greater(2.0f, 1.0f) == 1 && less(2.0f, 2.0f) == 0) { score += 16384; }

        // --- zero is signed, and `fneg` is a sign-bit flip -------------------------------------
        // `-(0.0f)` is `-0.0f`, which `== 0.0f` cannot see -- dividing into it is what can.
        if (fdiv(1.0f, fneg(0.0f)) < 0.0f) { score += 32768; }
        if (ddiv(1.0, dneg(0.0)) < 0.0) { score += 65536; }

        // --- widening conversions, and `%` (which deopts rather than computing) ----------------
        boolean conversions = widenInt(16777217) == 16777216.0f
            && widenIntD(16777217) == 16777217.0
            && widenLongD(9007199254740993L) == 9007199254740992.0
            && toDouble(0.5f) == 0.5
            && toFloat(1.0e300) == fdiv(1.0f, 0.0f);
        if (conversions && frem(7.5f, 2.0f) == 1.5f && drem(-7.5, 2.0) == -1.5) { score += 131072; }
        if (mixed(4.0, 2.0f, 1.0) == 15.0) { score += 262144; }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
