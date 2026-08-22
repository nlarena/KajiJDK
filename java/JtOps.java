// Differential workload for the F3 JIT — dimension: **every opcode in the compiled subset**.
//
// Each helper below is a static method whose whole body lies in the whitelist `burst::compile`
// accepts (int constants, int locals, int arithmetic, bits, shifts, comparisons, branches,
// `ireturn`), so each is compiled once it goes hot. `run()` itself never is: it is entered once
// and it is full of `invokestatic`.
//
// The point of the file is coverage, not speed: it drives every arm of the compiler's `match` with
// operands that make the arms distinguishable (negative values, so a signed/unsigned condition-code
// mix-up shows; a divisor that varies, so the zero check is exercised; shift counts that are not
// constant). The test runs it twice — JIT on and JIT off — and asserts the same answer.
public class JtOps {
    static int arith(int a, int b) {
        return (a + b) * (a - b) - (-a);
    }

    static int bits(int a, int b) {
        return (a & b) + (a | b) + (a ^ b);
    }

    static int shifts(int a, int b) {
        return (a << b) + (a >> b) + (a >>> b);
    }

    static int divrem(int a, int b) {
        return a / b + a % b;
    }

    // Every conditional branch in the subset, on both operand shapes: `if_icmp<cond>` (two stack
    // operands) and `if<cond>` (one operand against zero). A signed/unsigned condition-code mix-up
    // in the compiler flips the answer for any negative `a`, which the caller supplies.
    //
    // The result is accumulated as `r = r + r` then `r += 1`, one bit per test, rather than as
    // `r += 1, 2, 4, ...`: a `+= 256` would make javac emit `wide iinc`, which is **not** in the
    // compiled subset, and the whole method would then be refused — testing nothing. Keeping every
    // increment at 1 keeps the branches themselves under test, which is the point of the method.
    static int compare(int a, int b) {
        int r = 0;
        r = r + r; if (a == b) { r += 1; }
        r = r + r; if (a != b) { r += 1; }
        r = r + r; if (a < b) { r += 1; }
        r = r + r; if (a >= b) { r += 1; }
        r = r + r; if (a > b) { r += 1; }
        r = r + r; if (a <= b) { r += 1; }
        r = r + r; if (a == 0) { r += 1; }
        r = r + r; if (a != 0) { r += 1; }
        r = r + r; if (a < 0) { r += 1; }
        r = r + r; if (a >= 0) { r += 1; }
        r = r + r; if (a > 0) { r += 1; }
        r = r + r; if (a <= 0) { r += 1; }
        return r;
    }

    // Left-associative addition of non-folding terms, so javac emits one push per constant:
    // iconst_m1..iconst_5, bipush (both signs), sipush (both signs) and ldc (an Integer that fits
    // in neither) — the whole constant group of the subset in one method.
    static int consts(int a) {
        return a + (-1) + 0 + 1 + 2 + 3 + 4 + 5 + 100 + (-128) + 30000 + (-30000) + 900000 + (-900000);
    }

    // `x = y = a` is the one place javac emits `dup` for plain int locals.
    static int chain(int a, int b) {
        int x;
        int y;
        x = y = a;
        return x + y + b;
    }

    // A loop, so the compiled code has a real back-edge and the branch fixups run backwards.
    static int loop(int n) {
        int acc = 1;
        for (int i = 0; i < n; i++) {
            acc = acc + i;
            acc = acc ^ (acc >> 3);
            if ((i & 7) == 0) {
                acc = acc - 3;
            }
        }
        return acc & 0xFFFF;
    }

    static int run() {
        int acc = 0;
        for (int i = 0; i < 200; i++) {
            int a = i * 37 - 3000;
            int b = (i & 15) + 1;
            acc = (acc + arith(a, b)) & 0xFFFFFF;
            acc = (acc + bits(a, b)) & 0xFFFFFF;
            acc = (acc + shifts(a, b)) & 0xFFFFFF;
            acc = (acc + divrem(a, b)) & 0xFFFFFF;
            acc = (acc + compare(a, b)) & 0xFFFFFF;
            acc = (acc + consts(a)) & 0xFFFFFF;
            acc = (acc + chain(a, b)) & 0xFFFFFF;
            acc = (acc + loop(b)) & 0xFFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
