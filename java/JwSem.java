// Differential workload for the F3 JIT — dimension: **`long`, and how its semantics differ from
// `int`'s in exactly the places the emitter has to treat them differently**.
//
// The `int` twin of this file is `JtSem`, and the contrast is the point. There, every check is a
// case where a naive *64-bit* translation of a 32-bit type gives a plausible wrong answer. Here,
// every check is a case where carrying an `int`'s habits over to a `long` gives one:
//
//   * a `movsxd` after the arithmetic (right for `int`, catastrophic for `long`);
//   * a 5-bit shift mask (right for `int`, wrong for `long`, which masks to 6);
//   * an unguarded `idiv` (safe for `int` in 64-bit registers, a hardware `#DE` for `long`).
//
// Each observation is one bit, so a failure names itself: score = 1048575 means all twenty held.
// `warm()` runs first and only exists to push every helper past the JIT's invocation threshold —
// the interesting calls in `run()` happen once each, which is not hot.
public class JwSem {
    static long add(long a, long b) {
        return a + b;
    }

    static long sub(long a, long b) {
        return a - b;
    }

    static long mul(long a, long b) {
        return a * b;
    }

    static long neg(long a) {
        return -a;
    }

    static long div(long a, long b) {
        return a / b;
    }

    static long rem(long a, long b) {
        return a % b;
    }

    static long shl(long a, int b) {
        return a << b;
    }

    static long shr(long a, int b) {
        return a >> b;
    }

    static long ushr(long a, int b) {
        return a >>> b;
    }

    static long and(long a, long b) {
        return a & b;
    }

    static long or(long a, long b) {
        return a | b;
    }

    static long xor(long a, long b) {
        return a ^ b;
    }

    static int cmp(long a, long b) {
        // `javac` compiles this to `lcmp` followed by branches, which is how every `long`
        // comparison in Java is spelled.
        if (a < b) { return -1; }
        if (a > b) { return 1; }
        return 0;
    }

    static long widen(int a) {
        return a;
    }

    static int narrow(long a) {
        return (int) a;
    }

    // A method mixing the two widths, with an `int` local *between* two `long`s so that any
    // off-by-one in the category-2 slot arithmetic lands on a live variable rather than on padding.
    static long mixed(long a, int b, long c) {
        long t = a + c;
        int u = b + 1;
        return t * u;
    }

    static long warm() {
        long t = 0;
        for (int i = 0; i < 64; i++) {
            t += add(i, 1) + sub(i, 3) + mul(i, 2) + neg(i);
            t += shl(i, 1) + shr(i, 1) + ushr(i, 1);
            t += and(i, 3) + or(i, 4) + xor(i, 5);
            t += div(i + 1, 3) + rem(i + 1, 3);
            t += cmp(i, 32) + widen(i) + narrow(i) + mixed(i, i, i);
        }
        return t;
    }

    static int run() {
        int score = (int) (warm() * 0);

        // --- overflow: 64-bit wraparound, which a `movsxd` after the operation would destroy ---
        if (add(Long.MAX_VALUE, 1) == Long.MIN_VALUE) { score += 1; }
        if (sub(Long.MIN_VALUE, 1) == Long.MAX_VALUE) { score += 2; }
        if (mul(4294967296L, 4294967296L) == 0) { score += 4; }
        if (neg(Long.MIN_VALUE) == Long.MIN_VALUE) { score += 8; }
        // ...and the control: a value that fits 32 bits must NOT wrap there.
        if (add(2147483647L, 1) == 2147483648L) { score += 16; }
        if (mul(4000000000L, 3) == 12000000000L) { score += 32; }

        // --- division: `Long.MIN_VALUE / -1` really does raise #DE on x86-64 ------------------
        if (div(Long.MIN_VALUE, -1) == Long.MIN_VALUE) { score += 64; }
        if (rem(Long.MIN_VALUE, -1) == 0) { score += 128; }
        if (div(12000000000L, 3) == 4000000000L) { score += 256; }
        if (rem(-7L, 2L) == -1) { score += 512; }
        try {
            div(1, 0);
        } catch (ArithmeticException e) {
            score += 1024;
        }
        try {
            rem(1, 0);
        } catch (ArithmeticException e) {
            score += 2048;
        }

        // --- shifts: masked to SIX bits, not five --------------------------------------------
        if (shl(1L, 33) == 8589934592L) { score += 4096; }
        if (shl(1L, 65) == 2L) { score += 8192; }
        if (shr(Long.MIN_VALUE, 63) == -1L) { score += 16384; }
        if (ushr(-1L, 1) == Long.MAX_VALUE) { score += 32768; }

        // --- lcmp, in all three of its answers, across the whole 64-bit range -----------------
        if (cmp(Long.MIN_VALUE, Long.MAX_VALUE) == -1) { score += 65536; }
        if (cmp(4294967296L, 0L) == 1) { score += 131072; }

        // --- conversions and mixing ------------------------------------------------------------
        if (widen(Integer.MIN_VALUE) == -2147483648L && narrow(4294967297L) == 1) { score += 262144; }
        if (mixed(4000000000L, 2, 1000000000L) == 15000000000L) { score += 524288; }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
