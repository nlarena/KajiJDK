package java.lang;

/**
 * KajiLibrary's java.lang.StrictMath.
 *
 * <p>The name is a promise about <em>bit patterns</em>. Ordinary {@link Math} is allowed to take
 * whatever the hardware offers -- a platform's {@code sin} may differ in the last bit from
 * another's, and the JIT may substitute an intrinsic -- whereas everything here is required to
 * produce the same bits on every machine. That is what makes it usable in a checksum, a replayed
 * simulation, or anything two processes have to agree on.
 *
 * <p>Which is why almost every method below simply calls {@code Math}, and why that is the
 * correct implementation rather than a shortcut. Each of these operations has exactly ONE right
 * answer: the integer ones are exact by definition, the bit-level ones read the representation,
 * and {@code sqrt}, {@code rint}, {@code IEEEremainder} and {@code fma} are among the operations
 * IEEE-754 requires to be correctly rounded. For an operation like that, "strict" and "ordinary"
 * are not two functions that happen to agree -- they are the same function, and the only way to
 * guarantee they never drift apart is for there to be one implementation. Two copies of
 * {@code floorMod} would be two things that could disagree; forwarding cannot.
 *
 * <p>The eighteen that are absent -- {@code sin}, {@code cos}, {@code tan}, {@code asin},
 * {@code acos}, {@code atan}, {@code atan2}, {@code exp}, {@code log}, {@code log10},
 * {@code log1p}, {@code expm1}, {@code cbrt}, {@code pow}, {@code sinh}, {@code cosh},
 * {@code tanh} and {@code hypot} -- are exactly the ones where "strict" carries information.
 * Those are approximations whose contract is stated in ulps, and the JDK meets it by shipping a
 * named implementation (fdlibm) rather than by computing a defined value. There is no way to
 * write them "carefully enough" to land on the same bits; either the algorithm is the same one
 * or the answers differ. Until that port exists they are omitted rather than approximated,
 * because a {@code StrictMath.sin} that quietly disagreed with every other JVM would be a lie in
 * the API, and worse than a missing method.
 */
public final class StrictMath {

    // Non-instantiable: a static-only utility, exactly as the JDK hides it. Without this,
    // javac would synthesize a *public* default constructor.
    private StrictMath() {
    }

    /** The base of the natural logarithms. */
    public static final double E = 2.718281828459045d;

    /** The ratio of a circle's circumference to its diameter. */
    public static final double PI = 3.141592653589793d;

    /** The ratio of a circle's circumference to its radius -- two PI. */
    public static final double TAU = 6.283185307179586d;


    // ---- magnitude, extremes and clamping ----

    /**
     * The absolute value; the most negative int answers itself.
     *
     * @see Math#abs(int)
     */
    public static int abs(int a) {
        return Math.abs(a);
    }

    /**
     * The absolute value; the most negative long answers itself.
     *
     * @see Math#abs(long)
     */
    public static long abs(long a) {
        return Math.abs(a);
    }

    /**
     * The absolute value, sign bit cleared.
     *
     * @see Math#abs(double)
     */
    public static double abs(double a) {
        return Math.abs(a);
    }

    /**
     * The absolute value, sign bit cleared.
     *
     * @see Math#abs(float)
     */
    public static float abs(float a) {
        return Math.abs(a);
    }

    /**
     * The absolute value, or an exception where there is none.
     *
     * @see Math#absExact(int)
     */
    public static int absExact(int a) {
        return Math.absExact(a);
    }

    /**
     * The absolute value, or an exception where there is none.
     *
     * @see Math#absExact(long)
     */
    public static long absExact(long a) {
        return Math.absExact(a);
    }

    /**
     * The greater of the two.
     *
     * @see Math#max(int, int)
     */
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    /**
     * The greater of the two.
     *
     * @see Math#max(long, long)
     */
    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    /**
     * The greater of the two, with NaN winning and +0.0 beating -0.0.
     *
     * @see Math#max(double, double)
     */
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    /**
     * The greater of the two, with NaN winning and +0.0 beating -0.0.
     *
     * @see Math#max(float, float)
     */
    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    /**
     * The lesser of the two.
     *
     * @see Math#min(int, int)
     */
    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    /**
     * The lesser of the two.
     *
     * @see Math#min(long, long)
     */
    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    /**
     * The lesser of the two, with NaN winning and -0.0 beating +0.0.
     *
     * @see Math#min(double, double)
     */
    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    /**
     * The lesser of the two, with NaN winning and -0.0 beating +0.0.
     *
     * @see Math#min(float, float)
     */
    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    /**
     * {@code value} confined to the range, narrowed to an int.
     *
     * @see Math#clamp(long, int, int)
     */
    public static int clamp(long value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    /**
     * {@code value} confined to the range.
     *
     * @see Math#clamp(long, long, long)
     */
    public static long clamp(long value, long min, long max) {
        return Math.clamp(value, min, max);
    }

    /**
     * {@code value} confined to the range.
     *
     * @see Math#clamp(double, double, double)
     */
    public static double clamp(double value, double min, double max) {
        return Math.clamp(value, min, max);
    }

    /**
     * {@code value} confined to the range.
     *
     * @see Math#clamp(float, float, float)
     */
    public static float clamp(float value, float min, float max) {
        return Math.clamp(value, min, max);
    }


    // ---- division that rounds where you asked ----

    // C-style division truncates toward zero, which is the wrong rounding for anything
    // indexed: it makes -1/2 and 1/2 both zero, so a bucket index breaks at the origin. These
    // round the way the caller asked, and each remainder is the one that goes with its
    // quotient.

    /**
     * The quotient, rounded toward negative infinity.
     *
     * @see Math#floorDiv(int, int)
     */
    public static int floorDiv(int x, int y) {
        return Math.floorDiv(x, y);
    }

    /**
     * The quotient, rounded toward negative infinity.
     *
     * @see Math#floorDiv(long, long)
     */
    public static long floorDiv(long x, long y) {
        return Math.floorDiv(x, y);
    }

    /**
     * The quotient, rounded toward negative infinity.
     *
     * @see Math#floorDiv(long, int)
     */
    public static long floorDiv(long x, int y) {
        return Math.floorDiv(x, y);
    }

    /**
     * The remainder that goes with {@link #floorDiv(int, int)}.
     *
     * @see Math#floorMod(int, int)
     */
    public static int floorMod(int x, int y) {
        return Math.floorMod(x, y);
    }

    /**
     * The remainder that goes with {@link #floorDiv(long, long)}.
     *
     * @see Math#floorMod(long, long)
     */
    public static long floorMod(long x, long y) {
        return Math.floorMod(x, y);
    }

    /**
     * The remainder that goes with {@link #floorDiv(long, int)}.
     *
     * @see Math#floorMod(long, int)
     */
    public static int floorMod(long x, int y) {
        return Math.floorMod(x, y);
    }

    /**
     * The quotient, rounded toward positive infinity.
     *
     * @see Math#ceilDiv(int, int)
     */
    public static int ceilDiv(int x, int y) {
        return Math.ceilDiv(x, y);
    }

    /**
     * The quotient, rounded toward positive infinity.
     *
     * @see Math#ceilDiv(long, long)
     */
    public static long ceilDiv(long x, long y) {
        return Math.ceilDiv(x, y);
    }

    /**
     * The quotient, rounded toward positive infinity.
     *
     * @see Math#ceilDiv(long, int)
     */
    public static long ceilDiv(long x, int y) {
        return Math.ceilDiv(x, y);
    }

    /**
     * The remainder that goes with {@link #ceilDiv(int, int)}.
     *
     * @see Math#ceilMod(int, int)
     */
    public static int ceilMod(int x, int y) {
        return Math.ceilMod(x, y);
    }

    /**
     * The remainder that goes with {@link #ceilDiv(long, long)}.
     *
     * @see Math#ceilMod(long, long)
     */
    public static long ceilMod(long x, long y) {
        return Math.ceilMod(x, y);
    }

    /**
     * The remainder that goes with {@link #ceilDiv(long, int)}.
     *
     * @see Math#ceilMod(long, int)
     */
    public static int ceilMod(long x, int y) {
        return Math.ceilMod(x, y);
    }


    // ---- arithmetic that refuses to wrap ----

    // Java's arithmetic wraps in silence, which is the right default for a hash and the wrong
    // one for a size or an index. These throw instead of returning a plausible wrong number.

    /**
     * The sum, or {@code ArithmeticException} on overflow.
     *
     * @see Math#addExact(int, int)
     */
    public static int addExact(int x, int y) {
        return Math.addExact(x, y);
    }

    /**
     * The sum, or {@code ArithmeticException} on overflow.
     *
     * @see Math#addExact(long, long)
     */
    public static long addExact(long x, long y) {
        return Math.addExact(x, y);
    }

    /**
     * The difference, or {@code ArithmeticException} on overflow.
     *
     * @see Math#subtractExact(int, int)
     */
    public static int subtractExact(int x, int y) {
        return Math.subtractExact(x, y);
    }

    /**
     * The difference, or {@code ArithmeticException} on overflow.
     *
     * @see Math#subtractExact(long, long)
     */
    public static long subtractExact(long x, long y) {
        return Math.subtractExact(x, y);
    }

    /**
     * The product, or {@code ArithmeticException} on overflow.
     *
     * @see Math#multiplyExact(int, int)
     */
    public static int multiplyExact(int x, int y) {
        return Math.multiplyExact(x, y);
    }

    /**
     * The product, or {@code ArithmeticException} on overflow.
     *
     * @see Math#multiplyExact(long, long)
     */
    public static long multiplyExact(long x, long y) {
        return Math.multiplyExact(x, y);
    }

    /**
     * The product, or {@code ArithmeticException} on overflow.
     *
     * @see Math#multiplyExact(long, int)
     */
    public static long multiplyExact(long x, int y) {
        return Math.multiplyExact(x, y);
    }

    /**
     * The quotient, or {@code ArithmeticException} on the one case that overflows.
     *
     * @see Math#divideExact(int, int)
     */
    public static int divideExact(int x, int y) {
        return Math.divideExact(x, y);
    }

    /**
     * The quotient, or {@code ArithmeticException} on the one case that overflows.
     *
     * @see Math#divideExact(long, long)
     */
    public static long divideExact(long x, long y) {
        return Math.divideExact(x, y);
    }

    /**
     * {@link #floorDiv(int, int)}, refusing to overflow.
     *
     * @see Math#floorDivExact(int, int)
     */
    public static int floorDivExact(int x, int y) {
        return Math.floorDivExact(x, y);
    }

    /**
     * {@link #floorDiv(long, long)}, refusing to overflow.
     *
     * @see Math#floorDivExact(long, long)
     */
    public static long floorDivExact(long x, long y) {
        return Math.floorDivExact(x, y);
    }

    /**
     * {@link #ceilDiv(int, int)}, refusing to overflow.
     *
     * @see Math#ceilDivExact(int, int)
     */
    public static int ceilDivExact(int x, int y) {
        return Math.ceilDivExact(x, y);
    }

    /**
     * {@link #ceilDiv(long, long)}, refusing to overflow.
     *
     * @see Math#ceilDivExact(long, long)
     */
    public static long ceilDivExact(long x, long y) {
        return Math.ceilDivExact(x, y);
    }

    /**
     * One more, or {@code ArithmeticException} at the ceiling.
     *
     * @see Math#incrementExact(int)
     */
    public static int incrementExact(int a) {
        return Math.incrementExact(a);
    }

    /**
     * One more, or {@code ArithmeticException} at the ceiling.
     *
     * @see Math#incrementExact(long)
     */
    public static long incrementExact(long a) {
        return Math.incrementExact(a);
    }

    /**
     * One less, or {@code ArithmeticException} at the floor.
     *
     * @see Math#decrementExact(int)
     */
    public static int decrementExact(int a) {
        return Math.decrementExact(a);
    }

    /**
     * One less, or {@code ArithmeticException} at the floor.
     *
     * @see Math#decrementExact(long)
     */
    public static long decrementExact(long a) {
        return Math.decrementExact(a);
    }

    /**
     * The negation, or {@code ArithmeticException} at the most negative value.
     *
     * @see Math#negateExact(int)
     */
    public static int negateExact(int a) {
        return Math.negateExact(a);
    }

    /**
     * The negation, or {@code ArithmeticException} at the most negative value.
     *
     * @see Math#negateExact(long)
     */
    public static long negateExact(long a) {
        return Math.negateExact(a);
    }

    /**
     * The value as an int, or {@code ArithmeticException} if it does not fit.
     *
     * @see Math#toIntExact(long)
     */
    public static int toIntExact(long value) {
        return Math.toIntExact(value);
    }

    /**
     * {@code b} to the {@code e}, refusing to overflow.
     *
     * @see Math#powExact(int, int)
     */
    public static int powExact(int b, int e) {
        return Math.powExact(b, e);
    }

    /**
     * {@code b} to the {@code e}, refusing to overflow.
     *
     * @see Math#powExact(long, int)
     */
    public static long powExact(long b, int e) {
        return Math.powExact(b, e);
    }

    /**
     * The product read as unsigned, refusing to overflow.
     *
     * @see Math#unsignedMultiplyExact(int, int)
     */
    public static int unsignedMultiplyExact(int x, int y) {
        return Math.unsignedMultiplyExact(x, y);
    }

    /**
     * The product read as unsigned, refusing to overflow.
     *
     * @see Math#unsignedMultiplyExact(long, long)
     */
    public static long unsignedMultiplyExact(long x, long y) {
        return Math.unsignedMultiplyExact(x, y);
    }

    /**
     * The product read as unsigned, refusing to overflow.
     *
     * @see Math#unsignedMultiplyExact(long, int)
     */
    public static long unsignedMultiplyExact(long x, int y) {
        return Math.unsignedMultiplyExact(x, y);
    }

    /**
     * {@code b} to the {@code e}, read as unsigned, refusing to overflow.
     *
     * @see Math#unsignedPowExact(int, int)
     */
    public static int unsignedPowExact(int b, int e) {
        return Math.unsignedPowExact(b, e);
    }

    /**
     * {@code b} to the {@code e}, read as unsigned, refusing to overflow.
     *
     * @see Math#unsignedPowExact(long, int)
     */
    public static long unsignedPowExact(long b, int e) {
        return Math.unsignedPowExact(b, e);
    }


    // ---- the whole product ----

    // A product of two longs has 128 bits and a long holds 64. These give the half that
    // ordinary multiplication throws away, which is what makes 128-bit arithmetic possible at
    // all.

    /**
     * The exact product of two ints, which always fits in a long.
     *
     * @see Math#multiplyFull(int, int)
     */
    public static long multiplyFull(int x, int y) {
        return Math.multiplyFull(x, y);
    }

    /**
     * The upper 64 bits of the signed 128-bit product.
     *
     * @see Math#multiplyHigh(long, long)
     */
    public static long multiplyHigh(long x, long y) {
        return Math.multiplyHigh(x, y);
    }

    /**
     * The upper 64 bits of the unsigned 128-bit product.
     *
     * @see Math#unsignedMultiplyHigh(long, long)
     */
    public static long unsignedMultiplyHigh(long x, long y) {
        return Math.unsignedMultiplyHigh(x, y);
    }


    // ---- rounding ----

    // `round` answers an integer TYPE and saturates; `floor`, `ceil` and `rint` answer a
    // double that happens to be integral, so they keep working past 2^63 where an integer type
    // has nothing to say. All of them are decided by the bits.

    /**
     * The nearest long, halves going up.
     *
     * @see Math#round(double)
     */
    public static long round(double a) {
        return Math.round(a);
    }

    /**
     * The nearest int, halves going up.
     *
     * @see Math#round(float)
     */
    public static int round(float a) {
        return Math.round(a);
    }

    /**
     * The largest integral value not greater than {@code a}.
     *
     * @see Math#floor(double)
     */
    public static double floor(double a) {
        return Math.floor(a);
    }

    /**
     * The smallest integral value not less than {@code a}.
     *
     * @see Math#ceil(double)
     */
    public static double ceil(double a) {
        return Math.ceil(a);
    }

    /**
     * The nearest integral value, with ties going to the even one.
     *
     * @see Math#rint(double)
     */
    public static double rint(double a) {
        return Math.rint(a);
    }


    // ---- angles ----

    /**
     * Degrees as radians.
     *
     * @see Math#toRadians(double)
     */
    public static double toRadians(double angdeg) {
        return Math.toRadians(angdeg);
    }

    /**
     * Radians as degrees.
     *
     * @see Math#toDegrees(double)
     */
    public static double toDegrees(double angrad) {
        return Math.toDegrees(angrad);
    }


    // ---- reading the representation ----

    // What the next representable value is, or how far apart two neighbours are, is not a
    // property of the number -- it is a property of the FORMAT holding it. Every one of these
    // is exact, because reading bits cannot round.

    /**
     * The unbiased exponent.
     *
     * @see Math#getExponent(double)
     */
    public static int getExponent(double d) {
        return Math.getExponent(d);
    }

    /**
     * The unbiased exponent.
     *
     * @see Math#getExponent(float)
     */
    public static int getExponent(float f) {
        return Math.getExponent(f);
    }

    /**
     * {@code magnitude} carrying the sign bit of {@code sign}.
     *
     * @see Math#copySign(double, double)
     */
    public static double copySign(double magnitude, double sign) {
        return Math.copySign(magnitude, sign);
    }

    /**
     * {@code magnitude} carrying the sign bit of {@code sign}.
     *
     * @see Math#copySign(float, float)
     */
    public static float copySign(float magnitude, float sign) {
        return Math.copySign(magnitude, sign);
    }

    /**
     * The sign as a value; a zero answers itself, keeping its sign.
     *
     * @see Math#signum(double)
     */
    public static double signum(double d) {
        return Math.signum(d);
    }

    /**
     * The sign as a value; a zero answers itself, keeping its sign.
     *
     * @see Math#signum(float)
     */
    public static float signum(float f) {
        return Math.signum(f);
    }

    /**
     * The size of one step of the format at that magnitude.
     *
     * @see Math#ulp(double)
     */
    public static double ulp(double d) {
        return Math.ulp(d);
    }

    /**
     * The size of one step of the format at that magnitude.
     *
     * @see Math#ulp(float)
     */
    public static float ulp(float f) {
        return Math.ulp(f);
    }

    /**
     * The next representable value above.
     *
     * @see Math#nextUp(double)
     */
    public static double nextUp(double d) {
        return Math.nextUp(d);
    }

    /**
     * The next representable value above.
     *
     * @see Math#nextUp(float)
     */
    public static float nextUp(float f) {
        return Math.nextUp(f);
    }

    /**
     * The next representable value below.
     *
     * @see Math#nextDown(double)
     */
    public static double nextDown(double d) {
        return Math.nextDown(d);
    }

    /**
     * The next representable value below.
     *
     * @see Math#nextDown(float)
     */
    public static float nextDown(float f) {
        return Math.nextDown(f);
    }

    /**
     * The adjacent representable value, on the side {@code direction} is.
     *
     * @see Math#nextAfter(double, double)
     */
    public static double nextAfter(double start, double direction) {
        return Math.nextAfter(start, direction);
    }

    /**
     * The adjacent representable value, on the side {@code direction} is.
     *
     * @see Math#nextAfter(float, double)
     */
    public static float nextAfter(float start, double direction) {
        return Math.nextAfter(start, direction);
    }

    /**
     * {@code d} times two to the {@code scaleFactor}, rounded at most once.
     *
     * @see Math#scalb(double, int)
     */
    public static double scalb(double d, int scaleFactor) {
        return Math.scalb(d, scaleFactor);
    }

    /**
     * {@code f} times two to the {@code scaleFactor}, rounded at most once.
     *
     * @see Math#scalb(float, int)
     */
    public static float scalb(float f, int scaleFactor) {
        return Math.scalb(f, scaleFactor);
    }


    // ---- the correctly rounded operations ----

    // Operations IEEE-754 puts in the same class as +, -, * and /: the answer is the
    // representable value nearest the exact result, so there is nothing to approximate and
    // nothing for a platform to disagree about.

    /**
     * The square root, correctly rounded.
     *
     * @see Math#sqrt(double)
     */
    public static double sqrt(double a) {
        return Math.sqrt(a);
    }

    /**
     * The remainder with the quotient taken to the NEAREST integer; always exact.
     *
     * @see Math#IEEEremainder(double, double)
     */
    public static double IEEEremainder(double f1, double f2) {
        return Math.IEEEremainder(f1, f2);
    }

    /**
     * {@code a * b + c}, rounded once rather than twice.
     *
     * @see Math#fma(double, double, double)
     */
    public static double fma(double a, double b, double c) {
        return Math.fma(a, b, c);
    }

    /**
     * {@code a * b + c}, rounded once rather than twice.
     *
     * @see Math#fma(float, float, float)
     */
    public static float fma(float a, float b, float c) {
        return Math.fma(a, b, c);
    }


    // ---- a random number ----

    /**
     * A double in {@code [0.0, 1.0)}.
     *
     * <p>The one method here that promises nothing about reproducibility, and it exists in this
     * class only for symmetry with {@link Math}. The JDK gives it a second generator of its own;
     * that is a distinction without a difference, since no sequence of random numbers was ever
     * going to be the same on two machines.
     */
    public static double random() {
        return Math.random();
    }
}
