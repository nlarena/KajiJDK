/**
 * Behaviour test of case and search in java.lang.String, written to run **the same** in this VM
 * and in the real JDK.
 *
 * <p>Each check has an index. {@code run()} returns -1 if they all passed, or the index of the
 * first one that failed, which is enough to compare the two VMs without depending on the console
 * output matching character by character.
 *
 * <p>It exists because differential fuzzing against OpenJDK 25 found three defects here:
 *
 * <ul>
 *   <li>case folding for comparison only handled {@code A-Z}, so every letter outside ASCII
 *       compared as if it had no case at all;
 *   <li>{@code indexOf} of the empty string past the end answered -1 instead of clamping to the
 *       length, the one place where a start out of range did not simply clamp;
 *   <li>{@code toLowerCase} had no final-sigma rule, so Greek words ending in sigma came out
 *       misspelt.
 * </ul>
 *
 * <p>Everything outside ASCII is written as a unicode escape on purpose: the file then says
 * the same thing whatever encoding the reader, the editor or the compiler assumes.
 */
public class StringCaseTest {

    public static int run() {

        // ---- folding for comparison, beyond A-Z ----

        String upperEcole = "\u00c9COLE";
        String lowerEcole = "\u00e9cole";
        if (!upperEcole.equalsIgnoreCase(lowerEcole)) {
            return 1;
        }
        if (!"\u00d1".equalsIgnoreCase("\u00f1")) {
            return 2;
        }
        if (lowerEcole.compareToIgnoreCase(upperEcole) != 0) {
            return 3;
        }
        // U+01C4 DZ with caron has three cases -- upper, title and lower -- that are three
        // different characters. They only fold together if upper is applied before lower, which
        // is what the specification of compareToIgnoreCase asks for.
        if (!"\u01c4".equalsIgnoreCase("\u01c6") || !"\u01c5".equalsIgnoreCase("\u01c6")) {
            return 4;
        }
        if (!"\u03a3\u039f\u03a6".equalsIgnoreCase("\u03c3\u03bf\u03c6")) {
            return 5;
        }
        if (!upperEcole.regionMatches(true, 0, lowerEcole, 0, 5)) {
            return 6;
        }
        if (String.CASE_INSENSITIVE_ORDER.compare("\u00c1B", "\u00e1b") != 0) {
            return 7;
        }

        // Controls: ASCII must fold as it always did, and different letters must stay different.
        if (!"abc".equalsIgnoreCase("ABC")) {
            return 8;
        }
        if ("abc".equalsIgnoreCase("abd")) {
            return 9;
        }
        if (lowerEcole.equalsIgnoreCase("ecole")) {
            return 10;
        }
        if ("abc".compareToIgnoreCase("ABD") >= 0) {
            return 11;
        }

        // ---- indexOf of the empty string ----

        // The empty string sits between every pair of characters and also after the last one.
        if ("hola".indexOf("", 99) != 4) {
            return 12;
        }
        if ("hola".indexOf("", 4) != 4) {
            return 13;
        }
        if ("hola".indexOf("", 2) != 2) {
            return 14;
        }
        if ("hola".indexOf("", -3) != 0) {
            return 15;
        }
        if ("".indexOf("", 5) != 0) {
            return 16;
        }
        if ("hola".indexOf("") != 0) {
            return 17;
        }

        // Controls: a needle that is not empty is still not found past the end.
        if ("hola".indexOf("a", 99) != -1) {
            return 18;
        }
        if ("hola".indexOf("la") != 2) {
            return 19;
        }
        if ("hola".indexOf("x") != -1) {
            return 20;
        }
        if ("hola".lastIndexOf("") != 4) {
            return 21;
        }

        // ---- the final sigma ----

        // Greek writes the same letter as U+03C2 at the end of a word and U+03C3 elsewhere, so
        // lowering depends on the neighbours -- the only place in the class where it does.
        if (!"\u039f\u0394\u039f\u03a3".toLowerCase().equals("\u03bf\u03b4\u03bf\u03c2")) {
            return 22;
        }
        if (!"\u03a3\u039f\u03a6\u039f\u03a3".toLowerCase()
                .equals("\u03c3\u03bf\u03c6\u03bf\u03c2")) {
            return 23;
        }
        if (!"\u0391\u03a3\u0398\u039c\u0391".toLowerCase()
                .equals("\u03b1\u03c3\u03b8\u03bc\u03b1")) {
            return 24;
        }
        // On its own, or after something that is not a letter, it is not word-final.
        if (!"\u03a3".toLowerCase().equals("\u03c3")) {
            return 25;
        }
        if (!" \u03a3".toLowerCase().equals(" \u03c3")) {
            return 26;
        }
        if (!"\u039f\u03a3 \u039f\u03a3".toLowerCase().equals("\u03bf\u03c2 \u03bf\u03c2")) {
            return 27;
        }

        // Controls: the rest of Greek, the round trip back up, the mapping that changes length,
        // and plain ASCII.
        if (!"\u0391\u0392\u0393".toLowerCase().equals("\u03b1\u03b2\u03b3")) {
            return 28;
        }
        if (!"\u03bf\u03b4\u03bf\u03c2".toLowerCase().equals("\u03bf\u03b4\u03bf\u03c2")) {
            return 29;
        }
        if (!"\u03bf\u03b4\u03bf\u03c2".toUpperCase().equals("\u039f\u0394\u039f\u03a3")) {
            return 30;
        }
        if (!"\u00df".toUpperCase().equals("SS")) {
            return 31;
        }
        if (!"HoLa".toLowerCase().equals("hola") || !"HoLa".toUpperCase().equals("HOLA")) {
            return 32;
        }

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(StringCaseTest.run());
    }
}
