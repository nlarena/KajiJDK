// Differential workload for the F3 JIT — dimension: **the three `int`-semantics traps**.
//
// The compiler works in 64-bit registers; a Java `int` is 32-bit with wraparound. Every check below
// is a place where the naive 64-bit translation gives a *plausible but wrong* answer, so a bug here
// would never show up as a crash — only as a silently different number. Each is asked through a
// separate static method, so each is compiled in its own right.
//
// `warm()` runs first and only exists to push every helper past the JIT's invocation threshold: the
// interesting calls in `run()` happen once each, which is not hot, so without it the whole file
// would test the interpreter twice.
//
// One bit per observation, so a failure names itself: score = 16383 means all fourteen held.
public class JtSem {
    static int add(int a, int b) {
        return a + b;
    }

    static int mul(int a, int b) {
        return a * b;
    }

    static int sub(int a, int b) {
        return a - b;
    }

    static int neg(int a) {
        return -a;
    }

    static int inc(int a) {
        int x = a;
        x++;
        return x;
    }

    static int shl(int a, int b) {
        return a << b;
    }

    static int shr(int a, int b) {
        return a >> b;
    }

    static int ushr(int a, int b) {
        return a >>> b;
    }

    static int div(int a, int b) {
        return a / b;
    }

    static int rem(int a, int b) {
        return a % b;
    }

    static int warm() {
        int t = 0;
        for (int i = 0; i < 64; i++) {
            t = t + add(i, 1) + mul(i, 2) + sub(i, 3) + neg(i) + inc(i);
            t = t + shl(i, 1) + shr(i, 1) + ushr(i, 1);
            t = t + div(i + 1, 3) + rem(i + 1, 3);
        }
        return t;
    }

    static int run() {
        int score = warm() * 0;

        // Trap 1 — the normalisation invariant. Each of these overflows 32 bits, and each is
        // computed in a 64-bit register that would happily hold the un-wrapped result.
        if (add(Integer.MAX_VALUE, 1) == Integer.MIN_VALUE) { score += 1; }
        if (mul(65536, 65536) == 0) { score += 2; }
        if (sub(Integer.MIN_VALUE, 1) == Integer.MAX_VALUE) { score += 8192; }
        if (neg(Integer.MIN_VALUE) == Integer.MIN_VALUE) { score += 4; }
        if (inc(Integer.MAX_VALUE) == Integer.MIN_VALUE) { score += 8; }

        // Trap 2 — shift counts are masked to 5 bits (x86 masks 64-bit shifts to 6), and `>>>`
        // is a *logical* shift of the 32-bit value (a sign-extended -1 in 64 bits would keep its
        // top bits and answer -1).
        if (shl(1, 33) == 2) { score += 16; }
        if (shr(-1, 33) == -1) { score += 32; }
        if (ushr(-1, 1) == Integer.MAX_VALUE) { score += 64; }
        if (ushr(-1, 33) == Integer.MAX_VALUE) { score += 128; }
        if (ushr(-1, 0) == -1) { score += 256; }

        // Trap 3 — division. `MIN_VALUE / -1` overflows (a 32-bit idiv would fault); a zero
        // divisor must raise ArithmeticException rather than a hardware #DE, which is what the
        // JIT's deopt path is for: native code gives up and the interpreter throws.
        if (div(Integer.MIN_VALUE, -1) == Integer.MIN_VALUE) { score += 512; }
        if (rem(Integer.MIN_VALUE, -1) == 0) { score += 1024; }
        try {
            div(1, 0);
        } catch (ArithmeticException e) {
            score += 2048;
        }
        try {
            rem(1, 0);
        } catch (ArithmeticException e) {
            score += 4096;
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
