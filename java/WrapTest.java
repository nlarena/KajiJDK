/**
 * Exercises the parse, format and bit families of {@code Integer} and {@code Long}, and the
 * classification and ordering of {@code Double} and {@code Float}. Every method returns the
 * number of things that came out wrong, so 0 is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 *
 * <p>Two styles of check are mixed deliberately. Fixed values pin the cases where the answer is
 * surprising -- the extremes, the unsigned readings, the radix boundaries. Sweeps check
 * relationships that must hold everywhere: text that round-trips through every radix, bit
 * operations that undo each other, unsigned division whose quotient and remainder rebuild the
 * dividend. A relationship that holds over a grid says something a table of examples cannot.
 */
public class WrapTest {

    /** Signed text, out and back, in every radix. */
    public static int textoEntero() {
        int bad = 0;
        if (!Integer.toString(255, 16).equals("ff") || !Integer.toString(-255, 16).equals("-ff")) {
            bad = bad + 1;
        }
        if (!Integer.toString(255, 2).equals("11111111")) {
            bad = bad + 1;
        }
        // A radix outside 2..36 is silently taken as ten, which is worth pinning because it
        // hides a mistake rather than reporting it.
        if (!Integer.toString(255, 1).equals("255") || !Integer.toString(255, 99).equals("255")) {
            bad = bad + 1;
        }
        if (!Integer.toString(Integer.MIN_VALUE, 16).equals("-80000000")) {
            bad = bad + 1;
        }
        if (Integer.parseInt("ff", 16) != 255 || Integer.parseInt("-ff", 16) != -255) {
            bad = bad + 1;
        }
        if (Integer.parseInt("0") != 0 || Integer.parseInt("+7") != 7) {
            bad = bad + 1;
        }
        if (Integer.parseInt("2147483647") != Integer.MAX_VALUE) {
            bad = bad + 1;
        }
        // The asymmetric end, which a magnitude-then-negate parser cannot reach.
        if (Integer.parseInt("-2147483648") != Integer.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Long.parseLong("-9223372036854775808") != Long.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Long.parseLong("9223372036854775807") != Long.MAX_VALUE) {
            bad = bad + 1;
        }
        // The sub-range form.
        if (Integer.parseInt("xx123yy", 2, 5, 10) != 123) {
            bad = bad + 1;
        }
        // Every radix, both signs, out and back.
        int radix = 2;
        while (radix <= 36) {
            int v = -1000000;
            while (v <= 1000000) {
                String text = Integer.toString(v, radix);
                if (Integer.parseInt(text, radix) != v) {
                    bad = bad + 1;
                }
                long w = (long) v * 1000000L;
                String wide = Long.toString(w, radix);
                if (Long.parseLong(wide, radix) != w) {
                    bad = bad + 1;
                }
                v = v + 333333;
            }
            if (Integer.parseInt(Integer.toString(Integer.MIN_VALUE, radix), radix)
                    != Integer.MIN_VALUE) {
                bad = bad + 1;
            }
            if (Long.parseLong(Long.toString(Long.MIN_VALUE, radix), radix) != Long.MIN_VALUE) {
                bad = bad + 1;
            }
            radix = radix + 1;
        }
        return bad;
    }

    /** Unsigned text, where the same bits mean something else entirely. */
    public static int textoSinSigno() {
        int bad = 0;
        if (!Integer.toUnsignedString(-1).equals("4294967295")) {
            bad = bad + 1;
        }
        if (!Long.toUnsignedString(-1L).equals("18446744073709551615")) {
            bad = bad + 1;
        }
        if (!Integer.toBinaryString(5).equals("101") || !Integer.toOctalString(8).equals("10")) {
            bad = bad + 1;
        }
        if (!Integer.toHexString(-1).equals("ffffffff")) {
            bad = bad + 1;
        }
        if (!Long.toHexString(-1L).equals("ffffffffffffffff")) {
            bad = bad + 1;
        }
        if (!Long.toBinaryString(-1L).equals(
                "1111111111111111111111111111111111111111111111111111111111111111")) {
            bad = bad + 1;
        }
        if (!Long.toOctalString(-1L).equals("1777777777777777777777")) {
            bad = bad + 1;
        }
        if (Integer.parseUnsignedInt("4294967295") != -1) {
            bad = bad + 1;
        }
        if (Long.parseUnsignedLong("18446744073709551615") != -1L) {
            bad = bad + 1;
        }
        if (Integer.parseUnsignedInt("ffffffff", 16) != -1) {
            bad = bad + 1;
        }
        if (Integer.toUnsignedLong(-1) != 4294967295L) {
            bad = bad + 1;
        }
        // Round trip through the unsigned reading, over values with the top bit set.
        long step = 1L;
        int i = 0;
        while (i < 40) {
            int v = (int) (step * 7L);
            String text = Integer.toUnsignedString(v);
            if (Integer.parseUnsignedInt(text) != v) {
                bad = bad + 1;
            }
            String hex = Integer.toUnsignedString(v, 16);
            if (Integer.parseUnsignedInt(hex, 16) != v) {
                bad = bad + 1;
            }
            long w = step * 7L;
            if (Long.parseUnsignedLong(Long.toUnsignedString(w)) != w) {
                bad = bad + 1;
            }
            if (Long.parseUnsignedLong(Long.toUnsignedString(w, 16), 16) != w) {
                bad = bad + 1;
            }
            step = step * 3L;
            i = i + 1;
        }
        return bad;
    }

    /** Unsigned arithmetic, checked by rebuilding the dividend from the quotient. */
    public static int sinSigno() {
        int bad = 0;
        if (Integer.compareUnsigned(-1, 1) <= 0 || Integer.compareUnsigned(1, -1) >= 0) {
            bad = bad + 1;
        }
        if (Long.compareUnsigned(-1L, 1L) <= 0 || Long.compareUnsigned(0L, 0L) != 0) {
            bad = bad + 1;
        }
        if (Integer.divideUnsigned(-1, 2) != 2147483647) {
            bad = bad + 1;
        }
        if (Integer.remainderUnsigned(-1, 2) != 1) {
            bad = bad + 1;
        }
        if (Long.divideUnsigned(-1L, 2L) != Long.MAX_VALUE) {
            bad = bad + 1;
        }
        if (Long.remainderUnsigned(-1L, 2L) != 1L) {
            bad = bad + 1;
        }
        // A divisor with its top bit set: the quotient can only be 0 or 1.
        if (Long.divideUnsigned(-1L, -2L) != 1L || Long.divideUnsigned(1L, -2L) != 0L) {
            bad = bad + 1;
        }
        // The invariant, swept: quotient * divisor + remainder rebuilds the dividend.
        long a = 1L;
        int i = 0;
        while (i < 40) {
            long d = 1L;
            int k = 0;
            while (k < 12) {
                long q = Long.divideUnsigned(a, d);
                long r = Long.remainderUnsigned(a, d);
                if (q * d + r != a) {
                    bad = bad + 1;
                }
                if (Long.compareUnsigned(r, d) >= 0) {
                    bad = bad + 1;
                }
                int qi = Integer.divideUnsigned((int) a, (int) d);
                int ri = Integer.remainderUnsigned((int) a, (int) d);
                if ((int) d != 0 && qi * (int) d + ri != (int) a) {
                    bad = bad + 1;
                }
                d = d * 7L + 1L;
                k = k + 1;
            }
            a = a * 3L + 1L;
            i = i + 1;
        }
        return bad;
    }

    /** The bit operations, mostly checked by the relationships between them. */
    public static int bits() {
        int bad = 0;
        if (Integer.highestOneBit(0) != 0 || Integer.highestOneBit(255) != 128) {
            bad = bad + 1;
        }
        if (Integer.highestOneBit(-1) != Integer.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Integer.lowestOneBit(0) != 0 || Integer.lowestOneBit(12) != 4) {
            bad = bad + 1;
        }
        if (Integer.numberOfTrailingZeros(0) != 32 || Integer.numberOfTrailingZeros(8) != 3) {
            bad = bad + 1;
        }
        if (Long.numberOfTrailingZeros(0L) != 64 || Long.numberOfLeadingZeros(0L) != 64) {
            bad = bad + 1;
        }
        if (Long.numberOfLeadingZeros(1L) != 63 || Long.bitCount(-1L) != 64) {
            bad = bad + 1;
        }
        if (Integer.reverse(1) != Integer.MIN_VALUE || Long.reverse(1L) != Long.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Integer.reverseBytes(0x01020304) != 0x04030201) {
            bad = bad + 1;
        }
        if (Integer.signum(-5) != -1 || Integer.signum(0) != 0 || Integer.signum(5) != 1) {
            bad = bad + 1;
        }
        if (Long.signum(-5L) != -1 || Long.signum(0L) != 0 || Long.signum(5L) != 1) {
            bad = bad + 1;
        }
        if (Integer.compress(0xFF, 0x0F0F) != 0x0F) {
            bad = bad + 1;
        }
        // The sweep: each operation checked against what must be true of it everywhere.
        int v = 1;
        int i = 0;
        while (i < 64) {
            // Reversing twice is the identity, and so is rotating a full turn.
            if (Integer.reverse(Integer.reverse(v)) != v) {
                bad = bad + 1;
            }
            if (Integer.reverseBytes(Integer.reverseBytes(v)) != v) {
                bad = bad + 1;
            }
            if (Integer.rotateLeft(v, 32) != v || Integer.rotateRight(v, 32) != v) {
                bad = bad + 1;
            }
            if (Integer.rotateRight(Integer.rotateLeft(v, 7), 7) != v) {
                bad = bad + 1;
            }
            // A rotation moves no bits in or out.
            if (Integer.bitCount(Integer.rotateLeft(v, 5)) != Integer.bitCount(v)) {
                bad = bad + 1;
            }
            // The highest and lowest set bits bracket the value.
            if (v != 0) {
                if (Integer.highestOneBit(v) > v && v > 0) {
                    bad = bad + 1;
                }
                if (Integer.numberOfTrailingZeros(v)
                        + Integer.numberOfLeadingZeros(v) > 31 && Integer.bitCount(v) > 1) {
                    bad = bad + 1;
                }
            }
            // compress and expand undo each other against the same mask.
            int mask = 0x0F0F0F0F;
            if (Integer.compress(Integer.expand(v, mask), mask) != (v & 0xFFFF)) {
                bad = bad + 1;
            }
            long w = (long) v * 4294967311L;
            if (Long.reverse(Long.reverse(w)) != w) {
                bad = bad + 1;
            }
            if (Long.reverseBytes(Long.reverseBytes(w)) != w) {
                bad = bad + 1;
            }
            if (Long.rotateLeft(w, 64) != w) {
                bad = bad + 1;
            }
            if (Long.rotateRight(Long.rotateLeft(w, 13), 13) != w) {
                bad = bad + 1;
            }
            if (Long.bitCount(w) != Integer.bitCount((int) w) + Integer.bitCount((int) (w >>> 32))) {
                bad = bad + 1;
            }
            v = v * 3 + 1;
            i = i + 1;
        }
        return bad;
    }

    /** Decoding, where the prefix picks the base. */
    public static int decodificar() {
        int bad = 0;
        if (Integer.decode("0x1F").intValue() != 31 || Integer.decode("#1F").intValue() != 31) {
            bad = bad + 1;
        }
        if (Integer.decode("017").intValue() != 15 || Integer.decode("17").intValue() != 17) {
            bad = bad + 1;
        }
        if (Integer.decode("-0x1F").intValue() != -31) {
            bad = bad + 1;
        }
        if (Integer.decode("+10").intValue() != 10 || Integer.decode("0").intValue() != 0) {
            bad = bad + 1;
        }
        if (Long.decode("0xFFFFFFFFFF").longValue() != 1099511627775L) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Boxing: equality by value, and a hash that agrees with it. */
    public static int objetos() {
        int bad = 0;
        if (!Integer.valueOf(5).equals(Integer.valueOf(5))) {
            bad = bad + 1;
        }
        if (Integer.valueOf(5).equals(Integer.valueOf(6))) {
            bad = bad + 1;
        }
        if (Integer.valueOf(5).equals(Long.valueOf(5L))) {
            bad = bad + 1;
        }
        if (Integer.valueOf(-7).hashCode() != -7) {
            bad = bad + 1;
        }
        if (Long.valueOf(1L).hashCode() != 1) {
            bad = bad + 1;
        }
        if (!Long.valueOf(5L).equals(Long.valueOf(5L))) {
            bad = bad + 1;
        }
        if (Integer.valueOf(200).byteValue() != -56) {
            bad = bad + 1;
        }
        // Double and Float compare by BITS, which is the opposite of `==` in two places.
        if (!Double.valueOf(Double.NaN).equals(Double.valueOf(Double.NaN))) {
            bad = bad + 1;
        }
        if (Double.valueOf(0.0d).equals(Double.valueOf(-0.0d))) {
            bad = bad + 1;
        }
        if (!Float.valueOf(Float.NaN).equals(Float.valueOf(Float.NaN))) {
            bad = bad + 1;
        }
        if (Double.hashCode(1.0d) != Double.valueOf(1.0d).hashCode()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Classification and the total order, where NaN and the two zeros are the difficulty. */
    public static int flotantes() {
        int bad = 0;
        if (!Double.isNaN(Double.NaN) || Double.isNaN(1.0d)) {
            bad = bad + 1;
        }
        if (!Double.isInfinite(Double.POSITIVE_INFINITY) || Double.isInfinite(1.0d)) {
            bad = bad + 1;
        }
        if (!Double.isFinite(1.0d) || Double.isFinite(Double.NaN)) {
            bad = bad + 1;
        }
        if (!Float.isNaN(Float.NaN) || !Float.isInfinite(Float.NEGATIVE_INFINITY)) {
            bad = bad + 1;
        }
        // The total order: -0.0 below +0.0, NaN above everything.
        if (Double.compare(-0.0d, 0.0d) >= 0 || Double.compare(0.0d, -0.0d) <= 0) {
            bad = bad + 1;
        }
        if (Double.compare(Double.NaN, Double.POSITIVE_INFINITY) <= 0) {
            bad = bad + 1;
        }
        if (Double.compare(Double.NaN, Double.NaN) != 0) {
            bad = bad + 1;
        }
        if (Float.compare(-0.0f, 0.0f) >= 0 || Float.compare(Float.NaN, 1.0f) <= 0) {
            bad = bad + 1;
        }
        // The bits, and the round trip through them.
        if (Double.doubleToLongBits(1.0d) != 4607182418800017408L) {
            bad = bad + 1;
        }
        if (Double.longBitsToDouble(4607182418800017408L) != 1.0d) {
            bad = bad + 1;
        }
        if (Float.floatToIntBits(1.0f) != 1065353216) {
            bad = bad + 1;
        }
        if (Float.intBitsToFloat(1065353216) != 1.0f) {
            bad = bad + 1;
        }
        // A raw NaN keeps its payload; the collapsing form does not.
        if (Double.doubleToLongBits(Double.NaN) != 0x7ff8000000000000L) {
            bad = bad + 1;
        }
        // The extremes are the values their names claim.
        if (Double.MAX_VALUE <= 0.0d || !Double.isInfinite(Double.MAX_VALUE * 2.0d)) {
            bad = bad + 1;
        }
        if (Double.MIN_VALUE <= 0.0d || Double.MIN_VALUE / 2.0d != 0.0d) {
            bad = bad + 1;
        }
        if (Float.MAX_VALUE <= 0.0f || !Float.isInfinite(Float.MAX_VALUE * 2.0f)) {
            bad = bad + 1;
        }
        if (Double.MIN_NORMAL / 2.0d >= Double.MIN_NORMAL) {
            bad = bad + 1;
        }
        // Hexadecimal rendering, which is exact.
        if (!Double.toHexString(1.0d).equals("0x1.0p0")) {
            bad = bad + 1;
        }
        if (!Double.toHexString(0.5d).equals("0x1.0p-1")) {
            bad = bad + 1;
        }
        if (!Double.toHexString(-3.0d).equals("-0x1.8p1")) {
            bad = bad + 1;
        }
        if (!Double.toHexString(0.0d).equals("0x0.0p0")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Text that is not a number is refused. */
    public static int malos() {
        int bad = 0;
        int i = 0;
        while (i < 12) {
            if (!WrapTest.refuses(i)) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        return bad;
    }

    static boolean refuses(int which) {
        try {
            WrapTest.badCase(which);
            return false;
        } catch (NumberFormatException expected) {
            return true;
        }
    }

    static long badCase(int which) {
        if (which == 0) {
            return (long) Integer.parseInt("");
        }
        if (which == 1) {
            return (long) Integer.parseInt("abc");
        }
        if (which == 2) {
            return (long) Integer.parseInt("2147483648");
        }
        if (which == 3) {
            return (long) Integer.parseInt("-2147483649");
        }
        if (which == 4) {
            return (long) Integer.parseInt("-");
        }
        if (which == 5) {
            return (long) Integer.parseInt("12", 1);
        }
        if (which == 6) {
            return (long) Integer.parseInt("12", 37);
        }
        if (which == 7) {
            return (long) Integer.parseUnsignedInt("-1");
        }
        if (which == 8) {
            return (long) Integer.parseUnsignedInt("4294967296");
        }
        if (which == 9) {
            return Long.parseLong("9223372036854775808");
        }
        if (which == 10) {
            return Long.parseUnsignedLong("18446744073709551616");
        }
        if (which == 11) {
            return (long) Integer.decode("0x").intValue();
        }
        return 0L;
    }


    /**
     * {@code TYPE} and the property readers, which are the last two families to arrive.
     *
     * <p>{@code Integer.TYPE} is the mirror of the PRIMITIVE, not of the wrapper, even though
     * the field is declared {@code Class<Integer>} -- there is no {@code Class<int>} to declare
     * it with, so the wrapper stands in as the type argument. The two mirrors are different
     * objects and reflection tells them apart, which is exactly why this is worth pinning.
     */
    public static int mirrorsYPropiedades() {
        int bad = 0;
        if (!Integer.TYPE.getName().equals("int")) {
            bad = bad + 1;
        }
        if (!Long.TYPE.getName().equals("long")) {
            bad = bad + 1;
        }
        if (!Double.TYPE.getName().equals("double") || !Float.TYPE.getName().equals("float")) {
            bad = bad + 1;
        }
        // The wrapper mirror is a DIFFERENT object, and says so.
        if (Integer.TYPE.getName().equals(Integer.class.getName())) {
            bad = bad + 1;
        }
        // A property that is certainly absent gives null, and the default form gives the default.
        if (System.getProperty("kaji.no.such.property") != null) {
            bad = bad + 1;
        }
        if (!System.getProperty("kaji.no.such.property", "fallback").equals("fallback")) {
            bad = bad + 1;
        }
        // One that every platform has.
        if (System.getProperty("line.separator") == null) {
            bad = bad + 1;
        }
        // getInteger/getLong: absent means the default, and an unparsable value means the same.
        if (Integer.getInteger("kaji.no.such.property") != null) {
            bad = bad + 1;
        }
        if (Integer.getInteger("kaji.no.such.property", 7).intValue() != 7) {
            bad = bad + 1;
        }
        if (Long.getLong("kaji.no.such.property", 9L).longValue() != 9L) {
            bad = bad + 1;
        }
        if (Integer.getInteger("line.separator", 3).intValue() != 3) {
            bad = bad + 1;
        }
        // The half-precision conversions, which round.
        if (Float.float16ToFloat(Float.floatToFloat16(1.0f)) != 1.0f) {
            bad = bad + 1;
        }
        if (Float.float16ToFloat(Float.floatToFloat16(0.5f)) != 0.5f) {
            bad = bad + 1;
        }
        if (Float.floatToFloat16(0.0f) != (short) 0) {
            bad = bad + 1;
        }
        if (Float.float16ToFloat((short) 0x3c00) != 1.0f) {
            bad = bad + 1;
        }
        if (Float.float16ToFloat((short) 0x7c00) != Float.POSITIVE_INFINITY) {
            bad = bad + 1;
        }
        // Anything above the half range saturates to infinity; anything far below to zero.
        if (Float.float16ToFloat(Float.floatToFloat16(1e30f)) != Float.POSITIVE_INFINITY) {
            bad = bad + 1;
        }
        if (Float.float16ToFloat(Float.floatToFloat16(1e-30f)) != 0.0f) {
            bad = bad + 1;
        }
        // A sweep: every value the half format can hold must survive the round trip exactly.
        int b = 0;
        while (b < 65536) {
            short h = (short) b;
            float f = Float.float16ToFloat(h);
            if (f == f) {
                // Not a NaN, so the trip back must be the identical bit pattern.
                if (Float.floatToFloat16(f) != h) {
                    bad = bad + 1;
                }
            }
            b = b + 37;
        }
        return bad;
    }

    /** Everything, so one call answers "does it work". */
    public static int todo() {
        return WrapTest.textoEntero() + WrapTest.textoSinSigno() + WrapTest.sinSigno()
                + WrapTest.bits() + WrapTest.decodificar() + WrapTest.objetos()
                + WrapTest.flotantes() + WrapTest.malos() + WrapTest.mirrorsYPropiedades();
    }

    public static void main(String[] args) {
        System.out.println("textoEntero     " + WrapTest.textoEntero());
        System.out.println("textoSinSigno   " + WrapTest.textoSinSigno());
        System.out.println("sinSigno        " + WrapTest.sinSigno());
        System.out.println("bits            " + WrapTest.bits());
        System.out.println("decodificar     " + WrapTest.decodificar());
        System.out.println("objetos         " + WrapTest.objetos());
        System.out.println("flotantes       " + WrapTest.flotantes());
        System.out.println("malos           " + WrapTest.malos());
        System.out.println("mirrors         " + WrapTest.mirrorsYPropiedades());
        System.out.println("TOTAL           " + WrapTest.todo());
    }
}
