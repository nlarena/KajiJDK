/**
 * Exercises java.lang.Character on our VM. Every method returns the number of things that came
 * out wrong, so 0 is a pass. The exhaustive comparison against the JDK lives in the port; this
 * checks that the tables load and read back here.
 */
public class CharTest {

    /** ASCII case, both directions. */
    public static int ascii() {
        int bad = 0;
        if (Character.toLowerCase('A') != 'a') {
            bad = bad + 1;
        }
        if (Character.toUpperCase('z') != 'Z') {
            bad = bad + 1;
        }
        if (Character.toLowerCase('5') != '5') {
            bad = bad + 1;
        }
        if (!Character.isUpperCase('Q') || Character.isUpperCase('q')) {
            bad = bad + 1;
        }
        if (!Character.isLowerCase('q') || Character.isLowerCase('Q')) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Non-ASCII case: the whole point of having tables. */
    public static int acentos() {
        int bad = 0;
        if (Character.toLowerCase((char) 0xc1) != (char) 0xe1) {
            bad = bad + 1;
        }
        if (Character.toUpperCase((char) 0xf1) != (char) 0xd1) {
            bad = bad + 1;
        }
        // Greek sigma, and Cyrillic.
        if (Character.toLowerCase((char) 0x3a3) != (char) 0x3c3) {
            bad = bad + 1;
        }
        if (Character.toUpperCase((char) 0x434) != (char) 0x414) {
            bad = bad + 1;
        }
        if (!Character.isLetter((char) 0x4e2d)) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Classification. */
    public static int clases() {
        int bad = 0;
        if (!Character.isLetter('x') || Character.isLetter('7')) {
            bad = bad + 1;
        }
        if (!Character.isDigit('7') || Character.isDigit('x')) {
            bad = bad + 1;
        }
        if (!Character.isLetterOrDigit('7') || Character.isLetterOrDigit('-')) {
            bad = bad + 1;
        }
        if (!Character.isWhitespace('\t') || Character.isWhitespace('x')) {
            bad = bad + 1;
        }
        // A non-breaking space is a space char but NOT java whitespace.
        if (Character.isWhitespace((char) 0xa0)) {
            bad = bad + 1;
        }
        if (!Character.isSpaceChar((char) 0xa0)) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Digits in other scripts, which is what decimalValue is for. */
    public static int digitos() {
        int bad = 0;
        if (Character.digit('f', 16) != 15) {
            bad = bad + 1;
        }
        if (Character.digit('f', 10) != -1) {
            bad = bad + 1;
        }
        // Arabic-Indic three.
        if (Character.digit((char) 0x663, 10) != 3) {
            bad = bad + 1;
        }
        if (Character.forDigit(15, 16) != 'f') {
            bad = bad + 1;
        }
        if (Character.forDigit(15, 10) != 0) {
            bad = bad + 1;
        }
        if (Character.getNumericValue('z') != 35) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Surrogates and code points. */
    public static int puntos() {
        int bad = 0;
        int note = 0x1d160;
        if (Character.charCount(note) != 2) {
            bad = bad + 1;
        }
        char hi = Character.highSurrogate(note);
        char lo = Character.lowSurrogate(note);
        if (hi != (char) 0xd834 || lo != (char) 0xdd60) {
            bad = bad + 1;
        }
        if (Character.toCodePoint(hi, lo) != note) {
            bad = bad + 1;
        }
        if (!Character.isHighSurrogate(hi) || !Character.isLowSurrogate(lo)) {
            bad = bad + 1;
        }
        if (!Character.isSurrogatePair(hi, lo) || Character.isSurrogatePair(lo, hi)) {
            bad = bad + 1;
        }
        if (!Character.isSupplementaryCodePoint(note) || Character.isBmpCodePoint(note)) {
            bad = bad + 1;
        }
        char[] made = Character.toChars(note);
        if (made.length != 2 || made[0] != hi || made[1] != lo) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Everything at once, so one call answers "does it work". */
    public static int todo() {
        return CharTest.ascii() + CharTest.acentos() + CharTest.clases() + CharTest.digitos()
                + CharTest.puntos();
    }
}
