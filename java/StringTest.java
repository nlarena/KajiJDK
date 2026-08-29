import java.util.ArrayList;
import java.util.List;

/**
 * Exercises java.lang.String. Every method returns the number of things that came out wrong,
 * so 0 is a pass.
 *
 * The same source compiles against the JDK 25, where `main` prints the same counts, so every
 * expectation below is checked against the real String before it is asked of ours.
 */
public class StringTest {

    /** substring, concat, repeat, and the two emptiness questions. */
    public static int rebanadas() {
        int bad = 0;
        String s = "hola mundo";
        if (!s.substring(5).equals("mundo")) {
            bad = bad + 1;
        }
        if (!s.substring(0, 4).equals("hola")) {
            bad = bad + 1;
        }
        if (!s.substring(4, 4).equals("")) {
            bad = bad + 1;
        }
        if (!"ab".concat("cd").equals("abcd")) {
            bad = bad + 1;
        }
        if (!"ab".concat("").equals("ab")) {
            bad = bad + 1;
        }
        if (!"ab".repeat(3).equals("ababab")) {
            bad = bad + 1;
        }
        if (!"ab".repeat(0).equals("")) {
            bad = bad + 1;
        }
        if (!"".isEmpty() || "a".isEmpty()) {
            bad = bad + 1;
        }
        if (!"  \t\n".isBlank() || "a ".isBlank()) {
            bad = bad + 1;
        }
        boolean refused = false;
        try {
            String gone = s.substring(3, 2);
            if (gone != null) {
                bad = bad + 1;
            }
        } catch (RuntimeException expected) {
            refused = true;
        }
        if (!refused) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The index families, forwards and backwards, by char and by string. */
    public static int busqueda() {
        int bad = 0;
        String s = "abracadabra";
        if (s.indexOf('a') != 0) {
            bad = bad + 1;
        }
        if (s.indexOf('a', 1) != 3) {
            bad = bad + 1;
        }
        if (s.indexOf('z') != -1) {
            bad = bad + 1;
        }
        if (s.indexOf("abra") != 0) {
            bad = bad + 1;
        }
        if (s.indexOf("abra", 1) != 7) {
            bad = bad + 1;
        }
        if (s.indexOf("abra", 1, 10) != -1) {
            bad = bad + 1;
        }
        if (s.indexOf("") != 0) {
            bad = bad + 1;
        }
        if (s.lastIndexOf('a') != 10) {
            bad = bad + 1;
        }
        if (s.lastIndexOf('a', 5) != 5) {
            bad = bad + 1;
        }
        if (s.lastIndexOf("abra") != 7) {
            bad = bad + 1;
        }
        if (s.lastIndexOf("abra", 6) != 0) {
            bad = bad + 1;
        }
        if (!s.contains("cad")) {
            bad = bad + 1;
        }
        if (s.contains("zz")) {
            bad = bad + 1;
        }
        if (!s.endsWith("bra") || s.endsWith("brb")) {
            bad = bad + 1;
        }
        if (!s.startsWith("cad", 4)) {
            bad = bad + 1;
        }
        if (s.startsWith("cad", 5)) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Case-insensitive comparison and the region tests. */
    public static int comparacion() {
        int bad = 0;
        if (!"Hola".equalsIgnoreCase("hOLA")) {
            bad = bad + 1;
        }
        if ("Hola".equalsIgnoreCase("hOLAs")) {
            bad = bad + 1;
        }
        if ("Hola".compareToIgnoreCase("hola") != 0) {
            bad = bad + 1;
        }
        if ("abc".compareToIgnoreCase("abd") >= 0) {
            bad = bad + 1;
        }
        if (!"abcdef".regionMatches(2, "xxcdyy", 2, 2)) {
            bad = bad + 1;
        }
        if ("abcdef".regionMatches(2, "xxCDyy", 2, 2)) {
            bad = bad + 1;
        }
        if (!"abcdef".regionMatches(true, 2, "xxCDyy", 2, 2)) {
            bad = bad + 1;
        }
        // Out of range answers false rather than throwing.
        if ("abc".regionMatches(2, "abc", 0, 5)) {
            bad = bad + 1;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("hola");
        if (!"hola".contentEquals(sb)) {
            bad = bad + 1;
        }
        if ("holas".contentEquals(sb)) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Characters out, and the code-point view over a surrogate pair. */
    public static int caracteres() {
        int bad = 0;
        char[] out = "hola".toCharArray();
        if (out.length != 4 || out[0] != 'h' || out[3] != 'a') {
            bad = bad + 1;
        }
        char[] dst = new char[6];
        dst[0] = 'x';
        dst[5] = 'z';
        "hola".getChars(1, 4, dst, 1);
        if (dst[0] != 'x' || dst[1] != 'o' || dst[3] != 'a' || dst[5] != 'z') {
            bad = bad + 1;
        }
        // U+1D160, a musical note, is stored as the pair D834 DD60. Built from chars and
        // not written as a literal: a supplementary character in a literal produces a
        // class file that cannot be read back (finding #259).
        char[] pair = new char[4];
        pair[0] = 'a';
        pair[1] = (char) 0xd834;
        pair[2] = (char) 0xdd60;
        pair[3] = 'b';
        String note = String.valueOf(pair, 0, 4);
        if (note.length() != 4) {
            bad = bad + 1;
        }
        if (note.codePointAt(1) != 0x1d160) {
            bad = bad + 1;
        }
        if (note.codePointAt(0) != 'a') {
            bad = bad + 1;
        }
        if (note.codePointBefore(3) != 0x1d160) {
            bad = bad + 1;
        }
        if (note.codePointCount(0, 4) != 3) {
            bad = bad + 1;
        }
        if (note.offsetByCodePoints(0, 2) != 3) {
            bad = bad + 1;
        }
        if (note.offsetByCodePoints(4, -2) != 1) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Literal replacement, by char and by sequence. */
    public static int reescritura() {
        int bad = 0;
        if (!"banana".replace('a', 'o').equals("bonono")) {
            bad = bad + 1;
        }
        if (!"banana".replace('z', 'o').equals("banana")) {
            bad = bad + 1;
        }
        if (!"banana".replace("na", "NA").equals("baNANA")) {
            bad = bad + 1;
        }
        if (!"banana".replace("ana", "X").equals("bXna")) {
            bad = bad + 1;
        }
        if (!"aaa".replace("aa", "b").equals("ba")) {
            bad = bad + 1;
        }
        if (!"ab".replace("", "-").equals("-a-b-")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** trim against strip: the same word, two different rules. */
    public static int recorte() {
        int bad = 0;
        if (!"  hola  ".trim().equals("hola")) {
            bad = bad + 1;
        }
        if (!"  hola  ".strip().equals("hola")) {
            bad = bad + 1;
        }
        if (!"  hola  ".stripLeading().equals("hola  ")) {
            bad = bad + 1;
        }
        if (!"  hola  ".stripTrailing().equals("  hola")) {
            bad = bad + 1;
        }
        if (!"hola".trim().equals("hola")) {
            bad = bad + 1;
        }
        // trim cuts every control character; strip does not treat this one as space.
        String withNul = "hola";
        if (!withNul.trim().equals("hola")) {
            bad = bad + 1;
        }
        if (!withNul.strip().equals(withNul)) {
            bad = bad + 1;
        }
        // strip cuts a non-ASCII space; trim does not.
        String withEm = " hola ";
        if (!withEm.strip().equals("hola")) {
            bad = bad + 1;
        }
        if (!withEm.trim().equals(withEm)) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The text-block helpers. */
    public static int lineas() {
        int bad = 0;
        if (!"a\nb".indent(2).equals("  a\n  b\n")) {
            bad = bad + 1;
        }
        if (!"  a\n  b\n".indent(-1).equals(" a\n b\n")) {
            bad = bad + 1;
        }
        if (!"".indent(4).equals("")) {
            bad = bad + 1;
        }
        // With a trailing terminator there is an implicit empty last line, whose length is
        // zero, so the common indent is zero and nothing is stripped. Surprising, and real.
        if (!"    a\n      b\n".stripIndent().equals("    a\n      b\n")) {
            bad = bad + 1;
        }
        if (!"    a\n      b".stripIndent().equals("a\n  b")) {
            bad = bad + 1;
        }
        if (!"a\\nb".translateEscapes().equals("a\nb")) {
            bad = bad + 1;
        }
        if (!"a\\tb".translateEscapes().equals("a\tb")) {
            bad = bad + 1;
        }
        if (!"\\101".translateEscapes().equals("A")) {
            bad = bad + 1;
        }
        if (!"a\\\\b".translateEscapes().equals("a\\b")) {
            bad = bad + 1;
        }
        boolean refused = false;
        try {
            String gone = "a\\qb".translateEscapes();
            if (gone != null) {
                bad = bad + 1;
            }
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        if (!refused) {
            bad = bad + 1;
        }
        return bad;
    }

    /** valueOf, copyValueOf and join. */
    public static int fabricas() {
        int bad = 0;
        if (!String.valueOf(true).equals("true") || !String.valueOf(false).equals("false")) {
            bad = bad + 1;
        }
        if (!String.valueOf('x').equals("x")) {
            bad = bad + 1;
        }
        if (!String.valueOf(42).equals("42")) {
            bad = bad + 1;
        }
        if (!String.valueOf(-7L).equals("-7")) {
            bad = bad + 1;
        }
        char[] data = new char[4];
        data[0] = 'h';
        data[1] = 'o';
        data[2] = 'l';
        data[3] = 'a';
        if (!String.valueOf(data).equals("hola")) {
            bad = bad + 1;
        }
        if (!String.copyValueOf(data).equals("hola")) {
            bad = bad + 1;
        }
        if (!String.copyValueOf(data, 1, 2).equals("ol")) {
            bad = bad + 1;
        }
        List<String> parts = new ArrayList<String>();
        parts.add("x");
        parts.add("y");
        if (!String.join("+", parts).equals("x+y")) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * The varargs form of join, on its own: a spread call to a varargs method that comes
     * from the classpath is miscompiled, because ACC_VARARGS is never emitted so the
     * compiler cannot tell the method is variadic (finding #118). The Iterable overload,
     * which is not variadic, is exercised in `fabricas` and works.
     */
    public static int unirVarargs() {
        int bad = 0;
        if (!String.join("-", "a", "b", "c").equals("a-b-c")) {
            bad = bad + 1;
        }
        if (!String.join("-").equals("")) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * The case-insensitive comparator, on its own: reading a static field of another
     * class is emitted as a `getfield` and crashes our VM (finding #110), so this group
     * passes against the JDK and cannot run here. `compareToIgnoreCase` is the same
     * order reached through a method, and it works.
     */
    public static int comparador() {
        int bad = 0;
        if (String.CASE_INSENSITIVE_ORDER.compare("Hola", "hola") != 0) {
            bad = bad + 1;
        }
        if (String.CASE_INSENSITIVE_ORDER.compare("abc", "ABD") >= 0) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * Everything that can run on both sides, so one call answers "does it work".
     *
     * <p>`unirVarargs` and `comparador` are deliberately OUT: both pass against the JDK
     * and are blocked here by a toolchain defect, not by this class.
     */
    public static int todo() {
        return StringTest.rebanadas() + StringTest.busqueda() + StringTest.comparacion()
                + StringTest.caracteres() + StringTest.reescritura() + StringTest.recorte()
                + StringTest.lineas() + StringTest.fabricas();
    }

    public static void main(String[] args) {
        System.out.println("rebanadas    " + StringTest.rebanadas());
        System.out.println("busqueda     " + StringTest.busqueda());
        System.out.println("comparacion  " + StringTest.comparacion());
        System.out.println("caracteres   " + StringTest.caracteres());
        System.out.println("reescritura  " + StringTest.reescritura());
        System.out.println("recorte      " + StringTest.recorte());
        System.out.println("lineas       " + StringTest.lineas());
        System.out.println("fabricas     " + StringTest.fabricas());
        System.out.println("unirVarargs  " + StringTest.unirVarargs());
        System.out.println("comparador   " + StringTest.comparador());
        System.out.println("TOTAL        " + StringTest.todo());
    }
}
