import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * The String constructors, which do not run: the VM rewrites {@code new}/{@code <init>} into a
 * factory call and swaps the result in for the object {@code new} allocated.
 *
 * <p>Every method returns the number of things that came out wrong, so 0 is a pass. The same
 * source compiles against the JDK 25, where {@code main} prints the same counts, so every
 * expectation was checked against the reference before it was asked of ours.
 *
 * <p>All fifteen are covered. The six that take a {@code byte[]} and a charset are in their own
 * group, out of {@code todo}: they pass against the JDK and are blocked on our VM by finding
 * #110, which blocks every use of java.nio.charset equally.
 */
public class CtorTest {

    /** char[], whole and sliced, and that the result is a COPY of the array. */
    public static int caracteres() {
        int bad = 0;
        char[] src = new char[3];
        src[0] = 'a';
        src[1] = 'b';
        src[2] = 'c';
        String whole = new String(src);
        if (whole.length() != 3 || !whole.equals("abc")) {
            bad = bad + 1;
        }
        char[] wide = new char[5];
        wide[0] = 'x';
        wide[1] = 'h';
        wide[2] = 'o';
        wide[3] = 'i';
        wide[4] = 'y';
        String part = new String(wide, 1, 3);
        if (!part.equals("hoi")) {
            bad = bad + 1;
        }
        // A copy, not a view: this is the property the whole immutability story rests on.
        wide[1] = 'Z';
        if (!part.equals("hoi")) {
            bad = bad + 1;
        }
        // Empty, and a zero-length slice of a non-empty array.
        char[] none = new char[0];
        if (new String(none).length() != 0) {
            bad = bad + 1;
        }
        if (new String(wide, 2, 0).length() != 0) {
            bad = bad + 1;
        }
        // The last valid slice, where an off-by-one would show.
        if (!new String(wide, 4, 1).equals("y")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The no-argument one, which is the empty string. */
    public static int vacio() {
        int bad = 0;
        String s = new String();
        if (s.length() != 0 || !s.equals("")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Characters the inline layout has to carry as more than one byte. */
    public static int noAscii() {
        int bad = 0;
        char[] acc = new char[2];
        acc[0] = (char) 0xe9;
        acc[1] = (char) 0x20ac;
        String s = new String(acc);
        if (s.length() != 2) {
            bad = bad + 1;
        }
        if (s.charAt(0) != (char) 0xe9 || s.charAt(1) != (char) 0x20ac) {
            bad = bad + 1;
        }
        // A surrogate pair: two chars in, two chars out, one character.
        char[] pair = new char[2];
        pair[0] = (char) 0xd834;
        pair[1] = (char) 0xdd60;
        String note = new String(pair);
        if (note.length() != 2 || note.codePointAt(0) != 0x1d160) {
            bad = bad + 1;
        }
        return bad;
    }

    /** int[] as CODE POINTS, where the result can be longer than the count asked for. */
    public static int puntosDeCodigo() {
        int bad = 0;
        int[] cps = new int[3];
        cps[0] = 'a';
        cps[1] = 0x1d160;
        cps[2] = 'b';
        String s = new String(cps, 0, 3);
        // Three code points, FOUR chars: the middle one needs a surrogate pair.
        if (s.length() != 4) {
            bad = bad + 1;
        }
        if (s.charAt(0) != 'a' || s.charAt(3) != 'b') {
            bad = bad + 1;
        }
        if (s.codePointAt(1) != 0x1d160) {
            bad = bad + 1;
        }
        String one = new String(cps, 2, 1);
        if (!one.equals("b")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The copy constructor and the two builder ones. */
    public static int deOtros() {
        int bad = 0;
        String original = "hola";
        String copy = new String(original);
        if (!copy.equals(original)) {
            bad = bad + 1;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ho");
        sb.append("la");
        if (!new String(sb).equals("hola")) {
            bad = bad + 1;
        }
        // A SNAPSHOT: appending afterwards must not reach the string already made.
        String snapshot = new String(sb);
        sb.append("!");
        if (!snapshot.equals("hola")) {
            bad = bad + 1;
        }
        StringBuffer buf = new StringBuffer();
        buf.append("che");
        if (!new String(buf).equals("che")) {
            bad = bad + 1;
        }
        if (new String(new StringBuilder()).length() != 0) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The deprecated hibyte forms, which predate charsets. */
    public static int altoByte() {
        int bad = 0;
        byte[] low = new byte[3];
        low[0] = 0x41;
        low[1] = 0x42;
        low[2] = 0x43;
        // hibyte 0: plain ASCII.
        if (!new String(low, 0).equals("ABC")) {
            bad = bad + 1;
        }
        // hibyte 1: every character moves up a plane-worth of 256.
        String shifted = new String(low, 1);
        if (shifted.length() != 3 || shifted.charAt(0) != (char) 0x141) {
            bad = bad + 1;
        }
        // The sliced form: (ascii, hibyte, offset, count).
        if (!new String(low, 0, 1, 2).equals("BC")) {
            bad = bad + 1;
        }
        // Only the low eight bits of hibyte are used.
        if (new String(low, 0x100).charAt(0) != 'A') {
            bad = bad + 1;
        }
        return bad;
    }

    /** A slice outside the array is refused, not silently clamped. */
    public static int fueraDeRango() {
        int bad = 0;
        char[] src = new char[3];
        boolean threw = false;
        try {
            String s = new String(src, 1, 5);
            threw = false;
        } catch (StringIndexOutOfBoundsException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        threw = false;
        try {
            String s = new String(src, -1, 2);
            threw = false;
        } catch (StringIndexOutOfBoundsException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * The result is an ordinary String: everything built on the native seams works on it.
     *
     * <p>Worth its own group, because the rewrite could plausibly produce something that
     * answers `length` correctly and then fails at the first method that is not native.
     */
    public static int esUnStringDeVerdad() {
        int bad = 0;
        char[] src = new char[5];
        src[0] = ' ';
        src[1] = 'H';
        src[2] = 'o';
        src[3] = 'l';
        src[4] = 'a';
        String s = new String(src);
        if (!s.trim().equals("Hola")) {
            bad = bad + 1;
        }
        if (!s.substring(1, 3).equals("Ho")) {
            bad = bad + 1;
        }
        if (s.indexOf('l') != 3) {
            bad = bad + 1;
        }
        if (!s.concat("!").equals(" Hola!")) {
            bad = bad + 1;
        }
        if (s.hashCode() != " Hola".hashCode()) {
            bad = bad + 1;
        }
        if (!("<" + s + ">").equals("< Hola>")) {
            bad = bad + 1;
        }
        if (s.compareTo(" Hola") != 0) {
            bad = bad + 1;
        }
        return bad;
    }


    /**
     * The six that take a {@code byte[]} and a charset.
     *
     * <p>Out of {@code todo} for the same reason the whole of java.nio.charset is: decoding
     * reaches {@code StandardCharsets.UTF_8} and the coder constants, and a cross-unit static
     * read is emitted as a {@code getfield} over a static (finding #110). Nothing to do with the
     * constructors -- they pass against the JDK, and the decoding they delegate to is validated
     * on its own by {@code java/CharsetTest.java}.
     */
    public static int bytes() {
        int bad = 0;
        byte[] hola = new byte[4];
        hola[0] = 'h';
        hola[1] = 'o';
        hola[2] = 'l';
        hola[3] = 'a';
        if (!new String(hola).equals("hola")) {
            bad = bad + 1;
        }
        if (!new String(hola, StandardCharsets.UTF_8).equals("hola")) {
            bad = bad + 1;
        }
        if (!new String(hola, 1, 2).equals("ol")) {
            bad = bad + 1;
        }
        if (!new String(hola, 1, 2, StandardCharsets.UTF_8).equals("ol")) {
            bad = bad + 1;
        }
        // The same two bytes mean different things in different charsets, which is the whole
        // reason the charset has to be named.
        byte[] acute = new byte[2];
        acute[0] = (byte) 0xc3;
        acute[1] = (byte) 0xa9;
        String asUtf8 = new String(acute, StandardCharsets.UTF_8);
        if (asUtf8.length() != 1 || asUtf8.charAt(0) != (char) 0xe9) {
            bad = bad + 1;
        }
        String asLatin1 = new String(acute, StandardCharsets.ISO_8859_1);
        if (asLatin1.length() != 2 || asLatin1.charAt(0) != (char) 0xc3) {
            bad = bad + 1;
        }
        // Broken input is REPLACED, not reported: this constructor never throws for bad bytes.
        byte[] broken = new byte[2];
        broken[0] = 'a';
        broken[1] = (byte) 0x80;
        String repaired = new String(broken, StandardCharsets.UTF_8);
        if (repaired.length() != 2 || repaired.charAt(1) != (char) 0xfffd) {
            bad = bad + 1;
        }
        // By name, with an alias and the wrong case.
        try {
            if (!new String(hola, "utf8").equals("hola")) {
                bad = bad + 1;
            }
            if (!new String(hola, 0, 4, "UTF-8").equals("hola")) {
                bad = bad + 1;
            }
        } catch (UnsupportedEncodingException unexpected) {
            bad = bad + 1;
        }
        boolean threw = false;
        try {
            String s = new String(hola, "no-such-charset");
            threw = false;
        } catch (UnsupportedEncodingException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        // A supplementary character round-trips through four UTF-8 bytes into two chars.
        byte[] note = new byte[4];
        note[0] = (byte) 0xf0;
        note[1] = (byte) 0x9d;
        note[2] = (byte) 0x85;
        note[3] = (byte) 0xa0;
        String music = new String(note, StandardCharsets.UTF_8);
        if (music.length() != 2 || music.codePointAt(0) != 0x1d160) {
            bad = bad + 1;
        }
        return bad;
    }

    public static int todo() {
        return CtorTest.caracteres() + CtorTest.vacio() + CtorTest.noAscii()
                + CtorTest.puntosDeCodigo() + CtorTest.deOtros() + CtorTest.altoByte()
                + CtorTest.fueraDeRango() + CtorTest.esUnStringDeVerdad();
    }

    public static void main(String[] args) {
        System.out.println("caracteres           " + CtorTest.caracteres());
        System.out.println("vacio                " + CtorTest.vacio());
        System.out.println("noAscii              " + CtorTest.noAscii());
        System.out.println("puntosDeCodigo       " + CtorTest.puntosDeCodigo());
        System.out.println("deOtros              " + CtorTest.deOtros());
        System.out.println("altoByte             " + CtorTest.altoByte());
        System.out.println("fueraDeRango         " + CtorTest.fueraDeRango());
        System.out.println("esUnStringDeVerdad   " + CtorTest.esUnStringDeVerdad());
        System.out.println("bytes                " + CtorTest.bytes());
        System.out.println("TOTAL                " + CtorTest.todo());
    }
}
