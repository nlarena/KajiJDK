package java.math;

// KajiLibrary's java.math.BigInteger — integers with no upper bound, which is what you need the
// moment a result stops fitting in 64 bits.
//
// REPRESENTATION: sign-magnitude. A separate `signum` (-1, 0, +1) plus `mag`, the magnitude as an
// int[] in BIG-endian order (most significant word first) with no leading zeros; zero has an empty
// magnitude. This is the JDK's layout too, and the reason for it is that two's complement has no
// natural variable-length form — the sign would have to be re-extended on every resize, and every
// operation would have to agree on how many leading sign words are implied.
//
// Each int holds 32 bits treated as UNSIGNED. Java has no unsigned int, so every word is widened
// to long and masked with 0xFFFFFFFFL before arithmetic; the carry then falls out of the high half.
// Getting that mask wrong is the classic way to write a big-integer library that is right for
// small values and silently wrong past 2^31.
//
// DIVISION is shift-and-subtract (binary long division): O(bits x words) instead of the O(words^2)
// of Knuth's Algorithm D. That is the deliberate trade — Algorithm D needs a normalization step and
// a quotient-digit correction loop that are notoriously easy to get subtly wrong, and at the sizes
// this library is used for (BigDecimal with tens of digits) the difference is not observable. The
// algorithm here is short enough to check by reading.
//
// A KajiLibrary subset: the number-theory half (modPow, modInverse, isProbablePrime,
// nextProbablePrime), the bitwise operations (and/or/xor/not), and the byte[] constructors are
// omitted. `Comparable` is not implemented — `compareTo(BigInteger)` is declared directly, which
// gives the same descriptor without needing a bridge.
public final class BigInteger extends Number {

    public static final BigInteger ZERO = new BigInteger(0, new int[0]);

    public static final BigInteger ONE = new BigInteger(1, new int[] {1});

    public static final BigInteger TWO = new BigInteger(1, new int[] {2});

    public static final BigInteger TEN = new BigInteger(1, new int[] {10});

    private final int signum;
    private final int[] mag;

    private BigInteger(int signum, int[] mag) {
        this.signum = signum;
        this.mag = mag;
    }

    public static BigInteger valueOf(long val) {
        int sign = 1;
        long v = val;
        if (val < 0L) {
            sign = -1;
            // Negating Long.MIN_VALUE overflows back to itself, so the magnitude is built from the
            // unsigned bit pattern rather than from -val.
            v = -val;
        } else if (val == 0L) {
            sign = 0;
        }
        int hi = (int) (v >>> 32);
        int lo = (int) v;
        int[] m;
        if (val == 0L) {
            m = new int[0];
        } else if (hi == 0) {
            m = new int[] {lo};
        } else {
            m = new int[] {hi, lo};
        }
        return new BigInteger(sign, m);
    }

    // Base diez, con signo opcional. Es `BigInteger(val, 10)`.
    public BigInteger(String val) {
        this(val, 10);
    }

    /**
     * El entero escrito en la base dada, con signo opcional.
     *
     * <p>Faltaba, y con el faltaba tambien `Scanner.nextBigInteger(radix)`, que es su unico usuario
     * evidente. El cuerpo es el mismo Horner de siempre --`acc = acc * base + digito`-- con la base
     * como parametro en vez de fija en diez: la version decimal ahora delega aca.
     */
    public BigInteger(String val, int radix) {
        int start = 0;
        int sign = 1;
        if (val.length() == 0) {
            throw new NumberFormatException("Zero length BigInteger");
        }
        char first = val.charAt(0);
        if (first == '-') {
            sign = -1;
            start = 1;
        } else if (first == '+') {
            start = 1;
        }
        if (start >= val.length()) {
            throw new NumberFormatException("Zero length BigInteger");
        }
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Radix out of range");
        }
        // Horner sobre la magnitud: acc = acc * radix + digito.
        int[] acc = new int[0];
        int i = start;
        while (i < val.length()) {
            int d = Character.digit(val.charAt(i), radix);
            if (d < 0) {
                throw new NumberFormatException("Illegal digit");
            }
            acc = BigInteger.mulAddSmall(acc, radix, d);
            i = i + 1;
        }
        acc = BigInteger.strip(acc);
        if (acc.length == 0) {
            sign = 0;
        }
        this.signum = sign;
        this.mag = acc;
    }

    // ---- magnitude helpers (all operate on unsigned big-endian int[]) ----

    private static int[] strip(int[] m) {
        int zeros = 0;
        while (zeros < m.length && m[zeros] == 0) {
            zeros = zeros + 1;
        }
        if (zeros == 0) {
            return m;
        }
        int[] out = new int[m.length - zeros];
        int i = 0;
        while (i < out.length) {
            out[i] = m[zeros + i];
            i = i + 1;
        }
        return out;
    }

    // acc * factor + addend, in place of a general multiply for the decimal parser.
    private static int[] mulAddSmall(int[] acc, int factor, int addend) {
        int[] out = new int[acc.length + 1];
        long carry = (long) addend;
        int i = acc.length - 1;
        int o = out.length - 1;
        while (i >= 0) {
            long product = (acc[i] & 0xFFFFFFFFL) * (long) factor + carry;
            out[o] = (int) product;
            carry = product >>> 32;
            i = i - 1;
            o = o - 1;
        }
        out[o] = (int) carry;
        return BigInteger.strip(out);
    }

    private static int compareMag(int[] a, int[] b) {
        if (a.length != b.length) {
            if (a.length < b.length) {
                return -1;
            }
            return 1;
        }
        int i = 0;
        int result = 0;
        while (i < a.length) {
            long x = a[i] & 0xFFFFFFFFL;
            long y = b[i] & 0xFFFFFFFFL;
            if (x != y) {
                if (x < y) {
                    result = -1;
                } else {
                    result = 1;
                }
                i = a.length;
            } else {
                i = i + 1;
            }
        }
        return result;
    }

    private static int[] addMag(int[] a, int[] b) {
        int[] big = a;
        int[] small = b;
        if (a.length < b.length) {
            big = b;
            small = a;
        }
        int[] out = new int[big.length + 1];
        long carry = 0L;
        int i = big.length - 1;
        int j = small.length - 1;
        int o = out.length - 1;
        while (i >= 0) {
            long sum = (big[i] & 0xFFFFFFFFL) + carry;
            if (j >= 0) {
                sum = sum + (small[j] & 0xFFFFFFFFL);
            }
            out[o] = (int) sum;
            carry = sum >>> 32;
            i = i - 1;
            j = j - 1;
            o = o - 1;
        }
        out[o] = (int) carry;
        return BigInteger.strip(out);
    }

    // a - b, requiring a >= b.
    private static int[] subMag(int[] a, int[] b) {
        int[] out = new int[a.length];
        long borrow = 0L;
        int i = a.length - 1;
        int j = b.length - 1;
        while (i >= 0) {
            long diff = (a[i] & 0xFFFFFFFFL) - borrow;
            if (j >= 0) {
                diff = diff - (b[j] & 0xFFFFFFFFL);
            }
            if (diff < 0L) {
                diff = diff + 4294967296L;
                borrow = 1L;
            } else {
                borrow = 0L;
            }
            out[i] = (int) diff;
            i = i - 1;
            j = j - 1;
        }
        return BigInteger.strip(out);
    }

    private static int[] mulMag(int[] a, int[] b) {
        if (a.length == 0 || b.length == 0) {
            return new int[0];
        }
        int[] out = new int[a.length + b.length];
        int i = a.length - 1;
        while (i >= 0) {
            long carry = 0L;
            long av = a[i] & 0xFFFFFFFFL;
            int j = b.length - 1;
            while (j >= 0) {
                int at = i + j + 1;
                long product = av * (b[j] & 0xFFFFFFFFL) + (out[at] & 0xFFFFFFFFL) + carry;
                out[at] = (int) product;
                carry = product >>> 32;
                j = j - 1;
            }
            out[i] = (int) carry;
            i = i - 1;
        }
        return BigInteger.strip(out);
    }

    private static int bitLengthMag(int[] m) {
        if (m.length == 0) {
            return 0;
        }
        int top = m[0];
        int bits = 0;
        while (top != 0) {
            bits = bits + 1;
            top = top >>> 1;
        }
        return bits + (m.length - 1) * 32;
    }

    private static boolean testBitMag(int[] m, int n) {
        int word = m.length - 1 - (n / 32);
        if (word < 0) {
            return false;
        }
        return ((m[word] >>> (n % 32)) & 1) != 0;
    }

    private static int[] shiftLeftMag(int[] m, int n) {
        if (m.length == 0 || n == 0) {
            return m;
        }
        int words = n / 32;
        int bits = n % 32;
        // The output is `words` longer, which is what carries the whole-word part of the shift:
        // source word i lands at out[i+1] regardless of `words`, because growing the array already
        // pushed every word toward the significant end. Both halves OR in — out[i+1] receives the
        // low half of word i and the high half of word i+1.
        int[] out = new int[m.length + words + 1];
        int i = 0;
        while (i < m.length) {
            long v = (m[i] & 0xFFFFFFFFL) << bits;
            out[i] = out[i] | (int) (v >>> 32);
            out[i + 1] = out[i + 1] | (int) v;
            i = i + 1;
        }
        return BigInteger.strip(out);
    }

    private static int[] shiftRightMag(int[] m, int n) {
        int len = BigInteger.bitLengthMag(m);
        if (n >= len) {
            return new int[0];
        }
        int outBits = len - n;
        int outWords = (outBits + 31) / 32;
        int[] out = new int[outWords];
        int bit = 0;
        while (bit < outBits) {
            if (BigInteger.testBitMag(m, bit + n)) {
                int word = out.length - 1 - (bit / 32);
                out[word] = out[word] | (1 << (bit % 32));
            }
            bit = bit + 1;
        }
        return BigInteger.strip(out);
    }

    // Shift-and-subtract division. `wantRemainder` picks which half of the result to return, so
    // the one loop serves both divide() and remainder().
    private static int[] divMag(int[] a, int[] b, boolean wantRemainder) {
        if (b.length == 0) {
            throw new ArithmeticException("BigInteger divide by zero");
        }
        if (BigInteger.compareMag(a, b) < 0) {
            if (wantRemainder) {
                return a;
            }
            return new int[0];
        }
        int aBits = BigInteger.bitLengthMag(a);
        int qWords = (aBits + 31) / 32;
        int[] quotient = new int[qWords];
        int[] rem = new int[0];
        int bit = aBits - 1;
        while (bit >= 0) {
            // rem = rem*2 + next bit of a
            rem = BigInteger.shiftLeftMag(rem, 1);
            if (BigInteger.testBitMag(a, bit)) {
                rem = BigInteger.addMag(rem, new int[] {1});
            }
            if (BigInteger.compareMag(rem, b) >= 0) {
                rem = BigInteger.subMag(rem, b);
                int word = quotient.length - 1 - (bit / 32);
                quotient[word] = quotient[word] | (1 << (bit % 32));
            }
            bit = bit - 1;
        }
        if (wantRemainder) {
            return rem;
        }
        return BigInteger.strip(quotient);
    }

    private static BigInteger make(int sign, int[] mag) {
        int[] m = BigInteger.strip(mag);
        int s = sign;
        if (m.length == 0) {
            s = 0;
        }
        return new BigInteger(s, m);
    }

    // ---- arithmetic ----

    public BigInteger add(BigInteger val) {
        if (this.signum == 0) {
            return val;
        }
        if (val.signum == 0) {
            return this;
        }
        if (this.signum == val.signum) {
            return BigInteger.make(this.signum, BigInteger.addMag(this.mag, val.mag));
        }
        int cmp = BigInteger.compareMag(this.mag, val.mag);
        if (cmp == 0) {
            return BigInteger.valueOf(0L);
        }
        if (cmp > 0) {
            return BigInteger.make(this.signum, BigInteger.subMag(this.mag, val.mag));
        }
        return BigInteger.make(val.signum, BigInteger.subMag(val.mag, this.mag));
    }

    public BigInteger subtract(BigInteger val) {
        return this.add(val.negate());
    }

    public BigInteger multiply(BigInteger val) {
        if (this.signum == 0 || val.signum == 0) {
            return BigInteger.valueOf(0L);
        }
        return BigInteger.make(this.signum * val.signum, BigInteger.mulMag(this.mag, val.mag));
    }

    // Truncating division: the quotient rounds TOWARD ZERO, so the remainder takes the dividend's
    // sign. That is Java's `/` on ints, and it is what `mod` below has to correct for.
    public BigInteger divide(BigInteger val) {
        if (val.signum == 0) {
            throw new ArithmeticException("BigInteger divide by zero");
        }
        if (this.signum == 0) {
            return BigInteger.valueOf(0L);
        }
        return BigInteger.make(this.signum * val.signum, BigInteger.divMag(this.mag, val.mag, false));
    }

    public BigInteger remainder(BigInteger val) {
        if (val.signum == 0) {
            throw new ArithmeticException("BigInteger divide by zero");
        }
        if (this.signum == 0) {
            return BigInteger.valueOf(0L);
        }
        return BigInteger.make(this.signum, BigInteger.divMag(this.mag, val.mag, true));
    }

    // Unlike remainder(), mod() is ALWAYS non-negative — the mathematician's modulo, not the
    // hardware's. (-7).remainder(3) is -1; (-7).mod(3) is 2.
    public BigInteger mod(BigInteger m) {
        if (m.signum <= 0) {
            throw new ArithmeticException("BigInteger: modulus not positive");
        }
        BigInteger r = this.remainder(m);
        if (r.signum < 0) {
            return r.add(m);
        }
        return r;
    }

    public BigInteger pow(int exponent) {
        if (exponent < 0) {
            throw new ArithmeticException("Negative exponent");
        }
        // Square-and-multiply: exponent bits, not exponent multiplications.
        BigInteger result = BigInteger.valueOf(1L);
        BigInteger base = this;
        int e = exponent;
        while (e > 0) {
            if ((e & 1) != 0) {
                result = result.multiply(base);
            }
            base = base.multiply(base);
            e = e >>> 1;
        }
        return result;
    }

    public BigInteger negate() {
        return new BigInteger(-this.signum, this.mag);
    }

    public BigInteger abs() {
        if (this.signum < 0) {
            return this.negate();
        }
        return this;
    }

    // Euclid, on magnitudes: gcd is sign-independent.
    public BigInteger gcd(BigInteger val) {
        BigInteger a = this.abs();
        BigInteger b = val.abs();
        while (b.signum != 0) {
            BigInteger t = a.remainder(b);
            a = b;
            b = t;
        }
        return a;
    }

    public BigInteger min(BigInteger val) {
        if (this.compareTo(val) <= 0) {
            return this;
        }
        return val;
    }

    public BigInteger max(BigInteger val) {
        if (this.compareTo(val) >= 0) {
            return this;
        }
        return val;
    }

    public int signum() {
        return this.signum;
    }

    public int bitLength() {
        return BigInteger.bitLengthMag(this.mag);
    }

    public boolean testBit(int n) {
        if (n < 0) {
            throw new ArithmeticException("Negative bit address");
        }
        return BigInteger.testBitMag(this.mag, n);
    }

    public BigInteger shiftLeft(int n) {
        if (n < 0) {
            return this.shiftRight(-n);
        }
        return BigInteger.make(this.signum, BigInteger.shiftLeftMag(this.mag, n));
    }

    public BigInteger shiftRight(int n) {
        if (n < 0) {
            return this.shiftLeft(-n);
        }
        return BigInteger.make(this.signum, BigInteger.shiftRightMag(this.mag, n));
    }

    public int compareTo(BigInteger val) {
        if (this.signum != val.signum) {
            if (this.signum < val.signum) {
                return -1;
            }
            return 1;
        }
        int cmp = BigInteger.compareMag(this.mag, val.mag);
        if (this.signum < 0) {
            return -cmp;
        }
        return cmp;
    }

    public boolean equals(Object x) {
        if (this == x) {
            return true;
        }
        if (x instanceof BigInteger) {
            BigInteger other = (BigInteger) x;
            return this.compareTo(other) == 0;
        }
        return false;
    }

    public int hashCode() {
        int hash = 0;
        int i = 0;
        while (i < this.mag.length) {
            hash = hash * 31 + this.mag[i];
            i = i + 1;
        }
        return hash * this.signum;
    }

    // Repeated division by 1e9, which yields nine decimal digits per pass instead of one.
    //
    // The groups come out least-significant first and are collected into an array, then walked
    // backwards: KajiLibrary's StringBuilder has no `insert`, and prepending by rebuilding the
    // string each pass would make this quadratic in the number of groups for no reason.
    public String toString() {
        if (this.signum == 0) {
            return "0";
        }
        // Nine decimal digits need just under 30 bits, so this is a safe upper bound on groups.
        String[] groups = new String[this.bitLength() / 29 + 2];
        int count = 0;
        int[] rest = this.mag;
        int[] billion = new int[] {1000000000};
        while (rest.length > 0) {
            int[] q = BigInteger.divMag(rest, billion, false);
            int[] r = BigInteger.divMag(rest, billion, true);
            long chunk = 0L;
            if (r.length > 0) {
                chunk = r[r.length - 1] & 0xFFFFFFFFL;
            }
            groups[count] = Long.toString(chunk);
            count = count + 1;
            rest = q;
        }
        StringBuilder out = new StringBuilder();
        if (this.signum < 0) {
            out.append("-");
        }
        int i = count - 1;
        while (i >= 0) {
            String piece = groups[i];
            // The leading group keeps its natural width; every interior one is exactly nine
            // digits, so its leading zeros are significant and have to be written back.
            if (i < count - 1) {
                int pad = 9 - piece.length();
                int k = 0;
                while (k < pad) {
                    out.append("0");
                    k = k + 1;
                }
            }
            out.append(piece);
            i = i - 1;
        }
        return out.toString();
    }

    // ---- Number ----

    public int intValue() {
        return (int) this.longValue();
    }

    public long longValue() {
        long v = 0L;
        if (this.mag.length > 0) {
            v = this.mag[this.mag.length - 1] & 0xFFFFFFFFL;
        }
        if (this.mag.length > 1) {
            v = v | ((this.mag[this.mag.length - 2] & 0xFFFFFFFFL) << 32);
        }
        if (this.signum < 0) {
            return -v;
        }
        return v;
    }

    public float floatValue() {
        return (float) this.doubleValue();
    }

    public double doubleValue() {
        double d = 0.0;
        int i = 0;
        while (i < this.mag.length) {
            d = d * 4294967296.0 + (double) (this.mag[i] & 0xFFFFFFFFL);
            i = i + 1;
        }
        if (this.signum < 0) {
            return -d;
        }
        return d;
    }
}
