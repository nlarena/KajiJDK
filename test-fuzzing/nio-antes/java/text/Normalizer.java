package java.text;

/**
 * Transforms text into a normalized form, so that canonically equivalent strings compare equal.
 *
 * <p>Unicode lets the same text be spelled more than one way. The character "á" can be a single
 * code point (U+00E1) or a letter followed by a combining accent (U+0061 U+0301); both render
 * identically and mean the same thing, and {@code String.equals} says they are different. Any code
 * that compares, sorts, or keys on user-supplied text has to normalize first or it will treat one
 * word as two.
 *
 * <pre>
 *     String a = "á";              // one code point
 *     String b = "á";             // two
 *     a.equals(b);                                          // false
 *     Normalizer.normalize(a, Form.NFC)
 *         .equals(Normalizer.normalize(b, Form.NFC));       // true
 * </pre>
 *
 * @implNote A KajiLibrary subset: only the CANONICAL forms, NFD and NFC. The compatibility forms
 *           NFKD and NFKC are not declared at all rather than declared and left throwing — they
 *           need a second decomposition table of 3848 more entries, which is a follow-up and not a
 *           wall. Omitting the constants means a caller that needs them gets a compile error, which
 *           is the honest signal.
 */
public final class Normalizer {

    private Normalizer() {
    }

    /**
     * The normalization forms.
     *
     * @implNote Declared as a NESTED enum, matching the JDK, which was not obviously possible here:
     *           a nested type does not resolve through a qualified name or a cross-file import
     *           (finding #101). The same-file simple-name case does work, and the emitted descriptor
     *           is {@code Ljava/text/Normalizer$Form;} as it should be — verified before this class
     *           was written, because the whole public API depends on it.
     */
    public enum Form {

        /**
         * Canonical decomposition: every composed character is split into its parts, and the marks
         * are put in canonical order.
         */
        NFD,

        /**
         * Canonical decomposition followed by canonical composition. The result is the "most
         * composed" spelling, which is what most text is stored in.
         */
        NFC
    }

    /**
     * Normalizes a sequence of characters into the given form.
     *
     * @param src the text to normalize
     * @param form the form to normalize to
     * @return the normalized text
     * @throws NullPointerException if either argument is {@code null}
     */
    public static String normalize(CharSequence src, Form form) {
        if (src == null || form == null) {
            throw new NullPointerException();
        }
        if (form == Form.NFD) {
            return NormImpl.decompose(src);
        }
        return NormImpl.compose(src);
    }

    /**
     * Reports whether a sequence of characters is already in the given form.
     *
     * @param src the text to test
     * @param form the form to test against
     * @return {@code true} if normalizing would not change the text
     * @throws NullPointerException if either argument is {@code null}
     * @implSpec Normalizes and compares. The JDK has a quick-check table that can answer "certainly
     *           yes" or "certainly no" without transforming; that table is a separate extraction and
     *           only affects speed, never the answer.
     */
    public static boolean isNormalized(CharSequence src, Form form) {
        if (src == null || form == null) {
            throw new NullPointerException();
        }
        return Normalizer.normalize(src, form).equals(src.toString());
    }
}
