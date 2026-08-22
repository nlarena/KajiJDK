package java.math;

// KajiLibrary's java.math.MathContext — how many significant digits to keep, and how to round when
// dropping the rest. It is the pair that turns BigDecimal's exact arithmetic into a bounded one.
//
// A precision of 0 means UNLIMITED: keep every digit, and let an operation that cannot be
// represented exactly (1/3) throw instead of silently rounding. That is the honest default for a
// type whose whole point is not to lose digits behind your back.
//
// The three named contexts are the IEEE-754 decimal formats: 7, 16 and 34 significant digits, all
// with HALF_EVEN, which is the rounding that does not drift.
public final class MathContext {

    // Built with `RoundingMode.valueOf(...)` rather than reading `RoundingMode.HALF_EVEN`: a
    // cross-class read of a static field emits `getfield` and traps at runtime (finding #110),
    // while a static method call resolves correctly.
    public static final MathContext UNLIMITED = new MathContext(0, RoundingMode.valueOf("HALF_UP"));

    public static final MathContext DECIMAL32 = new MathContext(7, RoundingMode.valueOf("HALF_EVEN"));

    public static final MathContext DECIMAL64 = new MathContext(16, RoundingMode.valueOf("HALF_EVEN"));

    public static final MathContext DECIMAL128 = new MathContext(34, RoundingMode.valueOf("HALF_EVEN"));

    private final int precision;
    private final RoundingMode roundingMode;

    public MathContext(int setPrecision) {
        this(setPrecision, RoundingMode.valueOf("HALF_UP"));
    }

    public MathContext(int setPrecision, RoundingMode setRoundingMode) {
        if (setPrecision < 0) {
            throw new IllegalArgumentException("Digits < 0");
        }
        if (setRoundingMode == null) {
            throw new NullPointerException("null RoundingMode");
        }
        this.precision = setPrecision;
        this.roundingMode = setRoundingMode;
    }

    // The inverse of toString(): "precision=9 roundingMode=HALF_UP".
    //
    // Parsed with charAt/substring(int,int) only: KajiLibrary's String has no `indexOf` or
    // one-argument `substring`, and Integer has no `parseInt`. Writing the scan by hand is smaller
    // than widening two core classes that are already validated against the JDK.
    public MathContext(String val) {
        int space = -1;
        int i = 0;
        while (i < val.length()) {
            if (val.charAt(i) == ' ' && space < 0) {
                space = i;
            }
            i = i + 1;
        }
        if (space < 0 || !val.startsWith("precision=")) {
            throw new IllegalArgumentException("bad string format");
        }
        String rest = val.substring(space + 1, val.length());
        if (!rest.startsWith("roundingMode=")) {
            throw new IllegalArgumentException("bad string format");
        }
        int prec = 0;
        int d = 10;
        if (d >= space) {
            throw new IllegalArgumentException("bad string format");
        }
        while (d < space) {
            char c = val.charAt(d);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("bad string format");
            }
            prec = prec * 10 + (c - '0');
            d = d + 1;
        }
        this.precision = prec;
        this.roundingMode = RoundingMode.valueOf(rest.substring(13, rest.length()));
    }

    public int getPrecision() {
        return this.precision;
    }

    public RoundingMode getRoundingMode() {
        return this.roundingMode;
    }

    public boolean equals(Object x) {
        if (this == x) {
            return true;
        }
        if (x instanceof MathContext) {
            MathContext mc = (MathContext) x;
            return mc.precision == this.precision && mc.roundingMode == this.roundingMode;
        }
        return false;
    }

    public int hashCode() {
        return this.precision + this.roundingMode.label().hashCode() * 59;
    }

    public String toString() {
        return "precision=" + Integer.toString(this.precision)
                + " roundingMode=" + this.roundingMode.label();
    }
}
