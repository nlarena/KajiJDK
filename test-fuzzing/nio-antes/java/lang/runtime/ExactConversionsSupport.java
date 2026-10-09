package java.lang.runtime;

/**
 * The twenty-one questions of the form "does this conversion lose anything?".
 *
 * <h2>What it is for</h2>
 *
 * <p>Since a {@code switch} can have primitive type patterns, the compiler has to emit a test:
 * {@code case byte b} over an {@code int} must accept the value only if the {@code int} <em>fits</em>
 * in a {@code byte}. That test cannot be written once for every pair --each one loses something
 * different-- so the JDK solves it with one method per pair, and this is that table. Generated code
 * calls it, not people.
 *
 * <h2>The idea, and there is only one</h2>
 *
 * <p>Every exact conversion satisfies the same thing: <strong>converting and coming back gives the
 * original value</strong>. Hence nearly all the bodies being {@code n == (T)(U)n}. Converting to
 * {@code U} loses whatever it has to lose; coming back to {@code T} cannot recover it; if they match
 * all the same, nothing was lost.
 *
 * <h2>The two exceptions, which are where the content is</h2>
 *
 * <p><strong>Negative zero.</strong> IEEE-754 has two zeros, {@code +0.0} and {@code -0.0}, and they
 * are different: they differ in the sign bit. The integers have only one. So {@code -0.0f} passes the
 * round trip --{@code (float)(int)(-0.0f)} is {@code 0.0f}, and {@code 0.0f == -0.0f} gives
 * {@code true}-- and yet the conversion <em>did</em> lose something: the sign. That is why every
 * floating-point-to-integer test carries an {@code && !isNegativeZero(n)} stuck to it.
 *
 * <p>That {@code 0.0f == -0.0f} is {@code true} is what makes the comparison insufficient, and it is
 * also why {@link #isNegativeZero(float)} looks at the <em>bits</em>: it is the only way of telling
 * apart two values the {@code ==} operator declares equal.
 *
 * <p><strong>NaN towards {@code float}.</strong> {@link #isDoubleToFloatExact} is the only one that
 * accepts a value failing the round trip, and on purpose: {@code NaN != NaN}, so the comparison gives
 * {@code false} even though a {@code double} NaN converts to a {@code float} without losing anything
 * Java can observe --the NaN's <em>payload</em> is precisely what the specification leaves
 * undefined--. Hence the {@code || n != n}, which is the canonical way of asking "is it NaN?".
 *
 * @since 21
 */
public final class ExactConversionsSupport {

    // Nobody instantiates a table of functions.
    private ExactConversionsSupport() {
    }

    /**
     * Whether {@code n} is {@code -0.0f}.
     *
     * <p>By the bits and not by comparison, because {@code -0.0f == 0.0f} is {@code true} and so no
     * {@code ==} can separate them. {@link Float#floatToRawIntBits} of {@code -0.0f} is exactly
     * {@link Integer#MIN_VALUE}: the sign bit on and everything else zero.
     *
     * <p>It is {@code Raw} and not {@code floatToIntBits} because there is no collapsing of NaNs to
     * do here -- a NaN will never give {@code MIN_VALUE}, and collapsing it would be wasted work in
     * the hottest test this class has.
     */
    private static boolean isNegativeZero(float n) {
        return Float.floatToRawIntBits(n) == Integer.MIN_VALUE;
    }

    /** The same for {@code double}: {@code -0.0} has {@link Long#MIN_VALUE}'s bits. */
    private static boolean isNegativeZero(double n) {
        return Double.doubleToRawLongBits(n) == Long.MIN_VALUE;
    }

    /** Whether {@code n} fits in a {@code byte}, that is, whether it is in {@code [-128, 127]}. */
    public static boolean isIntToByteExact(int n) {
        return n == (int) (byte) n;
    }

    /** Whether {@code n} fits in a {@code short}. */
    public static boolean isIntToShortExact(int n) {
        return n == (int) (short) n;
    }

    /**
     * Whether {@code n} fits in a {@code char}.
     *
     * <p>{@code char} is unsigned, so no negative passes however small: {@code -1} gives
     * {@code 65535} on conversion and does not come back.
     */
    public static boolean isIntToCharExact(int n) {
        return n == (int) (char) n;
    }

    /**
     * Whether {@code n} fits in a {@code float}.
     *
     * <p>It is not a question of range but of <em>precision</em>: a {@code float} has 24 bits of
     * mantissa and an {@code int} has 32, so the large integers get rounded. {@code 16777217} is the
     * first that fails.
     */
    public static boolean isIntToFloatExact(int n) {
        return n == (int) (float) n;
    }

    /** Whether {@code n} fits in a {@code byte}. */
    public static boolean isLongToByteExact(long n) {
        return n == (long) (byte) n;
    }

    /** Whether {@code n} fits in a {@code short}. */
    public static boolean isLongToShortExact(long n) {
        return n == (long) (short) n;
    }

    /** Whether {@code n} fits in a {@code char}. */
    public static boolean isLongToCharExact(long n) {
        return n == (long) (char) n;
    }

    /** Whether {@code n} fits in an {@code int}. */
    public static boolean isLongToIntExact(long n) {
        return n == (long) (int) n;
    }

    /** Whether {@code n} fits in a {@code float}, with the 24 bits of mantissa that implies. */
    public static boolean isLongToFloatExact(long n) {
        return n == (long) (float) n;
    }

    /**
     * Whether {@code n} fits in a {@code double}.
     *
     * <p>53 bits of mantissa against a {@code long}'s 64: most of them fit, the very large ones do
     * not.
     */
    public static boolean isLongToDoubleExact(long n) {
        return n == (long) (double) n;
    }

    /** Whether {@code n} is a {@code byte}'s worth of integer and is not {@code -0.0f}. */
    public static boolean isFloatToByteExact(float n) {
        return n == (float) (byte) n && !isNegativeZero(n);
    }

    /** Whether {@code n} is a {@code short}'s worth of integer and is not {@code -0.0f}. */
    public static boolean isFloatToShortExact(float n) {
        return n == (float) (short) n && !isNegativeZero(n);
    }

    /** Whether {@code n} is a {@code char}'s worth of integer and is not {@code -0.0f}. */
    public static boolean isFloatToCharExact(float n) {
        return n == (float) (char) n && !isNegativeZero(n);
    }

    /** Whether {@code n} is an {@code int}'s worth of integer and is not {@code -0.0f}. */
    public static boolean isFloatToIntExact(float n) {
        return n == (float) (int) n && !isNegativeZero(n);
    }

    /** Whether {@code n} is a {@code long}'s worth of integer and is not {@code -0.0f}. */
    public static boolean isFloatToLongExact(float n) {
        return n == (float) (long) n && !isNegativeZero(n);
    }

    /** Whether {@code n} is a {@code byte}'s worth of integer and is not {@code -0.0}. */
    public static boolean isDoubleToByteExact(double n) {
        return n == (double) (byte) n && !isNegativeZero(n);
    }

    /** Whether {@code n} is a {@code short}'s worth of integer and is not {@code -0.0}. */
    public static boolean isDoubleToShortExact(double n) {
        return n == (double) (short) n && !isNegativeZero(n);
    }

    /** Whether {@code n} is a {@code char}'s worth of integer and is not {@code -0.0}. */
    public static boolean isDoubleToCharExact(double n) {
        return n == (double) (char) n && !isNegativeZero(n);
    }

    /** Whether {@code n} is an {@code int}'s worth of integer and is not {@code -0.0}. */
    public static boolean isDoubleToIntExact(double n) {
        return n == (double) (int) n && !isNegativeZero(n);
    }

    /** Whether {@code n} is a {@code long}'s worth of integer and is not {@code -0.0}. */
    public static boolean isDoubleToLongExact(double n) {
        return n == (double) (long) n && !isNegativeZero(n);
    }

    /**
     * Whether {@code n} fits in a {@code float}.
     *
     * <p>The only one of the twenty-one that accepts something failing the round trip -- see the NaN
     * in the class's description. And the only one of the floating-point ones that does
     * <strong>not</strong> exclude negative zero, because here the destination has two zeros too:
     * {@code -0.0} converts to {@code -0.0f} without losing anything.
     */
    public static boolean isDoubleToFloatExact(double n) {
        return n == (double) (float) n || n != n;
    }
}
