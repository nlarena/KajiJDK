// Differential workload for the F3 JIT — dimension: **a `long` crossing the boundary**, in every
// direction the boundary has.
//
// `JwSem` asks whether the arithmetic is right. This asks whether the *value survives the trip*,
// which is a different question and fails in a different way: a `long` reconstructed from one slot,
// or spilled four bytes wide, or written back with the high half of a category-2 local treated as a
// value, produces a number that is wrong only above bit 31 — so every constant here has bits in
// both halves, and none of them is representable as an `int`.
//
// The four crossings, one per bit group:
//
//   * a **deopt** with `long`s live on the operand stack (a zero divisor mid-expression);
//   * a **safepoint poll**, which leaves a compiled loop with a `long` accumulator in a local;
//   * **fields and statics**, whose slots are eight bytes and whose neighbours must not move;
//   * **on-stack entry**, where the loop is re-entered with the `long` already part-way along.
//
// One bit per observation: score = 4095 means all twelve held.
public class JwState {
    static long total;
    static int guard;

    long field;
    int after;

    // A zero divisor **under** two live `long` operands: the deopt has to hand back all three, and
    // the one at the bottom is not one of the division's own arguments.
    static long deoptMid(long a, long b, long c) {
        return a + b / c;
    }

    // A compiled loop whose accumulator is a `long`. The safepoint poll at the header hands the
    // locals buffer straight back, so this is where a four-byte marshal would show.
    static long loop(long seed, int laps) {
        long acc = seed;
        for (int i = 0; i < laps; i++) {
            acc = acc * 31 + i;
        }
        return acc;
    }

    // The same shape with a `long` *and* an `int` local, so a slot-width error moves one of them.
    static long mixedLoop(long seed, int laps) {
        long acc = seed;
        int k = 0;
        for (int i = 0; i < laps; i++) {
            acc = acc ^ (acc << 13);
            k = k + i;
        }
        return acc + k;
    }

    static long readStatic() {
        return total;
    }

    static void writeStatic(long v) {
        total = v;
        guard = 0x5A5A5A5A;
    }

    long readField() {
        return field;
    }

    void writeField(long v) {
        field = v;
        after = 0x5A5A5A5A;
    }

    static long warm() {
        long t = 0;
        JwState o = new JwState();
        for (int i = 0; i < 64; i++) {
            t += deoptMid(i, i + 1, 3);
            t += loop(i, 4);
            t += mixedLoop(i, 4);
            writeStatic(i);
            t += readStatic();
            o.writeField(i);
            t += o.readField();
        }
        return t;
    }

    static int run() {
        int score = (int) (warm() * 0);
        final long BIG = 81985529216486895L; // 0x0123456789ABCDEF — bits in both halves

        // --- the deopt, with `long`s live on the operand stack ---------------------------------
        if (deoptMid(BIG, 12L, 4L) == BIG + 3) { score += 1; }
        try {
            deoptMid(BIG, 1L, 0L);
        } catch (ArithmeticException e) {
            score += 2;
        }
        // ...and the method still works after the deopt, which is what "resume, not restart" means.
        if (deoptMid(BIG, 12L, 4L) == BIG + 3) { score += 4; }

        // --- a `long` accumulator through a long-running loop -----------------------------------
        // 200000 laps is far past the JIT threshold, so this loop is entered on-stack and polled.
        long a = loop(BIG, 200000);
        long b = 0;
        {
            long acc = BIG;
            for (int i = 0; i < 200000; i++) {
                acc = acc * 31 + i;
            }
            b = acc;
        }
        if (a == b) { score += 8; }
        if (a != (int) a) { score += 16; } // the answer really does need more than 32 bits

        long c = mixedLoop(BIG, 200000);
        long d = 0;
        {
            long acc = BIG;
            int k = 0;
            for (int i = 0; i < 200000; i++) {
                acc = acc ^ (acc << 13);
                k = k + i;
            }
            d = acc + k;
        }
        if (c == d) { score += 32; }

        // --- a `long` static: eight bytes, and the neighbouring slot untouched -------------------
        writeStatic(BIG);
        if (readStatic() == BIG) { score += 64; }
        if (guard == 0x5A5A5A5A) { score += 128; }
        writeStatic(-1L);
        if (readStatic() == -1L && guard == 0x5A5A5A5A) { score += 256; }

        // --- a `long` instance field, likewise ----------------------------------------------------
        JwState o = new JwState();
        o.writeField(BIG);
        if (o.readField() == BIG) { score += 512; }
        if (o.after == 0x5A5A5A5A) { score += 1024; }
        o.writeField(Long.MIN_VALUE);
        if (o.readField() == Long.MIN_VALUE && o.after == 0x5A5A5A5A) { score += 2048; }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
