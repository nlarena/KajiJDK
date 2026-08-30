package java.math;

// KajiLibrary's java.math.BigDecimal — exact decimal arithmetic.
//
// REPRESENTATION: an arbitrary-precision `unscaled` integer plus an int `scale`, meaning
//
//     value = unscaled x 10^(-scale)
//
// So 1.23 is (123, scale 2) and 1.2300 is (12300, scale 4). Those are the SAME NUMBER and two
// different BigDecimals, which is the single most surprising thing about this class and the reason
// `equals` and `compareTo` disagree on purpose: equals() compares representations (1.0 != 1.00),
// compareTo() compares values (1.0 == 1.00). Both are useful; conflating them is the bug.
//
// WHY IT EXISTS: a double cannot represent 0.1, so money arithmetic on doubles drifts. Here every
// operation is exact unless you ask for rounding — and division, which is the one operation that
// may have no exact answer, REFUSES to guess: divide() without a rounding mode throws when the
// quotient does not terminate, rather than silently truncating.
//
// A KajiLibrary subset: the MathContext overloads (arithmetic bounded to N significant digits),
// remainder/divideAndRemainder, the scientific-notation string constructors with exponents, and
// toEngineeringString are omitted; MathContext itself exists for the API that takes it.
public final class BigDecimal extends Number implements Comparable<BigDecimal> {

    public static final BigDecimal ZERO = new BigDecimal(BigInteger.valueOf(0L), 0);

    public static final BigDecimal ONE = new BigDecimal(BigInteger.valueOf(1L), 0);

    public static final BigDecimal TWO = new BigDecimal(BigInteger.valueOf(2L), 0);

    public static final BigDecimal TEN = new BigDecimal(BigInteger.valueOf(10L), 0);

    /** @deprecated legacy rounding-mode constants; use {@link RoundingMode}. */
    @Deprecated
    public static final int ROUND_UP = 0;
    /** @deprecated use {@link RoundingMode#DOWN}. */
    @Deprecated
    public static final int ROUND_DOWN = 1;
    /** @deprecated use {@link RoundingMode#CEILING}. */
    @Deprecated
    public static final int ROUND_CEILING = 2;
    /** @deprecated use {@link RoundingMode#FLOOR}. */
    @Deprecated
    public static final int ROUND_FLOOR = 3;
    /** @deprecated use {@link RoundingMode#HALF_UP}. */
    @Deprecated
    public static final int ROUND_HALF_UP = 4;
    /** @deprecated use {@link RoundingMode#HALF_DOWN}. */
    @Deprecated
    public static final int ROUND_HALF_DOWN = 5;
    /** @deprecated use {@link RoundingMode#HALF_EVEN}. */
    @Deprecated
    public static final int ROUND_HALF_EVEN = 6;
    /** @deprecated use {@link RoundingMode#UNNECESSARY}. */
    @Deprecated
    public static final int ROUND_UNNECESSARY = 7;

    private final BigInteger unscaled;
    private final int scale;

    public BigDecimal(BigInteger val) {
        this(val, 0);
    }

    // The JDK exposes this pair directly, so it doubles as the internal constructor: every other
    // factory here funnels through it.
    public BigDecimal(BigInteger unscaledVal, int scaleVal) {
        this.unscaled = unscaledVal;
        this.scale = scaleVal;
    }

    // The EXACT value of the double, not its printed form — and the difference is the whole point.
    // 2.675 as a double is 2.674999999999999822..., so a formatter that rounds it to two places
    // must produce 2.67, not 2.68. Going through Double.toString (as valueOf does) would round the
    // shortest decimal instead and give the wrong answer. The JDK draws the same distinction
    // between `new BigDecimal(double)` and `BigDecimal.valueOf(double)`.
    //
    // IEEE-754: value = mantissa x 2^exponent. A negative exponent is turned into a decimal scale
    // using 2^-n = 5^n / 10^n, which is exact — every binary fraction IS a finite decimal.
    public BigDecimal(double val) {
        long bits = Double.doubleToLongBits(val);
        int sign = 1;
        if (bits < 0L) {
            sign = -1;
        }
        int exponent = (int) ((bits >> 52) & 2047L);
        long mantissa;
        if (exponent == 0) {
            mantissa = (bits & 4503599627370495L) << 1;      // subnormal: no implicit leading 1
        } else {
            mantissa = (bits & 4503599627370495L) | 4503599627370496L;
        }
        if (exponent == 2047) {
            throw new NumberFormatException("Infinite or NaN");
        }
        exponent = exponent - 1075;
        if (mantissa == 0L) {
            this.unscaled = BigInteger.valueOf(0L);
            this.scale = 0;
            return;
        }
        // Drop trailing zero bits so the resulting scale is no larger than it has to be.
        while ((mantissa & 1L) == 0L && exponent < 0) {
            mantissa = mantissa >> 1;
            exponent = exponent + 1;
        }
        BigInteger m = BigInteger.valueOf(mantissa);
        if (sign < 0) {
            m = m.negate();
        }
        if (exponent >= 0) {
            this.unscaled = m.shiftLeft(exponent);
            this.scale = 0;
        } else {
            this.unscaled = m.multiply(BigInteger.valueOf(5L).pow(-exponent));
            this.scale = -exponent;
        }
    }

    public BigDecimal(int val) {
        this(BigInteger.valueOf((long) val), 0);
    }

    public BigDecimal(long val) {
        this(BigInteger.valueOf(val), 0);
    }

    // Plain decimal only: an optional sign, digits, and at most one point. The exponent forms
    // ("1.2E-5") are omitted, which is why this is a subset.
    public BigDecimal(String val) {
        if (val.length() == 0) {
            throw new NumberFormatException("empty String");
        }
        StringBuilder digits = new StringBuilder();
        int scaleSoFar = 0;
        boolean seenPoint = false;
        boolean seenDigit = false;
        int i = 0;
        char first = val.charAt(0);
        if (first == '-' || first == '+') {
            digits.append(first);
            i = 1;
        }
        while (i < val.length()) {
            char c = val.charAt(i);
            if (c == '.') {
                if (seenPoint) {
                    throw new NumberFormatException("multiple points");
                }
                seenPoint = true;
            } else if (c >= '0' && c <= '9') {
                digits.append(c);
                seenDigit = true;
                if (seenPoint) {
                    scaleSoFar = scaleSoFar + 1;
                }
            } else {
                throw new NumberFormatException("Character is neither a decimal digit number, "
                        + "decimal point, nor \"e\" notation exponential mark.");
            }
            i = i + 1;
        }
        if (!seenDigit) {
            throw new NumberFormatException("no digits found");
        }
        this.unscaled = new BigInteger(digits.toString());
        this.scale = scaleSoFar;
    }

    public static BigDecimal valueOf(long val) {
        return new BigDecimal(BigInteger.valueOf(val), 0);
    }

    public static BigDecimal valueOf(long unscaledVal, int scale) {
        return new BigDecimal(BigInteger.valueOf(unscaledVal), scale);
    }

    // Goes through the double's SHORTEST decimal representation, not its exact binary value —
    // so valueOf(0.1) is exactly 0.1, while `new BigDecimal(0.1)` in the JDK would give the full
    // 0.1000000000000000055511151231257827... That difference is deliberate in the JDK too.
    public static BigDecimal valueOf(double val) {
        return new BigDecimal(Double.toString(val));
    }

    // ---- scale plumbing ----

    private static BigInteger tenPow(int n) {
        return BigInteger.valueOf(10L).pow(n);
    }

    // Restate this value with a larger scale, which is exact: multiply the unscaled value by a
    // power of ten. Used to line two operands up before adding.
    private BigInteger unscaledAtScale(int target) {
        int diff = target - this.scale;
        if (diff <= 0) {
            return this.unscaled;
        }
        return this.unscaled.multiply(BigDecimal.tenPow(diff));
    }

    public int scale() {
        return this.scale;
    }

    public BigInteger unscaledValue() {
        return this.unscaled;
    }

    // The number of significant digits. Zero has precision 1 by definition (the JDK's rule).
    public int precision() {
        String s = this.unscaled.abs().toString();
        return s.length();
    }

    public int signum() {
        return this.unscaled.signum();
    }

    // ---- arithmetic ----

    public BigDecimal add(BigDecimal augend) {
        int target = this.scale;
        if (augend.scale > target) {
            target = augend.scale;
        }
        BigInteger sum = this.unscaledAtScale(target).add(augend.unscaledAtScale(target));
        return new BigDecimal(sum, target);
    }

    public BigDecimal subtract(BigDecimal subtrahend) {
        return this.add(subtrahend.negate());
    }

    // Exact, always: the scales simply add, because 10^-a x 10^-b = 10^-(a+b).
    public BigDecimal multiply(BigDecimal multiplicand) {
        return new BigDecimal(this.unscaled.multiply(multiplicand.unscaled),
                this.scale + multiplicand.scale);
    }

    public BigDecimal negate() {
        return new BigDecimal(this.unscaled.negate(), this.scale);
    }

    public BigDecimal abs() {
        if (this.signum() < 0) {
            return this.negate();
        }
        return this;
    }

    public BigDecimal pow(int n) {
        if (n < 0 || n > 999999999) {
            throw new ArithmeticException("Invalid operation");
        }
        return new BigDecimal(this.unscaled.pow(n), this.scale * n);
    }

    // Divide to an explicit scale, rounding as told. This is the form that always works.
    public BigDecimal divide(BigDecimal divisor, int scale, RoundingMode roundingMode) {
        if (divisor.signum() == 0) {
            throw new ArithmeticException("Division by zero");
        }
        // this/divisor at the requested scale is
        //   (thisUnscaled x 10^(scale + divisorScale - thisScale)) / divisorUnscaled
        int shift = scale + divisor.scale - this.scale;
        BigInteger numerator = this.unscaled;
        BigInteger denominator = divisor.unscaled;
        if (shift >= 0) {
            numerator = numerator.multiply(BigDecimal.tenPow(shift));
        } else {
            denominator = denominator.multiply(BigDecimal.tenPow(-shift));
        }
        return new BigDecimal(BigDecimal.divideAndRound(numerator, denominator, roundingMode), scale);
    }

    public BigDecimal divide(BigDecimal divisor, RoundingMode roundingMode) {
        return this.divide(divisor, this.scale, roundingMode);
    }

    // The exact form: no rounding mode, so a non-terminating quotient is an ERROR rather than a
    // silent approximation. 1/8 is fine (0.125); 1/3 throws.
    public BigDecimal divide(BigDecimal divisor) {
        if (divisor.signum() == 0) {
            throw new ArithmeticException("Division by zero");
        }
        // A quotient terminates only if, after removing common factors, the denominator is a
        // product of 2s and 5s — the primes of base ten. Rather than factor, try increasing scales
        // up to the bound where a terminating expansion must have appeared.
        int maxScale = this.scale + divisor.precision() + 20;
        int trial = this.scale;
        BigDecimal result = null;
        while (trial <= maxScale && result == null) {
            BigDecimal candidate = this.divide(divisor, trial, RoundingMode.valueOf("DOWN"));
            if (candidate.multiply(divisor).compareTo(this) == 0) {
                result = candidate;
            }
            trial = trial + 1;
        }
        if (result == null) {
            throw new ArithmeticException("Non-terminating decimal expansion; "
                    + "no exact representable decimal result.");
        }
        return result;
    }

    // The rounding table. `label()` is RoundingMode's package-private name accessor: `toString()`
    // and `name()` are inherited from java.lang.Enum, and a call to a method inherited from an
    // external superclass is silently dropped by our compiler (finding #120). Reading
    // `RoundingMode.HALF_UP` directly would hit finding #110 instead, so neither route is open.
    private static BigInteger divideAndRound(BigInteger n, BigInteger d, RoundingMode mode) {
        BigInteger q = n.divide(d);
        BigInteger r = n.remainder(d);
        if (r.signum() == 0) {
            return q;
        }
        String m = mode.label();
        if (m.equals("UNNECESSARY")) {
            throw new ArithmeticException("Rounding necessary");
        }
        int resultSign = n.signum() * d.signum();
        boolean increment = false;
        if (m.equals("UP")) {
            increment = true;
        } else if (m.equals("DOWN")) {
            increment = false;
        } else if (m.equals("CEILING")) {
            increment = resultSign > 0;
        } else if (m.equals("FLOOR")) {
            increment = resultSign < 0;
        } else {
            // The three half-modes differ only on an exact tie, so compare 2|r| against |d| once.
            BigInteger twice = r.abs().multiply(BigInteger.valueOf(2L));
            int cmp = twice.compareTo(d.abs());
            if (cmp > 0) {
                increment = true;
            } else if (cmp == 0) {
                if (m.equals("HALF_UP")) {
                    increment = true;
                } else if (m.equals("HALF_EVEN")) {
                    // Round the tie to the even quotient: this is what keeps repeated rounding
                    // from drifting upward.
                    increment = q.testBit(0);
                }
            }
        }
        if (increment) {
            BigInteger one = BigInteger.valueOf(1L);
            if (resultSign < 0) {
                return q.subtract(one);
            }
            return q.add(one);
        }
        return q;
    }

    public BigDecimal setScale(int newScale, RoundingMode roundingMode) {
        if (newScale >= this.scale) {
            // Growing the scale is exact.
            return new BigDecimal(this.unscaledAtScale(newScale), newScale);
        }
        BigInteger divisor = BigDecimal.tenPow(this.scale - newScale);
        return new BigDecimal(BigDecimal.divideAndRound(this.unscaled, divisor, roundingMode), newScale);
    }

    public BigDecimal setScale(int newScale) {
        return this.setScale(newScale, RoundingMode.valueOf("UNNECESSARY"));
    }

    // Shifting the point is pure scale arithmetic — no digits move, no rounding happens.
    public BigDecimal movePointLeft(int n) {
        int newScale = this.scale + n;
        if (newScale < 0) {
            return new BigDecimal(this.unscaled.multiply(BigDecimal.tenPow(-newScale)), 0);
        }
        return new BigDecimal(this.unscaled, newScale);
    }

    public BigDecimal movePointRight(int n) {
        return this.movePointLeft(-n);
    }

    public BigDecimal stripTrailingZeros() {
        BigInteger value = this.unscaled;
        int s = this.scale;
        if (value.signum() == 0) {
            return new BigDecimal(value, 0);
        }
        // No guard on the sign of the scale: a trailing zero is removable whatever the scale is,
        // and the scale simply keeps decreasing — 4.0E+6 (unscaled 40, scale -5) strips to
        // 4E+6 (unscaled 4, scale -6). Stopping at scale 0 leaves the value unnormalized, which is
        // exactly the bug the JDK oracle caught here.
        BigInteger ten = BigInteger.valueOf(10L);
        boolean more = true;
        while (more) {
            if (value.remainder(ten).signum() == 0) {
                value = value.divide(ten);
                s = s - 1;
            } else {
                more = false;
            }
        }
        return new BigDecimal(value, s);
    }

    public BigInteger toBigInteger() {
        if (this.scale == 0) {
            return this.unscaled;
        }
        if (this.scale < 0) {
            return this.unscaled.multiply(BigDecimal.tenPow(-this.scale));
        }
        return this.unscaled.divide(BigDecimal.tenPow(this.scale));
    }

    public BigDecimal min(BigDecimal val) {
        if (this.compareTo(val) <= 0) {
            return this;
        }
        return val;
    }

    public BigDecimal max(BigDecimal val) {
        if (this.compareTo(val) >= 0) {
            return this;
        }
        return val;
    }

    // Compares VALUES: 1.0 and 1.00 compare equal. Contrast with equals() below.
    public int compareTo(BigDecimal val) {
        int target = this.scale;
        if (val.scale > target) {
            target = val.scale;
        }
        return this.unscaledAtScale(target).compareTo(val.unscaledAtScale(target));
    }

    // Compares REPRESENTATIONS: 1.0 and 1.00 are NOT equal, because their scales differ. This is
    // the JDK's contract, and it is why a BigDecimal must never be used as a HashMap key when what
    // you mean is numeric equality.
    public boolean equals(Object x) {
        if (this == x) {
            return true;
        }
        if (x instanceof BigDecimal) {
            BigDecimal other = (BigDecimal) x;
            return this.scale == other.scale && this.unscaled.equals(other.unscaled);
        }
        return false;
    }

    public int hashCode() {
        return this.unscaled.hashCode() * 31 + this.scale;
    }

    // The JDK's layout rules: plain notation while the value is "close to" its digits, scientific
    // once the point would be far away. The switch is on the ADJUSTED EXPONENT — the power of ten
    // of the leading digit — and the thresholds are exactly those in the JDK's specification.
    public String toString() {
        if (this.scale == 0) {
            return this.unscaled.toString();
        }
        String coeff = this.unscaled.abs().toString();
        String sign = "";
        if (this.unscaled.signum() < 0) {
            sign = "-";
        }
        long adjusted = (long) (-this.scale) + (long) (coeff.length() - 1);
        if (this.scale >= 0 && adjusted >= -6L) {
            return sign + BigDecimal.plainDigits(coeff, this.scale);
        }
        // Scientific: one digit, then the rest, then the exponent.
        StringBuilder buf = new StringBuilder();
        buf.append(sign);
        buf.append(coeff.charAt(0));
        if (coeff.length() > 1) {
            buf.append(".");
            buf.append(coeff.substring(1, coeff.length()));
        }
        buf.append("E");
        if (adjusted > 0L) {
            buf.append("+");
        }
        buf.append(Long.toString(adjusted));
        return buf.toString();
    }

    public String toPlainString() {
        if (this.scale == 0) {
            return this.unscaled.toString();
        }
        String coeff = this.unscaled.abs().toString();
        String sign = "";
        if (this.unscaled.signum() < 0) {
            sign = "-";
        }
        if (this.scale < 0) {
            // Zero is special-cased: 0 with a negative scale is still just "0", not a run of the
            // zeros the scale would otherwise spell out.
            if (this.unscaled.signum() == 0) {
                return "0";
            }
            StringBuilder buf = new StringBuilder();
            buf.append(sign);
            buf.append(coeff);
            int k = 0;
            while (k < -this.scale) {
                buf.append("0");
                k = k + 1;
            }
            return buf.toString();
        }
        return sign + BigDecimal.plainDigits(coeff, this.scale);
    }

    // Places the decimal point `scale` digits from the right, padding with leading zeros when the
    // point falls left of the first digit.
    private static String plainDigits(String coeff, int scale) {
        StringBuilder buf = new StringBuilder();
        int pointAt = coeff.length() - scale;
        if (pointAt <= 0) {
            buf.append("0.");
            int k = 0;
            while (k < -pointAt) {
                buf.append("0");
                k = k + 1;
            }
            buf.append(coeff);
        } else {
            buf.append(coeff.substring(0, pointAt));
            buf.append(".");
            buf.append(coeff.substring(pointAt, coeff.length()));
        }
        return buf.toString();
    }

    // ---- Number ----

    public int intValue() {
        return this.toBigInteger().intValue();
    }

    public long longValue() {
        return this.toBigInteger().longValue();
    }

    public float floatValue() {
        return (float) this.doubleValue();
    }

    public double doubleValue() {
        double u = this.unscaled.doubleValue();
        int s = this.scale;
        while (s > 0) {
            u = u / 10.0;
            s = s - 1;
        }
        while (s < 0) {
            u = u * 10.0;
            s = s + 1;
        }
        return u;
    }

    // ---- legacy int-rounding overloads ----

    public BigDecimal divide(BigDecimal divisor, int roundingMode) {
        return this.divide(divisor, this.scale, RoundingMode.valueOf(roundingMode));
    }

    public BigDecimal divide(BigDecimal divisor, int scale, int roundingMode) {
        return this.divide(divisor, scale, RoundingMode.valueOf(roundingMode));
    }

    public BigDecimal setScale(int newScale, int roundingMode) {
        return this.setScale(newScale, RoundingMode.valueOf(roundingMode));
    }

    // ---- exact narrowing ----

    /** @throws ArithmeticException if this has a nonzero fractional part. */
    public BigInteger toBigIntegerExact() {
        return this.setScale(0, RoundingMode.UNNECESSARY).toBigInteger();
    }

    public long longValueExact() {
        return this.toBigIntegerExact().longValueExact();
    }

    public int intValueExact() {
        return this.toBigIntegerExact().intValueExact();
    }

    public short shortValueExact() {
        return this.toBigIntegerExact().shortValueExact();
    }

    public byte byteValueExact() {
        return this.toBigIntegerExact().byteValueExact();
    }

    // ---- integer division / remainder ----

    /** The integer part of {@code this / divisor}, truncated toward zero. */
    public BigDecimal divideToIntegralValue(BigDecimal divisor) {
        return this.divide(divisor, 0, RoundingMode.DOWN);
    }

    /** {@code this - divideToIntegralValue(divisor) * divisor}. */
    public BigDecimal remainder(BigDecimal divisor) {
        return this.subtract(this.divideToIntegralValue(divisor).multiply(divisor));
    }

    /** {@return {divideToIntegralValue(divisor), remainder(divisor)}}. */
    public BigDecimal[] divideAndRemainder(BigDecimal divisor) {
        BigDecimal q = this.divideToIntegralValue(divisor);
        return new BigDecimal[] {q, this.subtract(q.multiply(divisor))};
    }

    // ---- sign / scale conveniences ----

    /** {@return this} — the unary plus. */
    public BigDecimal plus() {
        return this;
    }

    /** This value with its scale reduced by {@code n} (i.e. multiplied by 10^n). */
    public BigDecimal scaleByPowerOfTen(int n) {
        return new BigDecimal(this.unscaled, this.scale - n);
    }

    /** The size of an ulp of this: {@code 1} at this value's scale (10^-scale). */
    public BigDecimal ulp() {
        return new BigDecimal(BigInteger.valueOf(1L), this.scale);
    }

    // ---- MathContext-aware operations ----

    /** Rounds this to {@code mc}'s precision (a no-op when precision is 0). */
    public BigDecimal round(MathContext mc) {
        int p = mc.getPrecision();
        if (p == 0) {
            return this;
        }
        int drop = this.precision() - p;
        if (drop <= 0) {
            return this;
        }
        return this.setScale(this.scale - drop, mc.getRoundingMode());
    }

    public BigDecimal add(BigDecimal augend, MathContext mc) {
        return this.add(augend).round(mc);
    }

    public BigDecimal subtract(BigDecimal subtrahend, MathContext mc) {
        return this.subtract(subtrahend).round(mc);
    }

    public BigDecimal multiply(BigDecimal multiplicand, MathContext mc) {
        return this.multiply(multiplicand).round(mc);
    }

    public BigDecimal negate(MathContext mc) {
        return this.negate().round(mc);
    }

    public BigDecimal abs(MathContext mc) {
        return this.abs().round(mc);
    }

    public BigDecimal plus(MathContext mc) {
        return this.round(mc);
    }

    public BigDecimal pow(int n, MathContext mc) {
        return this.pow(n).round(mc);
    }

    /** Division rounded to {@code mc}'s precision (exact division when precision is 0). */
    public BigDecimal divide(BigDecimal divisor, MathContext mc) {
        int p = mc.getPrecision();
        if (p == 0) {
            return this.divide(divisor);
        }
        // A generous working scale keeps far more than p significant digits (truncating, which is
        // exact for the digits kept), then round back to p with mc's mode.
        int interScale = p + divisor.precision() + 8;
        BigDecimal q = this.divide(divisor, interScale, RoundingMode.DOWN);
        return q.round(mc);
    }

    public BigDecimal divideToIntegralValue(BigDecimal divisor, MathContext mc) {
        BigDecimal q = this.divideToIntegralValue(divisor);
        if (mc.getPrecision() != 0 && q.precision() > mc.getPrecision()) {
            throw new ArithmeticException("Division impossible");
        }
        return q;
    }

    public BigDecimal remainder(BigDecimal divisor, MathContext mc) {
        return this.subtract(this.divideToIntegralValue(divisor, mc).multiply(divisor));
    }

    public BigDecimal[] divideAndRemainder(BigDecimal divisor, MathContext mc) {
        BigDecimal q = this.divideToIntegralValue(divisor, mc);
        return new BigDecimal[] {q, this.subtract(q.multiply(divisor))};
    }

    /** The square root of this, rounded to {@code mc}'s precision. */
    public BigDecimal sqrt(MathContext mc) {
        if (this.signum() < 0) {
            throw new ArithmeticException("Attempted square root of negative BigDecimal");
        }
        if (this.signum() == 0) {
            return this;
        }
        int p = mc.getPrecision();
        if (p == 0) {
            p = this.precision() + 1;
        }
        int workScale = p + 6;
        // Newton's method at a fixed working scale: x <- (x + this/x) / 2.
        BigDecimal two = new BigDecimal(BigInteger.valueOf(2L), 0);
        BigDecimal x = new BigDecimal(this.doubleValue());
        if (x.signum() == 0) {
            x = ONE;
        }
        int iters = 0;
        BigDecimal prev = null;
        while (iters < 200) {
            BigDecimal q = this.divide(x, workScale, RoundingMode.HALF_EVEN);
            BigDecimal next = x.add(q).divide(two, workScale, RoundingMode.HALF_EVEN);
            if (prev != null && next.compareTo(x) == 0) {
                break;
            }
            prev = x;
            x = next;
            iters = iters + 1;
        }
        MathContext out = (mc.getPrecision() == 0) ? new MathContext(this.precision() + 1) : mc;
        return x.round(out);
    }

    /** This value in engineering notation (exponents are multiples of three). */
    public String toEngineeringString() {
        if (this.signum() == 0) {
            return "0";
        }
        String digits = this.unscaled.abs().toString();
        String sign = this.signum() < 0 ? "-" : "";
        // adjusted exponent of the most significant digit
        int adj = (digits.length() - 1) - this.scale;
        if (this.scale >= 0 && adj >= -6) {
            // plain form
            return this.toPlainString();
        }
        // engineering: integer part has 1..3 digits, exponent a multiple of 3
        int mod = adj % 3;
        if (mod < 0) {
            mod = mod + 3;
        }
        int intDigits = mod + 1;
        int exp = adj - mod;
        StringBuilder sb = new StringBuilder(sign);
        if (digits.length() <= intDigits) {
            sb.append(digits);
            int pad = intDigits - digits.length();
            int i = 0;
            while (i < pad) {
                sb.append('0');
                i = i + 1;
            }
        } else {
            sb.append(digits.substring(0, intDigits));
            sb.append('.');
            sb.append(digits.substring(intDigits));
        }
        if (exp != 0) {
            sb.append('E');
            if (exp > 0) {
                sb.append('+');
            }
            sb.append(Integer.toString(exp));
        }
        return sb.toString();
    }

    // ---- char[] and MathContext constructors ----

    public BigDecimal(char[] in) {
        this(new String(in));
    }

    public BigDecimal(char[] in, int offset, int len) {
        this(new String(in, offset, len));
    }

    public BigDecimal(char[] in, MathContext mc) {
        this(new String(in), mc);
    }

    public BigDecimal(char[] in, int offset, int len, MathContext mc) {
        this(new String(in, offset, len), mc);
    }

    public BigDecimal(String val, MathContext mc) {
        this(round0(new BigDecimal(val), mc));
    }

    public BigDecimal(double val, MathContext mc) {
        this(round0(new BigDecimal(val), mc));
    }

    public BigDecimal(int val, MathContext mc) {
        this(round0(new BigDecimal(val), mc));
    }

    public BigDecimal(long val, MathContext mc) {
        this(round0(new BigDecimal(val), mc));
    }

    public BigDecimal(BigInteger val, MathContext mc) {
        this(round0(new BigDecimal(val), mc));
    }

    public BigDecimal(BigInteger unscaledVal, int scaleVal, MathContext mc) {
        this(round0(new BigDecimal(unscaledVal, scaleVal), mc));
    }

    // Copy constructor + a rounding helper, so the MathContext ctors can delegate through this(...).
    private BigDecimal(BigDecimal other) {
        this.unscaled = other.unscaled;
        this.scale = other.scale;
    }

    private static BigDecimal round0(BigDecimal v, MathContext mc) {
        return v.round(mc);
    }
}
