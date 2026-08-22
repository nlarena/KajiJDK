// F3 step 10 — **a deopt taken with the operand stack live in registers.**
//
// This is the delicate half of the step. A deopt stub has to hand the interpreter the operand stack
// as it was on entry to the instruction that could not proceed; before step 10 it copied those
// values out of frame slots, and now most of them are in R8-R15. The stub therefore spills *from
// the registers that belong to that pc*, which is decidable because the depth at every pc is
// already in the type map.
//
// The programs below are written so that a wrong answer is impossible to miss and impossible to
// get by luck:
//
//  - `guarded` divides by an operand that is zero on some calls, with **nine** values live: eight
//    in registers and one in a frame slot. The interpreter re-executes that `idiv` using the
//    spilled stack, so if the spill named the wrong register the divisor it re-reads is not zero,
//    no `ArithmeticException` is thrown, and the `catch` never runs — a different number.
//  - `bounded` is the same shape around an `iaload` whose index walks off the end, which is a guard
//    with *three* operands of its own under a deep stack — and the index the interpreter re-reads
//    decides whether it throws at all.
//  - `nulled` deopts on a `getfield` of a null receiver with a deep stack under it.
//
// Every arm's result is folded into the score, so the interpreted run is the oracle for all three.
class JgCell {
    int v;

    JgCell(int v) {
        this.v = v;
    }
}

public class JgGuard {
    // Nine operands live at the `idiv`: positions 0-7 cached, position 8 in a frame slot.
    static int guarded(int a, int b, int c, int d, int e, int k) {
        return a + (b + (c + (d + (e + (a + (b + (c / k)))))));
    }

    // Same, one level shallower, so the divisor is cached too.
    static int guardedShallow(int a, int b, int k) {
        return a + (b + (a / k));
    }

    // An `iaload` guard under a deep stack: the array, the index and seven operands below them.
    static int bounded(int a, int b, int c, int d, int e, int[] xs, int i) {
        return a + (b + (c + (d + (e + (a + (b + xs[i]))))));
    }

    // A `getfield` guard under a deep stack.
    static int nulled(int a, int b, int c, int d, int e, JgCell cell) {
        return a + (b + (c + (d + (e + (a + (b + cell.v))))));
    }

    static int run() {
        int[] xs = new int[16];
        for (int i = 0; i < 16; i++) {
            xs[i] = i * 3 + 1;
        }
        JgCell cell = new JgCell(11);
        int acc = 0;
        for (int i = 1; i <= 3000; i++) {
            int a = i & 0x3F;
            int b = (i >> 2) & 0x1F;
            int c = (i * 7) & 0xFF;
            int d = (i & 0x0F) + 1;
            int e = (i >> 5) & 0x3F;
            // Every ninth iteration the divisor is zero, which is a guard the compiled code
            // refuses and the rebuilt frame throws on.
            try {
                acc = (acc + guarded(a, b, c, d, e, i % 9)) & 0xFFFFF;
            } catch (ArithmeticException ex) {
                acc = (acc + 3) & 0xFFFFF;
            }
            try {
                acc = (acc + guardedShallow(a, b, i % 7)) & 0xFFFFF;
            } catch (ArithmeticException ex) {
                acc = (acc + 5) & 0xFFFFF;
            }
            // Every eleventh iteration the index is past the end.
            try {
                acc = (acc + bounded(a, b, c, d, e, xs, (i % 11) + 6)) & 0xFFFFF;
            } catch (ArrayIndexOutOfBoundsException ex) {
                acc = (acc + 13) & 0xFFFFF;
            }
            // Every thirteenth iteration the receiver is null.
            try {
                acc = (acc + nulled(a, b, c, d, e, i % 13 == 0 ? null : cell)) & 0xFFFFF;
            } catch (NullPointerException ex) {
                acc = (acc + 17) & 0xFFFFF;
            }
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
