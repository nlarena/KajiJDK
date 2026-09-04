/**
 * Exercises {@code Double.parseDouble} and {@code Float.parseFloat}. Every method returns the
 * number of things that came out wrong, so 0 is a pass.
 *
 * <p>The test is a ROUND TRIP, and deliberately so. Checking a parser against a table of
 * expected values only proves the entries in the table; checking that {@code parse(toString(x))}
 * gives back exactly {@code x} -- bit for bit, over a wide sweep of values -- proves the two
 * halves agree everywhere, which is the property a program actually relies on when its numbers
 * pass through a file.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 *
 * <p><strong>Ojo: {@code main} no termina en nuestra VM, y no es esta prueba la que esta mal.</strong>
 * {@code Double.toString} crece de forma cuadratica con la cantidad de llamadas --50 tardan 1 s y 250
 * tardan 28 s, siempre con el mismo valor-- asi que los dos metodos que barren de a miles,
 * {@code ida_y_vuelta} y {@code dobleRedondeo}, no llegan. Los otros cinco ({@code basicos},
 * {@code dificiles}, {@code hexadecimal}, {@code flotantes}, {@code malos}) dan <strong>0</strong> en
 * segundos y se pueden correr sueltos. Esta medido y con repro en {@code COMPILER_FINDINGS.md} y
 * {@code scratchpad/zz324/}.
 */
public class ParseTest {

    /** The plain cases, where a wrong answer would be obvious. */
    public static int basicos() {
        int bad = 0;
        if (Double.parseDouble("0") != 0.0d || Double.parseDouble("1") != 1.0d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("-1") != -1.0d || Double.parseDouble("+1") != 1.0d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("0.5") != 0.5d || Double.parseDouble("-0.5") != -0.5d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("100") != 100.0d || Double.parseDouble(".5") != 0.5d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("2.") != 2.0d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("1e3") != 1000.0d || Double.parseDouble("1E-3") != 0.001d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("1.5e2") != 150.0d) {
            bad = bad + 1;
        }
        // Surrounding whitespace is trimmed, and a type suffix is ignored.
        if (Double.parseDouble("  2.5  ") != 2.5d || Double.parseDouble("2.5f") != 2.5d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("2.5d") != 2.5d) {
            bad = bad + 1;
        }
        // The sign of a zero survives.
        if (1.0d / Double.parseDouble("-0.0") > 0.0d) {
            bad = bad + 1;
        }
        if (1.0d / Double.parseDouble("0.0") < 0.0d) {
            bad = bad + 1;
        }
        // The three names.
        double nan = Double.parseDouble("NaN");
        if (nan == nan) {
            bad = bad + 1;
        }
        if (Double.parseDouble("Infinity") != Double.POSITIVE_INFINITY) {
            bad = bad + 1;
        }
        if (Double.parseDouble("-Infinity") != Double.NEGATIVE_INFINITY) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The values where a rounding shortcut goes wrong. */
    public static int dificiles() {
        int bad = 0;
        // A decimal that is exactly halfway between two doubles: half-to-even must pick the one
        // with the even significand, and picking "up" instead is the classic bug.
        if (Double.parseDouble("1.0000000000000002") != 1.0000000000000002d) {
            bad = bad + 1;
        }
        // 17 significant digits, which is where a 53-bit double runs out.
        if (Double.parseDouble("0.1") != 0.1d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("0.3") != 0.3d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("2.2250738585072011e-308") != 2.2250738585072011e-308d) {
            bad = bad + 1;
        }
        // The extremes of the format.
        if (Double.parseDouble("1.7976931348623157E308") != Double.MAX_VALUE) {
            bad = bad + 1;
        }
        if (Double.parseDouble("4.9E-324") != Double.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Double.parseDouble("2.2250738585072014E-308") != Double.MIN_NORMAL) {
            bad = bad + 1;
        }
        // Past the extremes: overflow to infinity, underflow to zero.
        if (Double.parseDouble("1e400") != Double.POSITIVE_INFINITY) {
            bad = bad + 1;
        }
        if (Double.parseDouble("-1e400") != Double.NEGATIVE_INFINITY) {
            bad = bad + 1;
        }
        if (Double.parseDouble("1e-400") != 0.0d) {
            bad = bad + 1;
        }
        // Leading zeros and a long tail of them change nothing.
        if (Double.parseDouble("000123.4500000") != 123.45d) {
            bad = bad + 1;
        }
        // A subnormal, where the significand is short and the exponent pinned.
        if (Double.parseDouble("1e-320") != 1e-320d) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Hexadecimal significands, which are exact and so have no rounding to get wrong. */
    public static int hexadecimal() {
        int bad = 0;
        if (Double.parseDouble("0x1p0") != 1.0d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("0x1.8p1") != 3.0d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("0x1p-1") != 0.5d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("-0x1.0p3") != -8.0d) {
            bad = bad + 1;
        }
        if (Double.parseDouble("0X1.FFFFFFFFFFFFFP1023") != Double.MAX_VALUE) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * The round trip over a wide sweep: every value printed and read back must be identical.
     *
     * <p>Compared by BITS rather than by {@code ==}, which would call the two zeros equal and
     * every NaN unequal -- neither of which is what "came back unchanged" means.
     *
     * <p>The steps are coarse on purpose. Each round trip runs an arbitrary-precision division,
     * and on a green-threads interpreter that is not free; the sweep walks the whole exponent
     * range in strides rather than densely, which is where the format changes behaviour
     * (normal to subnormal, and the two ends) rather than in between.
     */
    public static int ida_y_vuelta() {
        int bad = 0;
        double v = 1.0d;
        int i = 0;
        // Powers of two and their neighbours, up and down across the whole exponent range.
        while (i < 40) {
            if (!ParseTest.trips(v) || !ParseTest.trips(-v)) {
                bad = bad + 1;
            }
            if (!ParseTest.trips(v * 3.0d)) {
                bad = bad + 1;
            }
            v = v * 32.0d;
            i = i + 1;
        }
        v = 1.0d;
        i = 0;
        while (i < 40) {
            if (!ParseTest.trips(v) || !ParseTest.trips(-v)) {
                bad = bad + 1;
            }
            if (!ParseTest.trips(v * 7.0d)) {
                bad = bad + 1;
            }
            v = v / 32.0d;
            i = i + 1;
        }
        // A spread of ordinary decimals.
        int k = 1;
        while (k < 2000) {
            double d = (double) k / 1000.0d;
            if (!ParseTest.trips(d)) {
                bad = bad + 1;
            }
            if (!ParseTest.trips((double) k * 1000000.0d)) {
                bad = bad + 1;
            }
            k = k + 137;
        }
        // And the named extremes.
        if (!ParseTest.trips(Double.MAX_VALUE) || !ParseTest.trips(Double.MIN_VALUE)) {
            bad = bad + 1;
        }
        if (!ParseTest.trips(Double.MIN_NORMAL) || !ParseTest.trips(0.0d)) {
            bad = bad + 1;
        }
        return bad;
    }

    static boolean trips(double d) {
        String text = Double.toString(d);
        double back = Double.parseDouble(text);
        return Double.doubleToLongBits(back) == Double.doubleToLongBits(d);
    }

    /** The same for floats, where reading at the wrong width would round twice. */
    public static int flotantes() {
        int bad = 0;
        if (Float.parseFloat("1.5") != 1.5f || Float.parseFloat("-0.25") != -0.25f) {
            bad = bad + 1;
        }
        if (Float.parseFloat("1e10") != 1e10f) {
            bad = bad + 1;
        }
        if (Float.parseFloat("3.4028235E38") != Float.MAX_VALUE) {
            bad = bad + 1;
        }
        if (Float.parseFloat("1.4E-45") != Float.MIN_VALUE) {
            bad = bad + 1;
        }
        if (Float.parseFloat("1e40") != Float.POSITIVE_INFINITY) {
            bad = bad + 1;
        }
        if (Float.parseFloat("1e-50") != 0.0f) {
            bad = bad + 1;
        }
        float nan = Float.parseFloat("NaN");
        if (nan == nan) {
            bad = bad + 1;
        }
        // The round trip, over the float exponent range.
        float f = 1.0f;
        int i = 0;
        while (i < 15) {
            if (!ParseTest.tripsF(f) || !ParseTest.tripsF(-f) || !ParseTest.tripsF(f * 3.0f)) {
                bad = bad + 1;
            }
            f = f * 16.0f;
            i = i + 1;
        }
        f = 1.0f;
        i = 0;
        while (i < 15) {
            if (!ParseTest.tripsF(f) || !ParseTest.tripsF(f / 3.0f)) {
                bad = bad + 1;
            }
            f = f / 16.0f;
            i = i + 1;
        }
        int k = 1;
        while (k < 2000) {
            if (!ParseTest.tripsF((float) k / 100.0f)) {
                bad = bad + 1;
            }
            k = k + 271;
        }
        if (!ParseTest.tripsF(Float.MAX_VALUE) || !ParseTest.tripsF(Float.MIN_VALUE)) {
            bad = bad + 1;
        }
        return bad;
    }

    static boolean tripsF(float f) {
        String text = Float.toString(f);
        float back = Float.parseFloat(text);
        return Float.floatToIntBits(back) == Float.floatToIntBits(f);
    }

    /**
     * Reading a float must not go through a double.
     *
     * <p>These decimals sit close enough to the midpoint between two floats that rounding to a
     * double first and narrowing afterwards lands on the wrong one -- the double rounding moves
     * the value across the boundary. Reading at float width in one step cannot.
     */
    public static int dobleRedondeo() {
        int bad = 0;
        String[] tricky = new String[6];
        tricky[0] = "1.00000005960464477539062";
        tricky[1] = "0.500000000000000055511151231257827";
        tricky[2] = "8.5070593e37";
        tricky[3] = "1.1754944e-38";
        tricky[4] = "16777217";
        tricky[5] = "16777219";
        int i = 0;
        while (i < tricky.length) {
            float direct = Float.parseFloat(tricky[i]);
            // The value must round-trip, which pins it to one specific float.
            if (!ParseTest.tripsF(direct)) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        // 16777217 is 2^24 + 1, which no float can hold: it must land on 2^24 exactly.
        if (Float.parseFloat("16777217") != 16777216.0f) {
            bad = bad + 1;
        }
        // 16777219 is 2^24 + 3, which is a tie and must go to the even neighbour, 2^24 + 4.
        if (Float.parseFloat("16777219") != 16777220.0f) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Text that is not a number is refused, rather than answered with a guess. */
    public static int malos() {
        int bad = 0;
        int i = 0;
        while (i < 9) {
            if (!ParseTest.refuses(i)) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        return bad;
    }

    static boolean refuses(int which) {
        try {
            ParseTest.badCase(which);
            return false;
        } catch (NumberFormatException expected) {
            return true;
        }
    }

    static double badCase(int which) {
        if (which == 0) {
            return Double.parseDouble("");
        }
        if (which == 1) {
            return Double.parseDouble("   ");
        }
        if (which == 2) {
            return Double.parseDouble("abc");
        }
        if (which == 3) {
            return Double.parseDouble("1.2.3");
        }
        if (which == 4) {
            return Double.parseDouble("1e");
        }
        if (which == 5) {
            return Double.parseDouble("1e+");
        }
        if (which == 6) {
            return Double.parseDouble("--1");
        }
        if (which == 7) {
            return Double.parseDouble("1 2");
        }
        if (which == 8) {
            return (double) Float.parseFloat("xyz");
        }
        return 0.0d;
    }

    /** Everything, so one call answers "does it work". */
    public static int todo() {
        return ParseTest.basicos() + ParseTest.dificiles() + ParseTest.hexadecimal()
                + ParseTest.ida_y_vuelta() + ParseTest.flotantes() + ParseTest.dobleRedondeo()
                + ParseTest.malos();
    }

    public static void main(String[] args) {
        System.out.println("basicos         " + ParseTest.basicos());
        System.out.println("dificiles       " + ParseTest.dificiles());
        System.out.println("hexadecimal     " + ParseTest.hexadecimal());
        System.out.println("ida_y_vuelta    " + ParseTest.ida_y_vuelta());
        System.out.println("flotantes       " + ParseTest.flotantes());
        System.out.println("dobleRedondeo   " + ParseTest.dobleRedondeo());
        System.out.println("malos           " + ParseTest.malos());
        System.out.println("TOTAL           " + ParseTest.todo());
    }
}
