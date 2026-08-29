/**
 * Exercises java.lang.StrictMath. Every method returns the number of things that came out wrong,
 * so 0 is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 *
 * <p>Two different questions are being asked, and they need two different kinds of check. The
 * sweeps ask <em>did every method forward to the right place</em> -- StrictMath is a wrapper over
 * Math, and the way a wrapper fails is by landing on the wrong overload, which a compiler will
 * accept in silence because the call still typechecks. Comparing the two side by side over a grid
 * catches that, and catches nothing else, since a shared implementation cannot disagree with
 * itself. So the second check is a TABLE of bit patterns read off the JDK's StrictMath: that one
 * is the check that the values are right, and it would still fail if both classes were wrong
 * together.
 */
public class StrictTest {

    static final int N = 24;

    /**
     * The three constants, by bit pattern.
     *
     * <p>By bit pattern and not by {@code ==} on a literal, because a compiler that folded the
     * constant wrongly would fold both sides the same way and the comparison would pass. The
     * numbers below were read off the reference.
     */
    public static int constantes() {
        int bad = 0;
        if (Double.doubleToRawLongBits(StrictMath.E) != 4613303445314885481L) {
            bad = bad + 1;
        }
        if (Double.doubleToRawLongBits(StrictMath.PI) != 4614256656552045848L) {
            bad = bad + 1;
        }
        if (Double.doubleToRawLongBits(StrictMath.TAU) != 4618760256179416344L) {
            bad = bad + 1;
        }
        // And they must be the same objects of arithmetic as Math's, not merely close.
        if (StrictMath.E != Math.E || StrictMath.PI != Math.PI || StrictMath.TAU != Math.TAU) {
            bad = bad + 1;
        }
        return bad;
    }

    /** True when the two doubles are the SAME value, NaN and signed zero included. */
    static boolean same(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    /** True when the two floats are the SAME value, NaN and signed zero included. */
    static boolean same(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    /**
     * The integer families, swept against Math.
     *
     * <p>The pairs are chosen so both operands cross zero and both extremes appear: the overloads
     * that differ only in the width of the second argument -- {@code floorDiv(long, int)} against
     * {@code floorDiv(long, long)} -- are the ones a wrong forward would silently pick, and they
     * disagree in nothing except the sign of the result at the extremes.
     */
    public static int enteros() {
        int bad = 0;
        int[] ints = new int[9];
        ints[0] = 0;
        ints[1] = 1;
        ints[2] = -1;
        ints[3] = 7;
        ints[4] = -7;
        ints[5] = 1000;
        ints[6] = -1000;
        ints[7] = Integer.MIN_VALUE;
        ints[8] = Integer.MAX_VALUE;
        long[] longs = new long[9];
        longs[0] = 0L;
        longs[1] = 1L;
        longs[2] = -1L;
        longs[3] = 7L;
        longs[4] = -7L;
        longs[5] = 1000000000000L;
        longs[6] = -1000000000000L;
        longs[7] = Long.MIN_VALUE;
        longs[8] = Long.MAX_VALUE;

        int i = 0;
        while (i < 9) {
            int a = ints[i];
            long la = longs[i];
            if (StrictMath.abs(a) != Math.abs(a)) {
                bad = bad + 1;
            }
            if (StrictMath.abs(la) != Math.abs(la)) {
                bad = bad + 1;
            }
            if (StrictMath.negateExact(0) != 0) {
                bad = bad + 1;
            }
            int j = 0;
            while (j < 9) {
                int b = ints[j];
                long lb = longs[j];
                if (StrictMath.max(a, b) != Math.max(a, b)) {
                    bad = bad + 1;
                }
                if (StrictMath.min(a, b) != Math.min(a, b)) {
                    bad = bad + 1;
                }
                if (StrictMath.max(la, lb) != Math.max(la, lb)) {
                    bad = bad + 1;
                }
                if (StrictMath.min(la, lb) != Math.min(la, lb)) {
                    bad = bad + 1;
                }
                if (StrictMath.multiplyFull(a, b) != Math.multiplyFull(a, b)) {
                    bad = bad + 1;
                }
                if (StrictMath.multiplyHigh(la, lb) != Math.multiplyHigh(la, lb)) {
                    bad = bad + 1;
                }
                if (StrictMath.unsignedMultiplyHigh(la, lb) != Math.unsignedMultiplyHigh(la, lb)) {
                    bad = bad + 1;
                }
                if (b != 0) {
                    if (StrictMath.floorDiv(a, b) != Math.floorDiv(a, b)) {
                        bad = bad + 1;
                    }
                    if (StrictMath.floorMod(a, b) != Math.floorMod(a, b)) {
                        bad = bad + 1;
                    }
                    if (StrictMath.ceilDiv(a, b) != Math.ceilDiv(a, b)) {
                        bad = bad + 1;
                    }
                    if (StrictMath.ceilMod(a, b) != Math.ceilMod(a, b)) {
                        bad = bad + 1;
                    }
                    // The (long, int) overloads. If either side landed on (long, long) instead,
                    // the value is still a number and still plausible -- which is exactly why
                    // this is worth a line of its own.
                    if (StrictMath.floorDiv(la, b) != Math.floorDiv(la, b)) {
                        bad = bad + 1;
                    }
                    if (StrictMath.floorMod(la, b) != Math.floorMod(la, b)) {
                        bad = bad + 1;
                    }
                    if (StrictMath.ceilDiv(la, b) != Math.ceilDiv(la, b)) {
                        bad = bad + 1;
                    }
                    if (StrictMath.ceilMod(la, b) != Math.ceilMod(la, b)) {
                        bad = bad + 1;
                    }
                }
                if (lb != 0L) {
                    if (StrictMath.floorDiv(la, lb) != Math.floorDiv(la, lb)) {
                        bad = bad + 1;
                    }
                    if (StrictMath.floorMod(la, lb) != Math.floorMod(la, lb)) {
                        bad = bad + 1;
                    }
                    if (StrictMath.ceilDiv(la, lb) != Math.ceilDiv(la, lb)) {
                        bad = bad + 1;
                    }
                    if (StrictMath.ceilMod(la, lb) != Math.ceilMod(la, lb)) {
                        bad = bad + 1;
                    }
                }
                j = j + 1;
            }
            i = i + 1;
        }

        // The families that refuse rather than wrap, on values where they do not have to.
        int k = -40;
        while (k < 40) {
            long lk = k;
            if (StrictMath.addExact(k, 3) != Math.addExact(k, 3)) {
                bad = bad + 1;
            }
            if (StrictMath.addExact(lk, 3L) != Math.addExact(lk, 3L)) {
                bad = bad + 1;
            }
            if (StrictMath.subtractExact(k, 3) != Math.subtractExact(k, 3)) {
                bad = bad + 1;
            }
            if (StrictMath.subtractExact(lk, 3L) != Math.subtractExact(lk, 3L)) {
                bad = bad + 1;
            }
            if (StrictMath.multiplyExact(k, 3) != Math.multiplyExact(k, 3)) {
                bad = bad + 1;
            }
            if (StrictMath.multiplyExact(lk, 3L) != Math.multiplyExact(lk, 3L)) {
                bad = bad + 1;
            }
            if (StrictMath.incrementExact(k) != Math.incrementExact(k)) {
                bad = bad + 1;
            }
            if (StrictMath.incrementExact(lk) != Math.incrementExact(lk)) {
                bad = bad + 1;
            }
            if (StrictMath.decrementExact(k) != Math.decrementExact(k)) {
                bad = bad + 1;
            }
            if (StrictMath.decrementExact(lk) != Math.decrementExact(lk)) {
                bad = bad + 1;
            }
            if (StrictMath.negateExact(k) != Math.negateExact(k)) {
                bad = bad + 1;
            }
            if (StrictMath.negateExact(lk) != Math.negateExact(lk)) {
                bad = bad + 1;
            }
            if (StrictMath.absExact(k) != Math.absExact(k)) {
                bad = bad + 1;
            }
            if (StrictMath.absExact(lk) != Math.absExact(lk)) {
                bad = bad + 1;
            }
            if (StrictMath.toIntExact(lk) != Math.toIntExact(lk)) {
                bad = bad + 1;
            }
            if (StrictMath.clamp(lk, -10, 10) != Math.clamp(lk, -10, 10)) {
                bad = bad + 1;
            }
            if (StrictMath.clamp(lk, -10L, 10L) != Math.clamp(lk, -10L, 10L)) {
                bad = bad + 1;
            }
            // Read as unsigned, so the operand is kept in a range where three times it fits.
            if (StrictMath.unsignedMultiplyExact(k + 100, 3) != Math.unsignedMultiplyExact(k + 100, 3)) {
                bad = bad + 1;
            }
            // The (long, int) product, on a value that does not overflow.
            if (StrictMath.multiplyExact(lk, 3) != Math.multiplyExact(lk, 3)) {
                bad = bad + 1;
            }
            if (k != 0) {
                if (StrictMath.divideExact(120, k) != Math.divideExact(120, k)) {
                    bad = bad + 1;
                }
                if (StrictMath.divideExact(120L, lk) != Math.divideExact(120L, lk)) {
                    bad = bad + 1;
                }
                if (StrictMath.floorDivExact(120, k) != Math.floorDivExact(120, k)) {
                    bad = bad + 1;
                }
                if (StrictMath.floorDivExact(120L, lk) != Math.floorDivExact(120L, lk)) {
                    bad = bad + 1;
                }
                if (StrictMath.ceilDivExact(120, k) != Math.ceilDivExact(120, k)) {
                    bad = bad + 1;
                }
                if (StrictMath.ceilDivExact(120L, lk) != Math.ceilDivExact(120L, lk)) {
                    bad = bad + 1;
                }
            }
            k = k + 1;
        }

        int e = 0;
        while (e < 8) {
            if (StrictMath.powExact(3, e) != Math.powExact(3, e)) {
                bad = bad + 1;
            }
            if (StrictMath.powExact(3L, e) != Math.powExact(3L, e)) {
                bad = bad + 1;
            }
            if (StrictMath.unsignedPowExact(3, e) != Math.unsignedPowExact(3, e)) {
                bad = bad + 1;
            }
            if (StrictMath.unsignedPowExact(3L, e) != Math.unsignedPowExact(3L, e)) {
                bad = bad + 1;
            }
            if (StrictMath.unsignedMultiplyExact(1000L, e) != Math.unsignedMultiplyExact(1000L, e)) {
                bad = bad + 1;
            }
            if (StrictMath.unsignedMultiplyExact(1000L, 7L) != Math.unsignedMultiplyExact(1000L, 7L)) {
                bad = bad + 1;
            }
            e = e + 1;
        }

        // And that the refusing ones do refuse.
        bad = bad + StrictTest.expectArithmetic(1);
        bad = bad + StrictTest.expectArithmetic(2);
        bad = bad + StrictTest.expectArithmetic(3);
        bad = bad + StrictTest.expectArithmetic(4);
        bad = bad + StrictTest.expectArithmetic(5);
        return bad;
    }

    static int expectArithmetic(int which) {
        try {
            StrictTest.arithmeticCase(which);
        } catch (ArithmeticException ex) {
            return 0;
        }
        return 1;
    }

    static long arithmeticCase(int which) {
        if (which == 1) {
            return StrictMath.absExact(Integer.MIN_VALUE);
        }
        if (which == 2) {
            return StrictMath.addExact(Integer.MAX_VALUE, 1);
        }
        if (which == 3) {
            return StrictMath.toIntExact(Long.MAX_VALUE);
        }
        if (which == 4) {
            return StrictMath.negateExact(Long.MIN_VALUE);
        }
        return StrictMath.multiplyExact(Long.MAX_VALUE, 2L);
    }

    /**
     * The floating-point families, swept against Math, by BIT pattern.
     *
     * <p>By bit pattern because {@code ==} cannot see the two differences that matter here: it
     * says a NaN differs from itself, and it says the two zeros are equal. A forward that lost
     * the sign of a zero would pass an {@code ==} sweep completely.
     */
    public static int flotantes() {
        int bad = 0;
        double[] ds = new double[14];
        ds[0] = 0.0d;
        ds[1] = -0.0d;
        ds[2] = 1.0d;
        ds[3] = -1.0d;
        ds[4] = 0.5d;
        ds[5] = -7.5d;
        ds[6] = 123456.789d;
        ds[7] = 1.0e15d;
        ds[8] = 4.9e-324d;
        ds[9] = 1.7976931348623157e308d;
        ds[10] = 1.0d / 0.0d;
        ds[11] = -1.0d / 0.0d;
        ds[12] = 0.0d / 0.0d;
        ds[13] = 2.5d;

        int i = 0;
        while (i < 14) {
            double x = ds[i];
            float fx = (float) x;
            if (!StrictTest.same(StrictMath.abs(x), Math.abs(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.abs(fx), Math.abs(fx))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.floor(x), Math.floor(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.ceil(x), Math.ceil(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.rint(x), Math.rint(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.sqrt(x), Math.sqrt(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.signum(x), Math.signum(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.signum(fx), Math.signum(fx))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.ulp(x), Math.ulp(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.ulp(fx), Math.ulp(fx))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.nextUp(x), Math.nextUp(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.nextUp(fx), Math.nextUp(fx))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.nextDown(x), Math.nextDown(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.nextDown(fx), Math.nextDown(fx))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.toRadians(x), Math.toRadians(x))) {
                bad = bad + 1;
            }
            if (!StrictTest.same(StrictMath.toDegrees(x), Math.toDegrees(x))) {
                bad = bad + 1;
            }
            if (StrictMath.getExponent(x) != Math.getExponent(x)) {
                bad = bad + 1;
            }
            if (StrictMath.getExponent(fx) != Math.getExponent(fx)) {
                bad = bad + 1;
            }
            if (StrictMath.round(x) != Math.round(x)) {
                bad = bad + 1;
            }
            if (StrictMath.round(fx) != Math.round(fx)) {
                bad = bad + 1;
            }
            int s = -30;
            while (s < 30) {
                if (!StrictTest.same(StrictMath.scalb(x, s), Math.scalb(x, s))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.scalb(fx, s), Math.scalb(fx, s))) {
                    bad = bad + 1;
                }
                s = s + 7;
            }
            int j = 0;
            while (j < 14) {
                double y = ds[j];
                float fy = (float) y;
                if (!StrictTest.same(StrictMath.max(x, y), Math.max(x, y))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.min(x, y), Math.min(x, y))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.max(fx, fy), Math.max(fx, fy))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.min(fx, fy), Math.min(fx, fy))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.copySign(x, y), Math.copySign(x, y))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.copySign(fx, fy), Math.copySign(fx, fy))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.nextAfter(x, y), Math.nextAfter(x, y))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.nextAfter(fx, y), Math.nextAfter(fx, y))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.IEEEremainder(x, y), Math.IEEEremainder(x, y))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.clamp(x, -9.5d, 9.5d), Math.clamp(x, -9.5d, 9.5d))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.clamp(fx, -9.5f, 9.5f), Math.clamp(fx, -9.5f, 9.5f))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.fma(x, y, 3.25d), Math.fma(x, y, 3.25d))) {
                    bad = bad + 1;
                }
                if (!StrictTest.same(StrictMath.fma(fx, fy, 3.25f), Math.fma(fx, fy, 3.25f))) {
                    bad = bad + 1;
                }
                j = j + 1;
            }
            i = i + 1;
        }
        return bad;
    }

    /**
     * The table read off the reference.
     *
     * <p>The one check here that does not go through Math, and therefore the only one that could
     * still fail if this class and Math were wrong in the same way.
     */
    public static int tabla() {
        int bad = 0;
        long[] xs = new long[N];
        long[] ys = new long[N];
        long[] zs = new long[N];
        long[] sq = new long[N];
        long[] ri = new long[N];
        long[] up = new long[N];
        long[] ra = new long[N];
        long[] re = new long[N];
        long[] fm = new long[N];
        long[] de = new long[N];
        StrictTest.fill(xs, ys, zs, sq, ri, up, ra, re, fm, de);
        int i = 0;
        while (i < N) {
            double x = Double.longBitsToDouble(xs[i]);
            double y = Double.longBitsToDouble(ys[i]);
            double z = Double.longBitsToDouble(zs[i]);
            if (Double.doubleToLongBits(StrictMath.sqrt(StrictMath.abs(x))) != sq[i]) {
                bad = bad + 1;
            }
            if (Double.doubleToLongBits(StrictMath.rint(x)) != ri[i]) {
                bad = bad + 1;
            }
            if (Double.doubleToLongBits(StrictMath.ulp(x)) != up[i]) {
                bad = bad + 1;
            }
            if (Double.doubleToLongBits(StrictMath.toRadians(x)) != ra[i]) {
                bad = bad + 1;
            }
            if (Double.doubleToLongBits(StrictMath.toDegrees(x)) != de[i]) {
                bad = bad + 1;
            }
            if (Double.doubleToLongBits(StrictMath.IEEEremainder(x, y)) != re[i]) {
                bad = bad + 1;
            }
            if (Double.doubleToLongBits(StrictMath.fma(x, y, z)) != fm[i]) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        return bad;
    }

    /** A double in [0, 1), which is all this one promises. */
    public static int azar() {
        int bad = 0;
        int i = 0;
        while (i < 500) {
            double d = StrictMath.random();
            if (d != d || d < 0.0d) {
                bad = bad + 1;
            }
            if (d >= 1.0d) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        return bad;
    }

    public static int todo() {
        return StrictTest.constantes() + StrictTest.enteros() + StrictTest.flotantes()
                + StrictTest.tabla() + StrictTest.azar();
    }

    public static void main(String[] args) {
        System.out.println("constantes  " + StrictTest.constantes());
        System.out.println("enteros     " + StrictTest.enteros());
        System.out.println("flotantes   " + StrictTest.flotantes());
        System.out.println("tabla       " + StrictTest.tabla());
        System.out.println("azar        " + StrictTest.azar());
        System.out.println("TOTAL       " + StrictTest.todo());
    }

    // The reference values, filled here rather than in a static initialiser: a table this size in
    // <clinit> is how a class runs into the 64 KB method limit.
    static void fill(long[] xs, long[] ys, long[] zs, long[] sq, long[] ri, long[] up,
                     long[] ra, long[] re, long[] fm, long[] de) {
        xs[0] = 0L;
        xs[1] = -9223372036854775808L;
        xs[2] = 4607182418800017408L;
        xs[3] = -4616189618054758400L;
        xs[4] = 4602678819172646912L;
        xs[5] = 4611686018427387904L;
        xs[6] = 2024L;
        xs[7] = 1L;
        xs[8] = 9218868437227405311L;
        xs[9] = 4613937818241073152L;
        xs[10] = 4620130267728707584L;
        xs[11] = -4603241769126068224L;
        xs[12] = 4831355200913801216L;
        xs[13] = 4683220299150161609L;
        xs[14] = 4963103797625998831L;
        xs[15] = -563368162015474120L;
        xs[16] = -6765585436902926033L;
        xs[17] = 7221905667373975845L;
        xs[18] = 6086658595655551033L;
        xs[19] = 1945802604917180299L;
        xs[20] = 4431996654098195063L;
        xs[21] = 2099240432372974034L;
        xs[22] = 8933341222687082127L;
        xs[23] = 6075577016135370797L;
        ys[0] = 4613937818241073152L;
        ys[1] = -9127805872893400528L;
        ys[2] = 1246231399943979940L;
        ys[3] = -8697452611878632802L;
        ys[4] = 8929947503871769294L;
        ys[5] = 4613937818241073152L;
        ys[6] = 6548941588262202948L;
        ys[7] = 7752823962274622237L;
        ys[8] = 6340114585227418679L;
        ys[9] = -6555006163404250190L;
        ys[10] = 4613937818241073152L;
        ys[11] = 8490133062284205939L;
        ys[12] = 1347799425809652579L;
        ys[13] = 5866390419461258247L;
        ys[14] = 8322357686023406492L;
        ys[15] = 4613937818241073152L;
        ys[16] = 4168199855841719315L;
        ys[17] = -6199682392805165492L;
        ys[18] = -6310710157013567002L;
        ys[19] = -9055738221218133394L;
        ys[20] = 4613937818241073152L;
        ys[21] = -8523298532772320241L;
        ys[22] = 1034044353411041869L;
        ys[23] = 1656596267170241702L;
        zs[0] = -8416433659064116520L;
        zs[1] = -8967630352209260279L;
        zs[2] = 1928306453672309043L;
        zs[3] = -2137043066256646160L;
        zs[4] = -6723170880651668516L;
        zs[5] = 111963125957630484L;
        zs[6] = 2573469114164236888L;
        zs[7] = -4566462652553982822L;
        zs[8] = -3489669689510004409L;
        zs[9] = 6050764896931172815L;
        zs[10] = 1133721135534842288L;
        zs[11] = 3530462783596595532L;
        zs[12] = -3286703832539410230L;
        zs[13] = -7590085612066934514L;
        zs[14] = 1388822556189468754L;
        zs[15] = 986813368499954691L;
        zs[16] = -6839836213832987691L;
        zs[17] = 899907924766372026L;
        zs[18] = -6401873464544351493L;
        zs[19] = -2577854070927215020L;
        zs[20] = -3776245678095740152L;
        zs[21] = 8235532604316661923L;
        zs[22] = 1065823823645008749L;
        zs[23] = 3771573124384926601L;
        sq[0] = 0L;
        sq[1] = 0L;
        sq[2] = 4607182418800017408L;
        sq[3] = 4607182418800017408L;
        sq[4] = 4604544271217802189L;
        sq[5] = 4609047870845172685L;
        sq[6] = 2213095440444558963L;
        sq[7] = 2188749418902061056L;
        sq[8] = 6913025428013711359L;
        sq[9] = 4610479282544200874L;
        sq[10] = 4613349226564724111L;
        sq[11] = 4613349226564724111L;
        sq[12] = 4719253884686597935L;
        sq[13] = 4644888880265906422L;
        sq[14] = 4784784409968475671L;
        sq[15] = 6633590661578042237L;
        sq[16] = 3532256183812741429L;
        sq[17] = 5914392939274841693L;
        sq[18] = 5346802729999678422L;
        sq[19] = 3276155094869965861L;
        sq[20] = 4519289616389149113L;
        sq[21] = 3352929959953831930L;
        sq[22] = 6770103689303553545L;
        sq[23] = 5341378392726048926L;
        ri[0] = 0L;
        ri[1] = -9223372036854775808L;
        ri[2] = 4607182418800017408L;
        ri[3] = -4616189618054758400L;
        ri[4] = 0L;
        ri[5] = 4611686018427387904L;
        ri[6] = 0L;
        ri[7] = 0L;
        ri[8] = 9218868437227405311L;
        ri[9] = 4613937818241073152L;
        ri[10] = 4620693217682128896L;
        ri[11] = -4602678819172646912L;
        ri[12] = 4831355200913801216L;
        ri[13] = 4683220313649971200L;
        ri[14] = 4963103797625998831L;
        ri[15] = -563368162015474120L;
        ri[16] = -9223372036854775808L;
        ri[17] = 7221905667373975845L;
        ri[18] = 6086658595655551033L;
        ri[19] = 0L;
        ri[20] = 0L;
        ri[21] = 0L;
        ri[22] = 8933341222687082127L;
        ri[23] = 6075577016135370797L;
        up[0] = 1L;
        up[1] = 1L;
        up[2] = 4372995238176751616L;
        up[3] = 4372995238176751616L;
        up[4] = 4368491638549381120L;
        up[5] = 4377498837804122112L;
        up[6] = 1L;
        up[7] = 1L;
        up[8] = 8980177656976769024L;
        up[9] = 4377498837804122112L;
        up[10] = 4382002437431492608L;
        up[11] = 4382002437431492608L;
        up[12] = 4593671619917905920L;
        up[13] = 4445052832214679552L;
        up[14] = 4728779608739020800L;
        up[15] = 8421731303182827520L;
        up[16] = 2220274616293654528L;
        up[17] = 6985083022051639296L;
        up[18] = 5850175915954274304L;
        up[19] = 1711367858400788480L;
        up[20] = 4197354852709302272L;
        up[21] = 1864490245731385344L;
        up[22] = 8696450880452427776L;
        up[23] = 5841168716699533312L;
        ra[0] = 0L;
        ra[1] = -9223372036854775808L;
        ra[2] = 4580687790476533049L;
        ra[3] = -4642684246378242759L;
        ra[4] = 4576184190849162553L;
        ra[5] = 4585191390103903545L;
        ra[6] = 35L;
        ra[7] = 0L;
        ra[8] = 9192373808903920952L;
        ra[9] = 4587706674637958102L;
        ra[10] = 4593884178791887717L;
        ra[11] = -4629487858062888091L;
        ra[12] = 4805269688919789207L;
        ra[13] = 4656956710850024503L;
        ra[14] = 4936625200738272264L;
        ra[15] = -589678153486500862L;
        ra[16] = -6791691027625191429L;
        ra[17] = 7195719416675828169L;
        ra[18] = 6060432565334873978L;
        ra[19] = 1919336944457414045L;
        ra[20] = 4405555221278403935L;
        ra[21] = 2072811681789776575L;
        ra[22] = 8907162893253472940L;
        ra[23] = 6049108261083633226L;
        re[0] = 0L;
        re[1] = -9223372036854775808L;
        re[2] = -7987037084371558432L;
        re[3] = 514948392122757112L;
        re[4] = 4602678819172646912L;
        re[5] = -4616189618054758400L;
        re[6] = 2024L;
        re[7] = 1L;
        re[8] = 6335529437728251434L;
        re[9] = 2661587141114326768L;
        re[10] = 4609434218613702656L;
        re[11] = -4603241769126068224L;
        re[12] = 1339946464743212944L;
        re[13] = 4683220299150161609L;
        re[14] = 4963103797625998831L;
        re[15] = -9223372036854775808L;
        re[16] = -6765585436902926033L;
        re[17] = 3008478420592799296L;
        re[18] = -6319199338541951904L;
        re[19] = -9066629473223905664L;
        re[20] = 4431996654098195063L;
        re[21] = -8541717353342409776L;
        re[22] = -8201266968730785288L;
        re[23] = 1646477759349920640L;
        fm[0] = -8416433659064116520L;
        fm[1] = -8967630352209260279L;
        fm[2] = 1928306453672309043L;
        fm[3] = -2137043066256646160L;
        fm[4] = 8925443904244398798L;
        fm[5] = 4618441417868443648L;
        fm[6] = 2573469114164236888L;
        fm[7] = -4566462652553982822L;
        fm[8] = 9218868437227405312L;
        fm[9] = 6050764896931172815L;
        fm[10] = 4627026404658118656L;
        fm[11] = -720062635958345348L;
        fm[12] = -3286703832539410230L;
        fm[13] = 5942532872719589308L;
        fm[14] = 8678407116120842832L;
        fm[15] = -556508210425877846L;
        fm[16] = -6839836213832987691L;
        fm[17] = -3584393073594668758L;
        fm[18] = -4830947872583723748L;
        fm[19] = -2577854070927215020L;
        fm[20] = -3776245678095740152L;
        fm[21] = 8235532604316661923L;
        fm[22] = 5360559671096554177L;
        fm[23] = 3771573124384926601L;
        de[0] = 0L;
        de[1] = -9223372036854775808L;
        de[2] = 4633260481411531256L;
        de[3] = -4590111555443244552L;
        de[4] = 4628756881784160760L;
        de[5] = 4637764081038901752L;
        de[6] = 115967L;
        de[7] = 57L;
        de[8] = 9218868437227405312L;
        de[9] = 4640251764640764282L;
        de[10] = 4646267301287265752L;
        de[11] = -4577104735567510056L;
        de[12] = 4857538771117159482L;
        de[13] = 4709353180634308798L;
        de[14] = 4989290164325960649L;
        de[15] = -537246290612947979L;
        de[16] = -6739383891287272310L;
        de[17] = 7248179423555155843L;
        de[18] = 6112967964406755358L;
        de[19] = 1972076366661427750L;
        de[20] = 4458434091292501700L;
        de[21] = 2125731285740806535L;
        de[22] = 8959607887383473655L;
        de[23] = 6101829871644666149L;
    }
}
