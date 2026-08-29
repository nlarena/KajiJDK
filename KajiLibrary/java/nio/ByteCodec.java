package java.nio;

/**
 * The byte-level arithmetic every multi-byte accessor in this package needs, in one place.
 *
 * <p>Two jobs live here. The first is trivial and is the whole reason {@link ByteOrder} exists:
 * assembling <em>n</em> bytes into an integer, or taking one apart, in one direction or the
 * other. The second is not trivial at all — turning a {@code float} or a {@code double} into its
 * IEEE-754 bit pattern and back.
 *
 * <p><strong>Why that is done by hand.</strong> Reinterpreting a floating-point value as an
 * integer is not something bytecode can express: there is no instruction for it, which is exactly
 * why the JDK declares {@code Float.floatToIntBits} and {@code Double.doubleToLongBits}
 * {@code native}. KajiLibrary declares {@code Double.doubleToLongBits} too, but the VM has no
 * implementation bound to it ("no native implementation for
 * java/lang/Double.doubleToLongBits(D)J"), and the {@code Float} pair does not exist at all. So
 * the conversion is done with ordinary arithmetic: scale the value by powers of two until it is
 * in {@code [1, 2)}, and the exponent is the count of scalings while the mantissa is what is
 * left. Every step is a multiplication by a power of two, which is exact in IEEE-754 all the way
 * down into the subnormals, so the round trip is bit-exact — it was checked value by value
 * against the reference JDK, including ±0, ±∞, NaN, and subnormal floats and doubles.
 *
 * <p>The cost is a loop proportional to the magnitude of the exponent: about 52 iterations for
 * an everyday value, up to about 1074 for the smallest subnormal. That is slow, and it is the
 * price of not having the one instruction that would make it free.
 *
 * <p>This class has no counterpart in {@code java.base}; it is ours, like {@code HeapByteBuffer}.
 */
final class ByteCodec {

    private ByteCodec() {
    }

    /**
     * Assembles {@code count} bytes of {@code a} starting at {@code at} into a long.
     *
     * @param a the array holding the bytes
     * @param at the index of the first byte
     * @param count how many bytes to read, 1 to 8
     * @param bigEndian whether the most significant byte comes first
     * @return the assembled value, zero-extended
     */
    static long read(byte[] a, int at, int count, boolean bigEndian) {
        // El sufijo L no es cosmetico: nuestro javac no emite `i2l` en la conversion de
        // asignacion, asi que `long value = 0;` deja un int en el slot y la primera operacion
        // larga revienta ("expected a long on the operand stack, found Int(0)").
        long value = 0L;
        int i = 0;
        while (i < count) {
            int b = a[at + i] & 0xff;
            if (bigEndian) {
                value = (value << 8) | (long) b;
            } else {
                value = value | ((long) b << (8 * i));
            }
            i = i + 1;
        }
        return value;
    }

    /**
     * Takes {@code value} apart into {@code count} bytes of {@code a} starting at {@code at}.
     *
     * @param a the array to write into
     * @param at the index of the first byte
     * @param count how many bytes to write, 1 to 8
     * @param bigEndian whether the most significant byte comes first
     * @param value the value to store
     */
    static void write(byte[] a, int at, int count, boolean bigEndian, long value) {
        int i = 0;
        while (i < count) {
            int shift = 8 * i;
            if (bigEndian) {
                shift = 8 * (count - 1 - i);
            }
            a[at + i] = (byte) ((value >> shift) & 0xff);
            i = i + 1;
        }
    }

    /**
     * Two raised to {@code e}, exactly, for every exponent a double can reach.
     *
     * <p>Repeated multiplication rather than {@code Math.pow}: each step doubles or halves an
     * exact power of two, which is exact in IEEE-754 right down to the smallest subnormal, so
     * the result carries no rounding error to leak into the conversions below.
     */
    static double pow2(int e) {
        double r = 1.0;
        if (e >= 0) {
            int i = 0;
            while (i < e) {
                r = r * 2.0;
                i = i + 1;
            }
        } else {
            int i = 0;
            while (i > e) {
                r = r * 0.5;
                i = i - 1;
            }
        }
        return r;
    }

    /**
     * The IEEE-754 double bit pattern of {@code v}, as {@code Double.doubleToLongBits} would
     * return it.
     *
     * <p>NaN is canonicalised to {@code 0x7ff8000000000000}, which is what the JDK's
     * {@code doubleToLongBits} (as opposed to {@code doubleToRawLongBits}) also does.
     *
     * @param v the value to encode
     * @return its 64 bits
     */
    static long doubleToBits(double v) {
        if (v != v) {
            return 0x7ff8000000000000L;
        }
        long sign = 0L;
        if (v < 0.0) {
            sign = 0x8000000000000000L;
            v = -v;
        } else if (v == 0.0 && 1.0 / v < 0.0) {
            // negative zero: it compares equal to +0.0, so the only way to see it is the sign
            // of its reciprocal.
            return 0x8000000000000000L;
        }
        if (v == 0.0) {
            return sign;
        }
        if (v - v != 0.0) {
            return sign | 0x7ff0000000000000L;
        }
        int e = 0;
        while (v >= 2.0) {
            v = v * 0.5;
            e = e + 1;
        }
        while (v < 1.0) {
            v = v * 2.0;
            e = e - 1;
        }
        if (e >= -1022) {
            long mant = (long) ((v - 1.0) * pow2(52));
            return sign | (((long) (e + 1023)) << 52) | mant;
        }
        // subnormal: no implicit leading one, and the exponent field is zero
        return sign | (long) (v * pow2(e + 1074));
    }

    /**
     * The double with the given bit pattern, as {@code Double.longBitsToDouble} would return it.
     *
     * @param bits the 64 bits to decode
     * @return the value they denote
     */
    static double bitsToDouble(long bits) {
        int exp = (int) ((bits >> 52) & 0x7ffL);
        long mant = bits & 0x000fffffffffffffL;
        boolean negative = (bits & 0x8000000000000000L) != 0L;
        double r;
        if (exp == 0x7ff) {
            if (mant != 0L) {
                return 0.0 / 0.0;
            }
            r = 1.0 / 0.0;
        } else if (exp == 0) {
            r = (double) mant * pow2(-1074);
        } else {
            r = (double) (mant | 0x0010000000000000L) * pow2(exp - 1075);
        }
        if (negative) {
            return -r;
        }
        return r;
    }

    /**
     * The IEEE-754 float bit pattern of {@code f}, as {@code Float.floatToIntBits} would return
     * it.
     *
     * <p>Goes through the double encoding: widening a float to a double is exact, so the double
     * bits carry the float's value without loss and only have to be re-laid-out — the mantissa
     * shifted 29 places and the exponent rebiased. A float subnormal widens to a normal double,
     * which is the {@code fexp <= 0} branch.
     *
     * @param f the value to encode
     * @return its 32 bits
     */
    static int floatToBits(float f) {
        long db = doubleToBits((double) f);
        int sign = 0;
        if ((db & 0x8000000000000000L) != 0L) {
            sign = 0x80000000;
        }
        int dexp = (int) ((db >> 52) & 0x7ffL);
        long dmant = db & 0x000fffffffffffffL;
        if (dexp == 0x7ff) {
            if (dmant == 0L) {
                return sign | 0x7f800000;
            }
            return 0x7fc00000;
        }
        if (dexp == 0) {
            return sign;
        }
        int fexp = dexp - 1023 + 127;
        if (fexp >= 255) {
            return sign | 0x7f800000;
        }
        if (fexp <= 0) {
            long full = 0x0010000000000000L | dmant;
            int shift = 29 + (1 - fexp);
            if (shift >= 63) {
                return sign;
            }
            return sign | (int) (full >>> shift);
        }
        return sign | (fexp << 23) | (int) (dmant >>> 29);
    }

    /**
     * The float with the given bit pattern, as {@code Float.intBitsToFloat} would return it.
     *
     * @param bits the 32 bits to decode
     * @return the value they denote
     */
    static float bitsToFloat(int bits) {
        int exp = (bits >> 23) & 0xff;
        int mant = bits & 0x7fffff;
        boolean negative = (bits & 0x80000000) != 0;
        double r;
        if (exp == 0xff) {
            if (mant != 0) {
                return (float) (0.0 / 0.0);
            }
            r = 1.0 / 0.0;
        } else if (exp == 0) {
            r = (double) mant * pow2(-149);
        } else {
            r = (double) (mant | 0x800000) * pow2(exp - 150);
        }
        if (negative) {
            return (float) (-r);
        }
        return (float) r;
    }
}
