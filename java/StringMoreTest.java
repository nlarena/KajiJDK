import java.io.UnsupportedEncodingException;
import java.lang.constant.ConstantDesc;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Exercises the second half of java.lang.String — case, regular expressions, streams and the
 * odds and ends. Every method returns the number of things that came out wrong, so 0 is a pass.
 *
 * The same source compiles against the JDK 25, where {@code main} prints the same counts, so
 * every expectation is checked against the real String before it is asked of ours.
 */
public class StringMoreTest {

    /** ASCII case, both directions. */
    public static int caso() {
        int bad = 0;
        if (!"Hola Mundo".toLowerCase().equals("hola mundo")) {
            bad = bad + 1;
        }
        if (!"Hola Mundo".toUpperCase().equals("HOLA MUNDO")) {
            bad = bad + 1;
        }
        if (!"123-abc".toUpperCase().equals("123-ABC")) {
            bad = bad + 1;
        }
        if (!"".toUpperCase().equals("")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Non-ASCII case: accents, Greek, Cyrillic. This is what the tables are for. */
    public static int casoAcentuado() {
        int bad = 0;
        char[] src = new char[3];
        src[0] = (char) 0xc1;
        src[1] = (char) 0xd1;
        src[2] = (char) 0xdc;
        String upper = String.valueOf(src, 0, 3);
        char[] want = new char[3];
        want[0] = (char) 0xe1;
        want[1] = (char) 0xf1;
        want[2] = (char) 0xfc;
        String lower = String.valueOf(want, 0, 3);
        if (!upper.toLowerCase().equals(lower)) {
            bad = bad + 1;
        }
        if (!lower.toUpperCase().equals(upper)) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * The mappings that are NOT one to one.
     *
     * <p>U+00DF is the German sharp s: upper-casing it gives TWO characters, "SS". A
     * character-by-character implementation gets this wrong and says nothing.
     */
    public static int casoEspecial() {
        int bad = 0;
        char[] one = new char[1];
        one[0] = (char) 0xdf;
        String sharpS = String.valueOf(one, 0, 1);
        String up = sharpS.toUpperCase();
        if (up.length() != 2) {
            bad = bad + 1;
        }
        if (!up.equals("SS")) {
            bad = bad + 1;
        }
        // And it grows in the middle of a word, not only alone.
        char[] word = new char[3];
        word[0] = 'a';
        word[1] = (char) 0xdf;
        word[2] = 'b';
        if (!String.valueOf(word, 0, 3).toUpperCase().equals("ASSB")) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * Regular expressions, all of them one line over java.util.regex.
     *
     * <p>Passes against the JDK and cannot run on our VM: `Pattern.compile` alone crashes there,
     * because the regex package reads static fields across compilation units and that is emitted
     * as a `getfield` over a static (finding #110). Nothing to do with this class -- a bare
     * `Pattern.compile("a")` from any file crashes the same way -- which is why it is out of
     * `todo`.
     */
    public static int expresiones() {
        int bad = 0;
        if (!"abc123".matches("[a-z]+[0-9]+")) {
            bad = bad + 1;
        }
        // The WHOLE string has to match, not a part of it: the pattern is happy with the
        // prefix and that is not enough.
        if ("abc123x".matches("[a-z]+[0-9]+")) {
            bad = bad + 1;
        }
        if (!"a1b2c3".replaceAll("[0-9]", "-").equals("a-b-c-")) {
            bad = bad + 1;
        }
        if (!"a1b2c3".replaceFirst("[0-9]", "-").equals("a-b2c3")) {
            bad = bad + 1;
        }
        String[] parts = "a,b,,c".split(",");
        if (parts.length != 4 || !parts[2].equals("")) {
            bad = bad + 1;
        }
        String[] trailing = "a,b,,".split(",");
        if (trailing.length != 2) {
            bad = bad + 1;
        }
        String[] kept = "a,b,,".split(",", -1);
        if (kept.length != 4) {
            bad = bad + 1;
        }
        String[] limited = "a,b,c".split(",", 2);
        if (limited.length != 2 || !limited[1].equals("b,c")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The streams. */
    public static int flujos() {
        int bad = 0;
        IntStream cs = "abc".chars();
        int[] got = cs.toArray();
        if (got.length != 3 || got[0] != 'a' || got[2] != 'c') {
            bad = bad + 1;
        }
        // A surrogate pair is two chars and one code point.
        char[] pair = new char[3];
        pair[0] = 'a';
        pair[1] = (char) 0xd834;
        pair[2] = (char) 0xdd60;
        String note = String.valueOf(pair, 0, 3);
        if (note.chars().toArray().length != 3) {
            bad = bad + 1;
        }
        int[] cps = note.codePoints().toArray();
        if (cps.length != 2 || cps[1] != 0x1d160) {
            bad = bad + 1;
        }
        Stream<String> ls = "a\nb\r\nc".lines();
        Object[] lines = ls.toArray();
        if (lines.length != 3) {
            bad = bad + 1;
        }
        if (!lines[2].equals("c")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The odds and ends. */
    public static int variados() {
        int bad = 0;
        byte[] out = new byte[5];
        out[0] = 9;
        out[4] = 9;
        "hola".getBytes(1, 3, out, 1);
        if (out[0] != 9 || out[1] != 'o' || out[2] != 'l' || out[4] != 9) {
            bad = bad + 1;
        }
        // A traves de la INTERFAZ, que es lo que ejercita el puente covariante: el metodo que
        // ve quien llama devuelve Object, y lo emite el compilador, no esta fuente.
        ConstantDesc asDesc = "hola";
        try {
            Object resolved = asDesc.resolveConstantDesc(null);
            if (!resolved.equals("hola")) {
                bad = bad + 1;
            }
        } catch (ReflectiveOperationException impossible) {
            bad = bad + 1;
        }
        Optional<String> desc = "hola".describeConstable();
        if (desc == null || !desc.isPresent()) {
            bad = bad + 1;
        }
        if (!desc.get().equals("hola")) {
            bad = bad + 1;
        }
        return bad;
    }


    /**
     * The charset-aware {@code getBytes} family.
     *
     * <p>Passes against the JDK and cannot run on our VM, for the same reason the whole of
     * java.nio.charset cannot: reaching {@code StandardCharsets.UTF_8} -- or any of the coder
     * constants -- is a cross-unit static read, emitted as a {@code getfield} over a static
     * (finding #110). Nothing to do with this class, which is why it is out of {@code todo}.
     */
    public static int bytes() {
        int bad = 0;
        byte[] ascii = "abc".getBytes(StandardCharsets.UTF_8);
        if (ascii.length != 3 || ascii[0] != 'a' || ascii[2] != 'c') {
            bad = bad + 1;
        }
        // U+00E9: one char, two bytes in UTF-8 and one in Latin-1. The difference between a
        // charset-aware conversion and a truncating one is exactly this.
        char[] one = new char[1];
        one[0] = (char) 0xe9;
        String acute = String.valueOf(one, 0, 1);
        byte[] utf8 = acute.getBytes(StandardCharsets.UTF_8);
        if (utf8.length != 2 || (utf8[0] & 0xff) != 0xc3 || (utf8[1] & 0xff) != 0xa9) {
            bad = bad + 1;
        }
        byte[] latin = acute.getBytes(StandardCharsets.ISO_8859_1);
        if (latin.length != 1 || (latin[0] & 0xff) != 0xe9) {
            bad = bad + 1;
        }
        // ...and what will not fit is replaced, not reported.
        byte[] narrowed = acute.getBytes(StandardCharsets.US_ASCII);
        if (narrowed.length != 1 || narrowed[0] != '?') {
            bad = bad + 1;
        }
        // The no-argument form is the default charset, which is UTF-8.
        if (acute.getBytes().length != 2) {
            bad = bad + 1;
        }
        // A supplementary character: two chars in, four bytes out.
        char[] pair = new char[2];
        pair[0] = (char) 0xd834;
        pair[1] = (char) 0xdd60;
        String note = String.valueOf(pair, 0, 2);
        if (note.getBytes(StandardCharsets.UTF_8).length != 4) {
            bad = bad + 1;
        }
        if (note.getBytes(StandardCharsets.UTF_16BE).length != 4) {
            bad = bad + 1;
        }
        // The empty string encodes to nothing, and each call hands back a FRESH array.
        if ("".getBytes(StandardCharsets.UTF_8).length != 0) {
            bad = bad + 1;
        }
        if ("abc".getBytes(StandardCharsets.UTF_8) == "abc".getBytes(StandardCharsets.UTF_8)) {
            bad = bad + 1;
        }
        // By name, including an alias and the wrong case.
        boolean threw = false;
        try {
            byte[] named = "abc".getBytes("utf8");
            if (named.length != 3) {
                bad = bad + 1;
            }
        } catch (UnsupportedEncodingException unexpected) {
            bad = bad + 1;
        }
        // An unknown name is the CHECKED exception, not the unchecked one Charset would throw.
        try {
            "abc".getBytes("no-such-charset");
            threw = false;
        } catch (UnsupportedEncodingException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        // ...and so is a malformed one.
        try {
            "abc".getBytes("!!illegal!!");
            threw = false;
        } catch (UnsupportedEncodingException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * Everything that can run on both sides, so one call answers "does it work".
     *
     * <p>`expresiones` and `bytes` are deliberately out: both pass against the JDK and both
     * are blocked here by finding #110, not by this class.
     */
    public static int todo() {
        return StringMoreTest.caso() + StringMoreTest.casoAcentuado()
                + StringMoreTest.casoEspecial() + StringMoreTest.flujos()
                + StringMoreTest.variados();
    }

    public static void main(String[] args) {
        System.out.println("caso            " + StringMoreTest.caso());
        System.out.println("casoAcentuado   " + StringMoreTest.casoAcentuado());
        System.out.println("casoEspecial    " + StringMoreTest.casoEspecial());
        System.out.println("expresiones     " + StringMoreTest.expresiones());
        System.out.println("flujos          " + StringMoreTest.flujos());
        System.out.println("bytes           " + StringMoreTest.bytes());
        System.out.println("variados        " + StringMoreTest.variados());
        System.out.println("TOTAL           " + StringMoreTest.todo());
    }
}
