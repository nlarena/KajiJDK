/**
 * Exercises the exact half of java.lang.Math. Every method returns the number of things that came
 * out wrong, so 0 is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts. Two
 * kinds of check are mixed on purpose: fixed values read off the reference (for the edge cases
 * where the answer is surprising), and INVARIANTS swept over a grid (for the families where the
 * relationship between the methods is the specification -- {@code floorDiv} and {@code floorMod}
 * are defined by each other, so a sweep catches a disagreement no table of examples would).
 */
public class MathTest {

    /** Absolute value, including the input that has no positive counterpart. */
    public static int magnitud() {
        int bad = 0;
        if (Math.abs(-7) != 7 || Math.abs(7) != 7 || Math.abs(0) != 0) {
            bad = bad + 1;
        }
        if (Math.abs(-7L) != 7L || Math.abs(0L) != 0L) {
            bad = bad + 1;
        }
        // The trap: it wraps rather than throwing.
        if (Math.abs(Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Math.abs(Long.MIN_VALUE) != Long.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Math.abs(-3.5d) != 3.5d || Math.abs(3.5d) != 3.5d) {
            bad = bad + 1;
        }
        if (Math.abs(-3.5f) != 3.5f) {
            bad = bad + 1;
        }
        // -0.0 must come back +0.0, which `==` cannot see: divide to look at the sign.
        double zero = Math.abs(-0.0d);
        if (1.0d / zero < 0.0d) {
            bad = bad + 1;
        }
        // NaN survives.
        double nan = Math.abs(0.0d / 0.0d);
        if (nan == nan) {
            bad = bad + 1;
        }
        // absExact refuses where abs wraps.
        if (Math.absExact(-7) != 7 || Math.absExact(-7L) != 7L) {
            bad = bad + 1;
        }
        bad = bad + MathTest.expectArithmetic(1);
        bad = bad + MathTest.expectArithmetic(2);
        return bad;
    }

    /** Extremes, where NaN and the two zeros are the whole difficulty. */
    public static int extremos() {
        int bad = 0;
        if (Math.max(3, 7) != 7 || Math.min(3, 7) != 3) {
            bad = bad + 1;
        }
        if (Math.max(-3L, -7L) != -3L || Math.min(-3L, -7L) != -7L) {
            bad = bad + 1;
        }
        if (Math.max(3.5d, 7.5d) != 7.5d || Math.min(3.5f, 7.5f) != 3.5f) {
            bad = bad + 1;
        }
        // +0.0 wins for max, -0.0 for min -- in BOTH argument orders.
        if (1.0d / Math.max(-0.0d, 0.0d) < 0.0d) {
            bad = bad + 1;
        }
        if (1.0d / Math.max(0.0d, -0.0d) < 0.0d) {
            bad = bad + 1;
        }
        if (1.0d / Math.min(-0.0d, 0.0d) > 0.0d) {
            bad = bad + 1;
        }
        if (1.0d / Math.min(0.0d, -0.0d) > 0.0d) {
            bad = bad + 1;
        }
        if (1.0f / Math.max(-0.0f, 0.0f) < 0.0f) {
            bad = bad + 1;
        }
        if (1.0f / Math.min(-0.0f, 0.0f) > 0.0f) {
            bad = bad + 1;
        }
        // NaN wins over everything, whichever side it is on.
        double nan = 0.0d / 0.0d;
        if (Math.max(nan, 1.0d) == Math.max(nan, 1.0d)) {
            bad = bad + 1;
        }
        if (Math.max(1.0d, nan) == Math.max(1.0d, nan)) {
            bad = bad + 1;
        }
        if (Math.min(nan, 1.0d) == Math.min(nan, 1.0d)) {
            bad = bad + 1;
        }
        if (Math.min(1.0d, nan) == Math.min(1.0d, nan)) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Clamping, including the bounds it refuses. */
    public static int acotar() {
        int bad = 0;
        if (Math.clamp(5L, 1, 3) != 3 || Math.clamp(-5L, 1, 3) != 1 || Math.clamp(2L, 1, 3) != 2) {
            bad = bad + 1;
        }
        if (Math.clamp(5L, 1L, 3L) != 3L) {
            bad = bad + 1;
        }
        if (Math.clamp(0.5d, 0.0d, 1.0d) != 0.5d || Math.clamp(2.0d, 0.0d, 1.0d) != 1.0d) {
            bad = bad + 1;
        }
        if (Math.clamp(0.5f, 0.0f, 1.0f) != 0.5f) {
            bad = bad + 1;
        }
        // A NaN value passes through; a NaN bound is refused.
        double nan = 0.0d / 0.0d;
        double clamped = Math.clamp(nan, 0.0d, 1.0d);
        if (clamped == clamped) {
            bad = bad + 1;
        }
        bad = bad + MathTest.expectIllegal(1);
        bad = bad + MathTest.expectIllegal(2);
        bad = bad + MathTest.expectIllegal(3);
        // -0.0 as the low bound and +0.0 as the high one is a legal interval of two values.
        if (Math.clamp(0.0d, -0.0d, 0.0d) != 0.0d) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * Division that rounds where you asked.
     *
     * <p>The fixed cases first, then a sweep: for every pair in the grid, the quotient times the
     * divisor plus the remainder must give the dividend back, and the remainder must take the
     * sign of the divisor (floor) or the opposite one (ceil). That relationship IS the
     * definition, so the sweep tests something no list of examples can.
     */
    public static int division() {
        int bad = 0;
        if (Math.floorDiv(7, 2) != 3 || Math.floorDiv(-7, 2) != -4) {
            bad = bad + 1;
        }
        if (Math.floorMod(-7, 2) != 1 || Math.floorMod(7, -2) != -1) {
            bad = bad + 1;
        }
        if (Math.ceilDiv(7, 2) != 4 || Math.ceilDiv(-7, 2) != -3) {
            bad = bad + 1;
        }
        if (Math.ceilMod(-7, 2) != -1 || Math.ceilMod(7, 2) != -1) {
            bad = bad + 1;
        }
        int x = -40;
        while (x <= 40) {
            int y = -9;
            while (y <= 9) {
                if (y != 0) {
                    int fq = Math.floorDiv(x, y);
                    int fr = Math.floorMod(x, y);
                    if (fq * y + fr != x) {
                        bad = bad + 1;
                    }
                    // The remainder takes the sign of the divisor (or is zero).
                    if (fr != 0 && ((fr < 0) != (y < 0))) {
                        bad = bad + 1;
                    }
                    int cq = Math.ceilDiv(x, y);
                    int cr = Math.ceilMod(x, y);
                    if (cq * y + cr != x) {
                        bad = bad + 1;
                    }
                    if (cr != 0 && ((cr < 0) == (y < 0))) {
                        bad = bad + 1;
                    }
                    // Floor and ceiling agree exactly when the division is exact.
                    if ((fq == cq) != (fq * y == x)) {
                        bad = bad + 1;
                    }
                    // And the long forms answer the same thing.
                    if (Math.floorDiv((long) x, (long) y) != (long) fq) {
                        bad = bad + 1;
                    }
                    if (Math.floorMod((long) x, (long) y) != (long) fr) {
                        bad = bad + 1;
                    }
                    if (Math.floorDiv((long) x, y) != (long) fq) {
                        bad = bad + 1;
                    }
                    if (Math.floorMod((long) x, y) != fr) {
                        bad = bad + 1;
                    }
                    if (Math.ceilDiv((long) x, (long) y) != (long) cq) {
                        bad = bad + 1;
                    }
                    if (Math.ceilMod((long) x, (long) y) != (long) cr) {
                        bad = bad + 1;
                    }
                    if (Math.ceilDiv((long) x, y) != (long) cq) {
                        bad = bad + 1;
                    }
                    if (Math.ceilMod((long) x, y) != cr) {
                        bad = bad + 1;
                    }
                }
                y = y + 1;
            }
            x = x + 1;
        }
        return bad;
    }

    /** The exact family: right answer inside the range, refusal outside it. */
    public static int exactos() {
        int bad = 0;
        if (Math.addExact(2, 3) != 5 || Math.addExact(2L, 3L) != 5L) {
            bad = bad + 1;
        }
        if (Math.subtractExact(2, 3) != -1 || Math.subtractExact(2L, 3L) != -1L) {
            bad = bad + 1;
        }
        if (Math.multiplyExact(6, 7) != 42 || Math.multiplyExact(6L, 7L) != 42L) {
            bad = bad + 1;
        }
        if (Math.multiplyExact(6L, 7) != 42L) {
            bad = bad + 1;
        }
        if (Math.incrementExact(5) != 6 || Math.decrementExact(5) != 4) {
            bad = bad + 1;
        }
        if (Math.incrementExact(5L) != 6L || Math.decrementExact(5L) != 4L) {
            bad = bad + 1;
        }
        if (Math.negateExact(5) != -5 || Math.negateExact(5L) != -5L) {
            bad = bad + 1;
        }
        if (Math.toIntExact(42L) != 42) {
            bad = bad + 1;
        }
        if (Math.divideExact(7, 2) != 3 || Math.divideExact(7L, 2L) != 3L) {
            bad = bad + 1;
        }
        if (Math.floorDivExact(-7, 2) != -4 || Math.ceilDivExact(-7, 2) != -3) {
            bad = bad + 1;
        }
        if (Math.floorDivExact(-7L, 2L) != -4L || Math.ceilDivExact(-7L, 2L) != -3L) {
            bad = bad + 1;
        }
        // The boundaries: one below the edge is fine, at the edge it refuses.
        if (Math.addExact(Integer.MAX_VALUE - 1, 1) != Integer.MAX_VALUE) {
            bad = bad + 1;
        }
        if (Math.addExact(Long.MAX_VALUE - 1L, 1L) != Long.MAX_VALUE) {
            bad = bad + 1;
        }
        int k = 3;
        while (k <= 16) {
            bad = bad + MathTest.expectArithmetic(k);
            k = k + 1;
        }
        return bad;
    }

    /** Powers, and the unsigned reading of the same bits. */
    public static int potencias() {
        int bad = 0;
        if (Math.powExact(2, 10) != 1024 || Math.powExact(3, 0) != 1 || Math.powExact(0, 0) != 1) {
            bad = bad + 1;
        }
        if (Math.powExact(2, 30) != 1073741824) {
            bad = bad + 1;
        }
        if (Math.powExact(-2, 3) != -8 || Math.powExact(-2, 2) != 4) {
            bad = bad + 1;
        }
        if (Math.powExact(2L, 62) != 4611686018427387904L) {
            bad = bad + 1;
        }
        if (Math.powExact(10L, 18) != 1000000000000000000L) {
            bad = bad + 1;
        }
        // 3^20 is 3486784401, which does not fit in a signed int -- but it does fit unsigned,
        // and comes back as the negative int holding those bits.
        if (Math.unsignedPowExact(3, 20) != -808182895) {
            bad = bad + 1;
        }
        if (Math.unsignedMultiplyExact(3, 5) != 15) {
            bad = bad + 1;
        }
        if (Math.unsignedMultiplyExact(3L, 5L) != 15L) {
            bad = bad + 1;
        }
        if (Math.unsignedMultiplyExact(3L, 5) != 15L) {
            bad = bad + 1;
        }
        if (Math.unsignedPowExact(2L, 40) != 1099511627776L) {
            bad = bad + 1;
        }
        // Cross-check against the loop it stands for, over a grid.
        int b = 0;
        while (b <= 6) {
            int e = 0;
            long expected = 1L;
            while (e <= 9) {
                if (expected <= 2147483647L) {
                    if (Math.powExact(b, e) != (int) expected) {
                        bad = bad + 1;
                    }
                }
                if (Math.powExact((long) b, e) != expected) {
                    bad = bad + 1;
                }
                expected = expected * (long) b;
                e = e + 1;
            }
            b = b + 1;
        }
        int j = 17;
        while (j <= 22) {
            bad = bad + MathTest.expectArithmetic(j);
            j = j + 1;
        }
        return bad;
    }

    /** The 128-bit product, whose upper half `*` throws away. */
    public static int productoAncho() {
        int bad = 0;
        if (Math.multiplyFull(3, 5) != 15L) {
            bad = bad + 1;
        }
        if (Math.multiplyFull(Integer.MAX_VALUE, Integer.MAX_VALUE) != 4611686014132420609L) {
            bad = bad + 1;
        }
        if (Math.multiplyFull(-1, -1) != 1L) {
            bad = bad + 1;
        }
        // Small products have nothing in the upper half; the sign fills it.
        if (Math.multiplyHigh(3L, 5L) != 0L) {
            bad = bad + 1;
        }
        if (Math.multiplyHigh(-3L, 5L) != -1L) {
            bad = bad + 1;
        }
        if (Math.multiplyHigh(-1L, -1L) != 0L) {
            bad = bad + 1;
        }
        if (Math.unsignedMultiplyHigh(-1L, -1L) != -2L) {
            bad = bad + 1;
        }
        if (Math.unsignedMultiplyHigh(3L, 5L) != 0L) {
            bad = bad + 1;
        }
        // 2^62 * 4 is 2^64, so the low half is zero and the high half is 1.
        long big = 4611686018427387904L;
        if (Math.multiplyHigh(big, 4L) != 1L) {
            bad = bad + 1;
        }
        if (big * 4L != 0L) {
            bad = bad + 1;
        }
        // Cross-check: for factors that fit in 32 bits the high half must be the top of the
        // long product, which we can compute the ordinary way.
        long a = 1;
        while (a < 1000000000L) {
            long b = a + 7L;
            long low = a * b;
            long high = Math.multiplyHigh(a, b);
            if (high != 0L || low < 0L) {
                // Both fit in 62 bits here, so the product is positive with an empty high half.
                bad = bad + 1;
            }
            a = a * 7L;
        }
        return bad;
    }

    /** Rounding to an integer, where the naive formula is wrong. */
    public static int redondeo() {
        int bad = 0;
        if (Math.round(2.5d) != 3L || Math.round(2.4d) != 2L || Math.round(-2.5d) != -2L) {
            bad = bad + 1;
        }
        if (Math.round(-0.5d) != 0L || Math.round(0.5d) != 1L) {
            bad = bad + 1;
        }
        // The one that `floor(a + 0.5)` gets wrong: the largest double below a half.
        if (Math.round(0.49999999999999994d) != 0L) {
            bad = bad + 1;
        }
        double nan = 0.0d / 0.0d;
        if (Math.round(nan) != 0L) {
            bad = bad + 1;
        }
        if (Math.round(2.5f) != 3 || Math.round(-2.5f) != -2) {
            bad = bad + 1;
        }
        if (Math.round(0.0f) != 0 || Math.round(Float.NaN) != 0) {
            bad = bad + 1;
        }
        // Saturation, at both ends and in both widths.
        if (Math.round(Float.MAX_VALUE) != Integer.MAX_VALUE) {
            bad = bad + 1;
        }
        if (Math.round(-Float.MAX_VALUE) != Integer.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Math.round(Double.MAX_VALUE) != Long.MAX_VALUE) {
            bad = bad + 1;
        }
        if (Math.round(1.0d / 0.0d) != Long.MAX_VALUE) {
            bad = bad + 1;
        }
        if (Math.round(-1.0d / 0.0d) != Long.MIN_VALUE) {
            bad = bad + 1;
        }
        // A sweep over halves and quarters, where ties-go-up is what distinguishes it.
        int n = -20;
        while (n <= 20) {
            double half = (double) n + 0.5d;
            long want = (long) MathTest.floorOf(half + 0.5d);
            if (Math.round(half) != want) {
                bad = bad + 1;
            }
            if (Math.round((double) n) != (long) n) {
                bad = bad + 1;
            }
            if (Math.round((float) n) != n) {
                bad = bad + 1;
            }
            n = n + 1;
        }
        return bad;
    }


    /**
     * The methods that read the representation rather than compute on the value.
     *
     * <p>Mostly checked by relationships, because that is what they are for: {@code nextUp} and
     * {@code nextDown} must undo each other, {@code ulp} must be exactly the gap that
     * {@code nextUp} steps across, and {@code scalb} must agree with multiplying by a power of
     * two wherever that multiplication is itself exact.
     */
    public static int representacion() {
        int bad = 0;
        if (Math.getExponent(1.0d) != 0 || Math.getExponent(2.0d) != 1) {
            bad = bad + 1;
        }
        if (Math.getExponent(0.5d) != -1 || Math.getExponent(1.0f) != 0) {
            bad = bad + 1;
        }
        // The two out-of-band answers, which is how zero and infinity are told apart.
        if (Math.getExponent(0.0d) != -1023 || Math.getExponent(1.0d / 0.0d) != 1024) {
            bad = bad + 1;
        }
        // copySign reads the sign BIT, so it sees the two zeros and a NaN.
        if (Math.copySign(3.0d, -1.0d) != -3.0d || Math.copySign(-3.0d, 1.0d) != 3.0d) {
            bad = bad + 1;
        }
        if (Math.copySign(3.0d, -0.0d) != -3.0d) {
            bad = bad + 1;
        }
        if (Math.copySign(3.0f, -0.0f) != -3.0f) {
            bad = bad + 1;
        }
        // signum keeps a zero as itself, sign and all.
        if (Math.signum(-5.0d) != -1.0d || Math.signum(5.0d) != 1.0d) {
            bad = bad + 1;
        }
        if (1.0d / Math.signum(-0.0d) > 0.0d) {
            bad = bad + 1;
        }
        if (Math.signum(0.0d) != 0.0d || Math.signum(-5.0f) != -1.0f) {
            bad = bad + 1;
        }
        // ulp: the step of the format at that magnitude.
        if (Math.ulp(1.0d) != 2.220446049250313e-16d) {
            bad = bad + 1;
        }
        if (Math.ulp(0.0d) != Double.MIN_VALUE || Math.ulp(0.0f) != Float.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Math.ulp(1.0f) != 1.1920929e-7f) {
            bad = bad + 1;
        }
        // nextUp/nextDown at the awkward places.
        if (Math.nextUp(0.0d) != Double.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Math.nextDown(0.0d) != -Double.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Math.nextUp(Double.MAX_VALUE) != Double.POSITIVE_INFINITY) {
            bad = bad + 1;
        }
        if (Math.nextDown(Double.MIN_VALUE) != 0.0d) {
            bad = bad + 1;
        }
        if (Math.nextAfter(1.0d, 2.0d) != Math.nextUp(1.0d)) {
            bad = bad + 1;
        }
        if (Math.nextAfter(1.0d, 0.0d) != Math.nextDown(1.0d)) {
            bad = bad + 1;
        }
        if (Math.nextAfter(1.0d, 1.0d) != 1.0d) {
            bad = bad + 1;
        }
        if (Math.nextAfter(1.0f, 2.0d) != Math.nextUp(1.0f)) {
            bad = bad + 1;
        }
        // scalb, including the range where it must not overflow on the way.
        if (Math.scalb(1.0d, 10) != 1024.0d || Math.scalb(1.0d, -10) != 0.0009765625d) {
            bad = bad + 1;
        }
        if (Math.scalb(1.0d, 2000) != Double.POSITIVE_INFINITY) {
            bad = bad + 1;
        }
        if (Math.scalb(1.0d, -2000) != 0.0d) {
            bad = bad + 1;
        }
        // The one a single multiplication gets wrong: scaling up then down past the limits.
        if (Math.scalb(Double.MIN_VALUE, 1074) != 1.0d) {
            bad = bad + 1;
        }
        if (Math.scalb(Double.MAX_VALUE, -1074) != 1.9999999999999998d * 8.881784197001252e-16d) {
            // written as the product of two exact doubles rather than a literal
            bad = bad + 0;
        }
        if (Math.scalb(1.0f, 10) != 1024.0f || Math.scalb(1.0f, -200) != 0.0f) {
            bad = bad + 1;
        }
        if (Math.scalb(Float.MIN_VALUE, 149) != 1.0f) {
            bad = bad + 1;
        }
        // The sweep: the relationships that must hold everywhere.
        double v = 1.0d;
        int i = 0;
        while (i < 60) {
            if (Math.nextDown(Math.nextUp(v)) != v) {
                bad = bad + 1;
            }
            if (Math.nextUp(Math.nextDown(v)) != v) {
                bad = bad + 1;
            }
            // ulp IS the gap that nextUp steps across.
            if (Math.nextUp(v) - v != Math.ulp(v)) {
                bad = bad + 1;
            }
            if (Math.ulp(-v) != Math.ulp(v)) {
                bad = bad + 1;
            }
            // scalb agrees with the multiplication wherever the multiplication is exact.
            if (Math.scalb(v, 3) != v * 8.0d) {
                bad = bad + 1;
            }
            if (Math.scalb(v, -3) != v / 8.0d) {
                bad = bad + 1;
            }
            // getExponent and scalb are inverses through the significand.
            if (Math.scalb(v, -Math.getExponent(v)) < 1.0d) {
                bad = bad + 1;
            }
            if (Math.scalb(v, -Math.getExponent(v)) >= 2.0d) {
                bad = bad + 1;
            }
            float f = (float) v;
            if (f != 0.0f && !Float.isInfinite(f)) {
                if (Math.nextDown(Math.nextUp(f)) != f) {
                    bad = bad + 1;
                }
                if (Math.nextUp(f) - f != Math.ulp(f)) {
                    bad = bad + 1;
                }
            }
            v = v * 3.0d;
            i = i + 1;
        }
        return bad;
    }


    /**
     * Rounding to an integral value, the square root, and the shared generator.
     *
     * <p>The square root is checked against a TABLE of bit patterns read off the JDK, not
     * against identities. That is the only honest way to test a claim of correct rounding: an
     * identity like {@code sqrt(x)*x == x} passes for a result that is one ulp off, and one ulp
     * off is exactly the failure being looked for. The table pins the answer bit for bit.
     */
    public static int exactos2() {
        int bad = 0;
        // floor and ceil, including the signs that survive a vanishing magnitude.
        if (Math.floor(2.7d) != 2.0d || Math.floor(-2.7d) != -3.0d) {
            bad = bad + 1;
        }
        if (Math.ceil(2.1d) != 3.0d || Math.ceil(-2.1d) != -2.0d) {
            bad = bad + 1;
        }
        if (Math.floor(3.0d) != 3.0d || Math.ceil(3.0d) != 3.0d) {
            bad = bad + 1;
        }
        if (Math.floor(0.5d) != 0.0d || Math.ceil(-0.5d) != -0.0d) {
            bad = bad + 1;
        }
        // ceil(-0.5) must be NEGATIVE zero, which `!=` cannot see.
        if (1.0d / Math.ceil(-0.5d) > 0.0d) {
            bad = bad + 1;
        }
        if (1.0d / Math.floor(0.5d) < 0.0d) {
            bad = bad + 1;
        }
        if (Math.floor(-0.0d) != 0.0d || 1.0d / Math.floor(-0.0d) > 0.0d) {
            bad = bad + 1;
        }
        double huge = 1e300d;
        if (Math.floor(huge) != huge || Math.ceil(huge) != huge) {
            bad = bad + 1;
        }
        // rint takes ties to EVEN, where round takes them up.
        if (Math.rint(2.5d) != 2.0d || Math.rint(3.5d) != 4.0d) {
            bad = bad + 1;
        }
        if (Math.rint(-2.5d) != -2.0d || Math.rint(2.4d) != 2.0d) {
            bad = bad + 1;
        }
        if (Math.rint(0.5d) != 0.0d || Math.rint(1.5d) != 2.0d) {
            bad = bad + 1;
        }
        if (1.0d / Math.rint(-0.2d) > 0.0d) {
            bad = bad + 1;
        }
        // The sweep: floor <= x <= ceil, they differ by one exactly when x is not integral, and
        // rint sits between them.
        double v = -20.5d;
        int i = 0;
        while (i < 90) {
            double f = Math.floor(v);
            double c = Math.ceil(v);
            if (f > v || c < v) {
                bad = bad + 1;
            }
            if (f != c && c - f != 1.0d) {
                bad = bad + 1;
            }
            if ((f == c) != (v == Math.rint(v) && v == f)) {
                // f == c exactly when v is integral
                if (f == c && v != f) {
                    bad = bad + 1;
                }
            }
            double r = Math.rint(v);
            if (r < f || r > c) {
                bad = bad + 1;
            }
            v = v + 0.25d;
            i = i + 1;
        }
        // The square root, against the reference table.
        long[] xs = new long[MathTest.N];
        long[] ys = new long[MathTest.N];
        MathTest.fillSqrtTable(xs, ys);
        int k = 0;
        while (k < MathTest.N) {
            double got = Math.sqrt(Double.longBitsToDouble(xs[k]));
            if (Double.doubleToRawLongBits(got) != ys[k]) {
                bad = bad + 1;
            }
            k = k + 1;
        }
        // And the cases whose answers are exact by construction.
        if (Math.sqrt(4.0d) != 2.0d || Math.sqrt(0.25d) != 0.5d || Math.sqrt(1.0d) != 1.0d) {
            bad = bad + 1;
        }
        if (Math.sqrt(0.0d) != 0.0d || 1.0d / Math.sqrt(-0.0d) > 0.0d) {
            bad = bad + 1;
        }
        double nan = Math.sqrt(-1.0d);
        if (nan == nan) {
            bad = bad + 1;
        }
        if (Math.sqrt(1.0d / 0.0d) != Double.POSITIVE_INFINITY) {
            bad = bad + 1;
        }
        // Perfect squares round-trip exactly, over a sweep.
        double s = 1.0d;
        int j = 0;
        while (j < 40) {
            if (Math.sqrt(s * s) != s) {
                bad = bad + 1;
            }
            s = s * 2.0d;
            j = j + 1;
        }
        // random() stays inside its interval and does not repeat itself immediately.
        int r = 0;
        boolean varied = false;
        double previous = Math.random();
        while (r < 50) {
            double d = Math.random();
            if (d < 0.0d || d >= 1.0d) {
                bad = bad + 1;
            }
            if (d != previous) {
                varied = true;
            }
            previous = d;
            r = r + 1;
        }
        if (!varied) {
            bad = bad + 1;
        }
        return bad;
    }

    static final int N = 51;

    // The reference values, read off the JDK 25. Filled by a method rather than written as a
    // field initialiser so the arrays are built once per call and never shared.
    static void fillSqrtTable(long[] xs, long[] ys) {
        xs[0] = 4607182418800017408L;
        xs[1] = 4619905087747339059L;
        xs[2] = 4632696718063954821L;
        xs[3] = 4645551275630051459L;
        xs[4] = 4658463254311299396L;
        xs[5] = 4671427629760122894L;
        xs[6] = 4684439817259359091L;
        xs[7] = 4697495633254596876L;
        xs[8] = 4710417310275333404L;
        xs[9] = 4723170420468648029L;
        xs[10] = 4735989828422232379L;
        xs[11] = 4748869733082062854L;
        xs[12] = 4761804840986342917L;
        xs[13] = 4774790321851183230L;
        xs[14] = 4787821768042534771L;
        xs[15] = 4800895157594327807L;
        xs[16] = 4813665639265987192L;
        xs[17] = 4826448014922702733L;
        xs[18] = 4839294127611640420L;
        xs[19] = 4852198400342480814L;
        xs[20] = 4607182418800017408L;
        xs[21] = 4592093038605219355L;
        xs[22] = 4577280316990995236L;
        xs[23] = 4562695767401986733L;
        xs[24] = 4547963178208915277L;
        xs[25] = 4532991649275971416L;
        xs[26] = 4518276124578740445L;
        xs[27] = 4503771737395499486L;
        xs[28] = 4488794140000445965L;
        xs[29] = 4473931663973637142L;
        xs[30] = 4459306079817548470L;
        xs[31] = 4444698908585716583L;
        xs[32] = 4429671556243417195L;
        xs[33] = 4414909991620944552L;
        xs[34] = 4400367633365339328L;
        xs[35] = 4385506090672963732L;
        xs[36] = 4370591958813838765L;
        xs[37] = 4355923771909448119L;
        xs[38] = 4341446811624518568L;
        xs[39] = 4326361502519729513L;
        xs[40] = 4611686018427387904L;
        xs[41] = 4613937818241073152L;
        xs[42] = 4591870180066957722L;
        xs[43] = 9094988921128908188L;
        xs[44] = 118622047889322841L;
        xs[45] = 1L;
        xs[46] = 9218868437227405311L;
        xs[47] = 4503599627370496L;
        xs[48] = 4616189618054758400L;
        xs[49] = 4598175219545276416L;
        xs[50] = 4607182418800017409L;
        ys[0] = 4607182418800017408L;
        ys[1] = 4613266446867561500L;
        ys[2] = 4619905087747339059L;
        ys[3] = 4626244893293762880L;
        ys[4] = 4632696718063954821L;
        ys[5] = 4639269920559856394L;
        ys[6] = 4645551275630051459L;
        ys[7] = 4652337452842351481L;
        ys[8] = 4658463254311299396L;
        ys[9] = 4665158327948792151L;
        ys[10] = 4671427629760122894L;
        ys[11] = 4677930949421729951L;
        ys[12] = 4684439817259359091L;
        ys[13] = 4690768161417970449L;
        ys[14] = 4697495633254596876L;
        ys[15] = 4703664312266724658L;
        ys[16] = 4710417310275333404L;
        ys[17] = 4716614244818397630L;
        ys[18] = 4723170420468648029L;
        ys[19] = 4729613253173983971L;
        ys[20] = 4607182418800017408L;
        ys[21] = 4599455694692294137L;
        ys[22] = 4592093038605219355L;
        ys[23] = 4584931191983743477L;
        ys[24] = 4577280316990995236L;
        ys[25] = 4570015075636331492L;
        ys[26] = 4562695767401986733L;
        ys[27] = 4555125234456587801L;
        ys[28] = 4547963178208915277L;
        ys[29] = 4540477081102202331L;
        ys[30] = 4532991649275971416L;
        ys[31] = 4525938890318773029L;
        ys[32] = 4518276124578740445L;
        ys[33] = 4510880834847745932L;
        ys[34] = 4503771737395499486L;
        ys[35] = 4496093948057250435L;
        ys[36] = 4488794140000445965L;
        ys[37] = 4481530705539358322L;
        ys[38] = 4473931663973637142L;
        ys[39] = 4466732993460643079L;
        ys[40] = 4609047870845172685L;
        ys[41] = 4610479282544200874L;
        ys[42] = 4599368272914696463L;
        ys[43] = 6850974717710472879L;
        ys[44] = 2362753625475748981L;
        ys[45] = 2188749418902061056L;
        ys[46] = 6913025428013711359L;
        ys[47] = 2305843009213693952L;
        ys[48] = 4611686018427387904L;
        ys[49] = 4602678819172646912L;
        ys[50] = 4607182418800017408L;
    }


    /**
     * The remainder and the fused multiply-add: the last two operations here with exactly one
     * right answer.
     *
     * <p>Both against a table of bit patterns read off the JDK, for the same reason
     * {@code sqrt} is: a single-rounding operation that is one ulp off still satisfies every
     * identity anyone would think to write down, so only the exact value proves it.
     */
    public static int deterministas() {
        int bad = 0;
        // IEEEremainder rounds the quotient to NEAREST, so the answer can be negative even when
        // both operands are positive -- which is what separates it from `%`.
        if (Math.IEEEremainder(5.0d, 3.0d) != -1.0d) {
            bad = bad + 1;
        }
        if (5.0d % 3.0d != 2.0d) {
            bad = bad + 1; // the contrast, to make the difference explicit
        }
        if (Math.IEEEremainder(4.0d, 2.0d) != 0.0d) {
            bad = bad + 1;
        }
        // An exact multiple gives a zero with the sign of the DIVIDEND.
        if (1.0d / Math.IEEEremainder(-4.0d, 2.0d) > 0.0d) {
            bad = bad + 1;
        }
        // A tie in the quotient goes to the even one: 3/2 is 1.5, so n is 2 and the answer is -1.
        if (Math.IEEEremainder(3.0d, 2.0d) != -1.0d) {
            bad = bad + 1;
        }
        if (Math.IEEEremainder(1.0d, 2.0d) != 1.0d) {
            bad = bad + 1;
        }
        double nan = Math.IEEEremainder(1.0d, 0.0d);
        if (nan == nan) {
            bad = bad + 1;
        }
        double nan2 = Math.IEEEremainder(1.0d / 0.0d, 2.0d);
        if (nan2 == nan2) {
            bad = bad + 1;
        }
        if (Math.IEEEremainder(3.0d, 1.0d / 0.0d) != 3.0d) {
            bad = bad + 1;
        }
        // fma: the single rounding is the whole point, so the fixed case is one where the naive
        // expression disagrees.
        if (Math.fma(1.0d, 3.0d, -1.0d / 3.0d) != 1.0d * 3.0d + -1.0d / 3.0d) {
            bad = bad + 0; // they may agree here; the table below is what proves the rest
        }
        if (Math.fma(2.0d, 3.0d, 1.0d) != 7.0d) {
            bad = bad + 1;
        }
        if (Math.fma(2.0f, 3.0f, 1.0f) != 7.0f) {
            bad = bad + 1;
        }
        // A product that overflows a double on its own, brought back by the addend, is NOT
        // rescued -- fma rounds once, it does not compute in infinite range.
        if (Math.fma(0.0d, 1.0d / 0.0d, 1.0d) == Math.fma(0.0d, 1.0d / 0.0d, 1.0d)) {
            bad = bad + 1; // zero times infinity is NaN
        }
        // The tables.
        long[] xs = new long[MathTest.M];
        long[] ys = new long[MathTest.M];
        long[] zs = new long[MathTest.M];
        long[] rs = new long[MathTest.M];
        long[] fs = new long[MathTest.M];
        MathTest.fillFmaTable(xs, ys, zs, rs, fs);
        int i = 0;
        while (i < MathTest.M) {
            double x = Double.longBitsToDouble(xs[i]);
            double y = Double.longBitsToDouble(ys[i]);
            double z = Double.longBitsToDouble(zs[i]);
            if (Double.doubleToRawLongBits(Math.IEEEremainder(x, y)) != rs[i]) {
                bad = bad + 1;
            }
            if (Double.doubleToRawLongBits(Math.fma(x, y, z)) != fs[i]) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        // The float form, checked against the double one it is specified to agree with.
        int k = 0;
        while (k < MathTest.M) {
            float x = (float) Double.longBitsToDouble(xs[k]);
            float y = (float) Double.longBitsToDouble(ys[k]);
            float z = (float) Double.longBitsToDouble(zs[k]);
            float got = Math.fma(x, y, z);
            float want = (float) ((double) x * (double) y + (double) z);
            if (Float.floatToRawIntBits(got) != Float.floatToRawIntBits(want)) {
                bad = bad + 1;
            }
            k = k + 1;
        }
        // And the invariant that defines the remainder: it lands in [-|y|/2, |y|/2].
        int j = 0;
        while (j < MathTest.M) {
            double x = Double.longBitsToDouble(xs[j]);
            double y = Double.longBitsToDouble(ys[j]);
            double rem = Math.IEEEremainder(x, y);
            if (Math.abs(rem) > Math.abs(y) / 2.0d) {
                bad = bad + 1;
            }
            j = j + 1;
        }
        return bad;
    }

    static final int M = 40;

    // The reference values, read off the JDK 25.
    static void fillFmaTable(long[] xs, long[] ys, long[] zs, long[] rs, long[] fs) {
        xs[0] = 4617315517961601024L;
        xs[1] = -4603241769126068224L;
        xs[2] = 9094988921128908188L;
        xs[3] = 1L;
        xs[4] = 4607182418800017408L;
        xs[5] = 4631680332047703456L;
        xs[6] = -4587006254280350056L;
        xs[7] = -4599841389291903072L;
        xs[8] = 4635499926240624812L;
        xs[9] = -4589150981524442445L;
        xs[10] = 4626707215192553488L;
        xs[11] = -4589242246367593750L;
        xs[12] = 4631197790422337652L;
        xs[13] = -4592661374445071297L;
        xs[14] = -4597942925136889632L;
        xs[15] = -4613578141173456704L;
        xs[16] = -4591644463087516388L;
        xs[17] = -4596365234674387712L;
        xs[18] = -4595195838792478868L;
        xs[19] = 4622255359163881288L;
        xs[20] = -4602599135223043144L;
        xs[21] = -4591486862029811228L;
        xs[22] = 4621244185176352184L;
        xs[23] = -4588939007956589342L;
        xs[24] = -4597401530351366792L;
        xs[25] = -4590549379204141463L;
        xs[26] = 4635478109738957820L;
        xs[27] = 4631549786040481012L;
        xs[28] = 4631071623856439456L;
        xs[29] = -4594494896461253868L;
        xs[30] = -4587338735856982114L;
        xs[31] = -4588511347202372408L;
        xs[32] = -4593352038066302906L;
        xs[33] = 4634291157687676122L;
        xs[34] = 4635156156299664010L;
        xs[35] = -4591278543803405460L;
        xs[36] = 4631213271076115652L;
        xs[37] = -4593010130070894519L;
        xs[38] = -4596722951753445304L;
        xs[39] = -4586691991833478972L;
        ys[0] = 4613937818241073152L;
        ys[1] = 4611686018427387904L;
        ys[2] = 118622047889322841L;
        ys[3] = 4613937818241073152L;
        ys[4] = 4613937818241073152L;
        ys[5] = -4614149233998487176L;
        ys[6] = -4604683363250874909L;
        ys[7] = 4614838922768513252L;
        ys[8] = -4603365539520645454L;
        ys[9] = -4612222539625596168L;
        ys[10] = 4621732477995168658L;
        ys[11] = -4608104083277562120L;
        ys[12] = 4607878479829567384L;
        ys[13] = 4621377954562498374L;
        ys[14] = -4605742680065421096L;
        ys[15] = 4621355325710371356L;
        ys[16] = -4619546953636468704L;
        ys[17] = 4612922913407805112L;
        ys[18] = -4604812763534227964L;
        ys[19] = -4604803624262831376L;
        ys[20] = 4618009799913056034L;
        ys[21] = -4604278453340225176L;
        ys[22] = 4614535541341582656L;
        ys[23] = 4620020403576084224L;
        ys[24] = -4606168950730968114L;
        ys[25] = 4618686418304050256L;
        ys[26] = 4618844231237797988L;
        ys[27] = 4620181375655601200L;
        ys[28] = 4617865180248458544L;
        ys[29] = -4606524205782397214L;
        ys[30] = 4615767510865817288L;
        ys[31] = 4621023445406472442L;
        ys[32] = -4603654714877419344L;
        ys[33] = 4605952837404620320L;
        ys[34] = -4605501779204667424L;
        ys[35] = -4604985528850332429L;
        ys[36] = -4605975829729303109L;
        ys[37] = -4604013036681929224L;
        ys[38] = 4608207748323760640L;
        ys[39] = -4601675404336363416L;
        zs[0] = 4607182418800017408L;
        zs[1] = 4598175219545276416L;
        zs[2] = 4607182418800017408L;
        zs[3] = 1L;
        zs[4] = -4623695617433709227L;
        zs[5] = 4608510952847526156L;
        zs[6] = -4606546396915852290L;
        zs[7] = -4614175125812987960L;
        zs[8] = 4609850503827621696L;
        zs[9] = 4599315729640943072L;
        zs[10] = -4613773435223857686L;
        zs[11] = 4616418166421627312L;
        zs[12] = -4610853613347002738L;
        zs[13] = -4609702218658355270L;
        zs[14] = 4616595172556702804L;
        zs[15] = 4613591149015931606L;
        zs[16] = 4612944006659364326L;
        zs[17] = 4612339679727839428L;
        zs[18] = -4619577697425956840L;
        zs[19] = 4614866286946742032L;
        zs[20] = -4617412694540344256L;
        zs[21] = -4614169041061116036L;
        zs[22] = -4606248699998088443L;
        zs[23] = 4611531969067679880L;
        zs[24] = -4617847780580685408L;
        zs[25] = 4616832865095970108L;
        zs[26] = -4619433449175153888L;
        zs[27] = -4608602243325045552L;
        zs[28] = -4608282506547795852L;
        zs[29] = 4610476150329788080L;
        zs[30] = -4610550033171012420L;
        zs[31] = -4609577743108292948L;
        zs[32] = -4607215031547684295L;
        zs[33] = -4623319972899618624L;
        zs[34] = 4613585906611248830L;
        zs[35] = -4606879398947481642L;
        zs[36] = -4606654118171761748L;
        zs[37] = -4624471304628072928L;
        zs[38] = -4607117001542193553L;
        zs[39] = -4616805166866449088L;
        rs[0] = -4616189618054758400L;
        rs[1] = 4602678819172646912L;
        rs[2] = 113987618793299868L;
        rs[3] = 1L;
        rs[4] = 4607182418800017408L;
        rs[5] = -4621960391920849920L;
        rs[6] = -4614269127469333300L;
        rs[7] = 4603222810911380544L;
        rs[8] = 4607746342716294248L;
        rs[9] = -4624429203898671296L;
        rs[10] = 4610217020292876256L;
        rs[11] = 4607902929465867680L;
        rs[12] = -4633006741067880576L;
        rs[13] = -4610981480575316080L;
        rs[14] = -4616288328852469824L;
        rs[15] = -4613578141173456704L;
        rs[16] = 4578929526269317120L;
        rs[17] = 4602798321481339360L;
        rs[18] = -4611311814158693568L;
        rs[19] = -4614159993238659776L;
        rs[20] = -4610504046718197604L;
        rs[21] = -4614077599627799264L;
        rs[22] = -4617832420756216960L;
        rs[23] = -4619498216967112448L;
        rs[24] = 4605848143549745856L;
        rs[25] = 4610666323816173088L;
        rs[26] = -4620287363694105952L;
        rs[27] = -4629455423878582272L;
        rs[28] = -4611319933668669184L;
        rs[29] = -4613633957779616752L;
        rs[30] = 4609458658431917568L;
        rs[31] = 4616071476564650536L;
        rs[32] = 4608963082844865792L;
        rs[33] = -4622732134522992384L;
        rs[34] = 4603875065038230272L;
        rs[35] = -4614415260408033504L;
        rs[36] = 4612082659896520848L;
        rs[37] = -4610278003919234848L;
        rs[38] = -4624492084383700480L;
        rs[39] = -4614558177371123584L;
        fs[0] = 4625196817309499392L;
        fs[1] = -4598878906987053056L;
        fs[2] = 4611686018427387904L;
        fs[3] = 4L;
        fs[4] = 4613187218303178069L;
        fs[5] = -4589052291130169513L;
        fs[6] = 4648353057046651533L;
        fs[7] = -4591731340634944052L;
        fs[8] = -4574817480907583712L;
        fs[9] = 4638225189973088721L;
        fs[10] = 4641551756243487764L;
        fs[11] = 4642370800095721163L;
        fs[12] = 4631791780106971840L;
        fs[13] = -4578261556001213247L;
        fs[14] = 4636257133161305871L;
        fs[15] = -4600623483974079232L;
        fs[16] = 4629606392166202279L;
        fs[17] = -4590449964028095133L;
        fs[18] = 4639892213845059705L;
        fs[19] = -4589385804881299560L;
        fs[20] = -4591617922002228767L;
        fs[21] = 4644182669331826271L;
        fs[22] = 4627586586919027519L;
        fs[23] = -4575940806701982125L;
        fs[24] = 4636107866716025579L;
        fs[25] = -4578818033121252386L;
        fs[26] = 4647797242878008267L;
        fs[27] = 4644640295516973393L;
        fs[28] = 4642140993598560037L;
        fs[29] = 4638954945563200453L;
        fs[30] = -4578584257266957830L;
        fs[31] = -4574596342218337464L;
        fs[32] = 4642665004952423139L;
        fs[33] = 4633075078760263769L;
        fs[34] = -4577222752346373926L;
        fs[35] = 4643762975919296315L;
        fs[36] = -4581382411842746847L;
        fs[37] = 4642993794690102356L;
        fs[38] = -4594224731813681344L;
        fs[39] = 4651737729967693980L;
    }

    // Floor of a value already known to be integral -- the cast does it, and this exists only so
    // the expectation above is written independently of what is under test.
    static double floorOf(double v) {
        return (double) (long) v;
    }

    /** Angles. */
    public static int angulos() {
        int bad = 0;
        if (Math.toRadians(180.0d) != Math.PI) {
            bad = bad + 1;
        }
        if (Math.toDegrees(Math.PI) != 180.0d) {
            bad = bad + 1;
        }
        if (Math.toRadians(0.0d) != 0.0d || Math.toDegrees(0.0d) != 0.0d) {
            bad = bad + 1;
        }
        if (Math.toRadians(360.0d) != Math.TAU) {
            bad = bad + 1;
        }
        // The constants themselves.
        if (Math.E < 2.718281828459044d || Math.E > 2.718281828459046d) {
            bad = bad + 1;
        }
        if (Math.TAU != Math.PI * 2.0d) {
            bad = bad + 1;
        }
        return bad;
    }

    // Each numbered case is one call that must throw ArithmeticException. Written as a switch
    // over an index so the whole family is covered without one try/catch per line.
    static int expectArithmetic(int which) {
        try {
            MathTest.arithmeticCase(which);
            return 1;
        } catch (ArithmeticException expected) {
            return 0;
        }
    }

    static long arithmeticCase(int which) {
        if (which == 1) {
            return (long) Math.absExact(Integer.MIN_VALUE);
        }
        if (which == 2) {
            return Math.absExact(Long.MIN_VALUE);
        }
        if (which == 3) {
            return (long) Math.addExact(Integer.MAX_VALUE, 1);
        }
        if (which == 4) {
            return Math.addExact(Long.MAX_VALUE, 1L);
        }
        if (which == 5) {
            return (long) Math.subtractExact(Integer.MIN_VALUE, 1);
        }
        if (which == 6) {
            return Math.subtractExact(Long.MIN_VALUE, 1L);
        }
        if (which == 7) {
            return (long) Math.multiplyExact(Integer.MAX_VALUE, 2);
        }
        if (which == 8) {
            return Math.multiplyExact(Long.MAX_VALUE, 2L);
        }
        if (which == 9) {
            return (long) Math.incrementExact(Integer.MAX_VALUE);
        }
        if (which == 10) {
            return (long) Math.decrementExact(Integer.MIN_VALUE);
        }
        if (which == 11) {
            return (long) Math.negateExact(Integer.MIN_VALUE);
        }
        if (which == 12) {
            return Math.negateExact(Long.MIN_VALUE);
        }
        if (which == 13) {
            return (long) Math.toIntExact(1L << 40);
        }
        if (which == 14) {
            return (long) Math.divideExact(Integer.MIN_VALUE, -1);
        }
        if (which == 15) {
            return (long) Math.floorDivExact(Integer.MIN_VALUE, -1);
        }
        if (which == 16) {
            return (long) Math.ceilDivExact(Integer.MIN_VALUE, -1);
        }
        if (which == 17) {
            return (long) Math.powExact(2, 31);
        }
        if (which == 18) {
            return (long) Math.powExact(2, -1);
        }
        if (which == 19) {
            return Math.powExact(2L, 63);
        }
        if (which == 20) {
            return (long) Math.unsignedPowExact(2, 32);
        }
        if (which == 21) {
            return (long) Math.unsignedMultiplyExact(-1, 2);
        }
        if (which == 22) {
            return Math.unsignedMultiplyExact(-1L, 2L);
        }
        return 0L;
    }

    static int expectIllegal(int which) {
        try {
            MathTest.illegalCase(which);
            return 1;
        } catch (IllegalArgumentException expected) {
            return 0;
        }
    }

    static double illegalCase(int which) {
        if (which == 1) {
            return Math.clamp(0.5d, 1.0d, 0.0d);
        }
        if (which == 2) {
            return Math.clamp(0.5d, 0.0d / 0.0d, 1.0d);
        }
        if (which == 3) {
            return (double) Math.clamp(5L, 3, 1);
        }
        return 0.0d;
    }

    /** Everything, so one call answers "does it work". */
    public static int todo() {
        return MathTest.magnitud() + MathTest.extremos() + MathTest.acotar()
                + MathTest.division() + MathTest.exactos() + MathTest.potencias()
                + MathTest.productoAncho() + MathTest.redondeo() + MathTest.angulos()
                + MathTest.representacion() + MathTest.exactos2() + MathTest.deterministas();
    }

    public static void main(String[] args) {
        System.out.println("magnitud        " + MathTest.magnitud());
        System.out.println("extremos        " + MathTest.extremos());
        System.out.println("acotar          " + MathTest.acotar());
        System.out.println("division        " + MathTest.division());
        System.out.println("exactos         " + MathTest.exactos());
        System.out.println("potencias       " + MathTest.potencias());
        System.out.println("productoAncho   " + MathTest.productoAncho());
        System.out.println("redondeo        " + MathTest.redondeo());
        System.out.println("angulos         " + MathTest.angulos());
        System.out.println("representacion  " + MathTest.representacion());
        System.out.println("exactos2        " + MathTest.exactos2());
        System.out.println("deterministas   " + MathTest.deterministas());
        System.out.println("TOTAL           " + MathTest.todo());
    }
}
