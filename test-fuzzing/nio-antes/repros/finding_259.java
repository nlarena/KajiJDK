/**
 * A SUPPLEMENTARY character (above U+FFFF) in a string literal is written into the constant
 * pool as raw UTF-8, not as MODIFIED UTF-8, so the class file cannot be read back.
 *
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_259.java
 *   bin/jvm.exe --javap KajiLibrary/repros/finding_259.class
 *
 * Expected: the class disassembles.
 * Actual:   `error parsing ...: invalid modified UTF-8 in a Utf8 constant`, and
 *           `run-headless` refuses to load it with `load class: BadUtf8`.
 *
 * §4.4.7 is explicit that CONSTANT_Utf8 uses the MODIFIED encoding: a code point above U+FFFF
 * is written as its UTF-16 surrogate PAIR, each surrogate encoded in three bytes, six in all.
 * Standard UTF-8 would write the code point directly in four bytes, and that is what comes out.
 *
 * The compiler accepts the source and emits a class file it cannot read back, which makes this
 * worse than #228 -- there the surrogate ESCAPE (`"𝅘𝅥𝅮"`) is rejected outright, so at
 * least the author is told. Between the two there is no way to put a supplementary character in
 * a string at all.
 *
 * `soloBMP` is the control: every character below U+FFFF, accents included, is encoded fine.
 */
public class finding_259 {

    /** A single supplementary character: U+1D160, a musical note. */
    public static int suplementario() {
        String note = "𝅘𝅥𝅮";
        return note.length();
    }

    /** Control: the BMP, including non-ASCII. */
    public static int soloBMP() {
        String s = "aceituna con ene";
        return s.length();
    }
}
