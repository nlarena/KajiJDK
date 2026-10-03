import java.math.BigInteger;

/**
 * Behaviour test of the bit operations of java.math.BigInteger, written to run **the same** in
 * this VM and in the real JDK.
 *
 * <p>Each check has an index. {@code run()} returns -1 if they all passed, or the index of the
 * first one that failed, which is enough to compare the two VMs without depending on the console
 * output matching character by character.
 *
 * <p>It exists because differential fuzzing against OpenJDK 25 found two defects here, and both
 * were of the same shape: a negative number is stored as sign plus magnitude, and the operation
 * was applied to the magnitude as if the sign were not there.
 *
 * <ul>
 *   <li>{@code shiftRight} rounded towards zero instead of towards minus infinity, so
 *       {@code -1 >> 1} came out 0 where it has to be -1;
 *   <li>{@code testBit} read the magnitude directly instead of the two's complement, so
 *       {@code -1} reported every bit clear where every bit is set.
 * </ul>
 *
 * <p>{@code modPow} is checked too. It never had a defect of its own: it diverged because it
 * reads the exponent bit by bit through {@code testBit}, which is worth pinning down so that a
 * future change to either one cannot quietly break the other.
 */
public class BigIntegerTest {

    private static BigInteger b(String s) {
        return new BigInteger(s);
    }

    public static int run() {

        // ---- shiftRight rounds towards minus infinity ----

        if (!b("-1").shiftRight(1).toString().equals("-1")) {
            return 1;
        }
        if (!b("-2").shiftRight(1).toString().equals("-1")) {
            return 2;
        }
        if (!b("-3").shiftRight(1).toString().equals("-2")) {
            return 3;
        }
        if (!b("-2").shiftRight(7).toString().equals("-1")) {
            return 4;
        }
        if (!b("-129").shiftRight(7).toString().equals("-2")) {
            return 5;
        }
        // Past the whole magnitude: everything negative collapses onto -1, not onto 0.
        if (!b("-2").shiftRight(64).toString().equals("-1")) {
            return 6;
        }
        if (!b("-123456789012345678901234567890").shiftRight(100).toString().equals("-1")) {
            return 7;
        }
        // A negative shiftLeft is a shiftRight and has to round the same way.
        if (!b("-5").shiftLeft(-1).toString().equals("-3")) {
            return 8;
        }
        if (!b("-2").shiftLeft(-33).toString().equals("-1")) {
            return 9;
        }

        // Controls. A positive value, an exact division and zero must not move: the rounding
        // only applies when the shift drops a 1 bit off a negative.
        if (!b("129").shiftRight(7).toString().equals("1")) {
            return 10;
        }
        if (!b("-128").shiftRight(7).toString().equals("-1")) {
            return 11;
        }
        if (!b("-4").shiftRight(2).toString().equals("-1")) {
            return 12;
        }
        if (!b("0").shiftRight(5).toString().equals("0")) {
            return 13;
        }
        if (!b("-1").shiftLeft(1).toString().equals("-2")) {
            return 14;
        }

        // ---- testBit reads the two's complement ----

        // -1 is ...1111, so every bit is set, however far up it is asked for.
        if (!b("-1").testBit(0) || !b("-1").testBit(1) || !b("-1").testBit(31)
                || !b("-1").testBit(63)) {
            return 15;
        }
        // -2 is ...1110.
        if (b("-2").testBit(0)) {
            return 16;
        }
        if (!b("-2").testBit(1) || !b("-2").testBit(31)) {
            return 17;
        }
        // -4 is ...1100: the bits below the lowest set bit of the magnitude stay clear.
        if (b("-4").testBit(0) || b("-4").testBit(1)) {
            return 18;
        }
        if (!b("-4").testBit(2) || !b("-4").testBit(70)) {
            return 19;
        }
        // -5 is ...1011.
        if (!b("-5").testBit(0) || !b("-5").testBit(1)) {
            return 20;
        }
        if (b("-5").testBit(2)) {
            return 21;
        }

        // Controls: on a positive value testBit is just the magnitude, and past the top it is 0.
        if (!b("5").testBit(0) || b("5").testBit(1) || !b("5").testBit(2)) {
            return 22;
        }
        if (b("5").testBit(99)) {
            return 23;
        }
        if (b("0").testBit(0) || b("0").testBit(64)) {
            return 24;
        }

        // ---- modPow, which walks the exponent through testBit ----

        if (!b("3").modPow(b("100"), b("7")).toString().equals("4")) {
            return 25;
        }
        if (!b("2").modPow(b("0"), b("7")).toString().equals("1")) {
            return 26;
        }
        if (!b("12345").modPow(b("6789"), b("1000000007")).toString().equals("130015824")) {
            return 27;
        }

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(BigIntegerTest.run());
    }
}
