package java.awt.im;

/**
 * The character sets that can be requested from an input method.
 *
 * <p>It serves to narrow what the user can type in a field: in one that only accepts numbers it
 * makes no sense for the input method to offer kanji candidates.
 *
 * <p>The four Han subsets —traditional and simplified Hanzi, Kanji, Hanja— are parts of the same
 * Unicode block, each as used in one language, and one character can belong to several of them.
 * (This note said there were three, and that the distinction lies not in which characters they are
 * but in which ones are worth offering first; the JDK defines each as the characters used in its
 * language.)
 *
 * <p>It extends {@code Character.Subset}, which is compared **by identity**: two subsets with the
 * same name are distinct objects and are not equal. That is why the constants here are the ones to
 * use, and not ones of one's own with the same name.
 */
public final class InputSubset extends Character.Subset {

    /** With the given name; private because the subsets that exist are the ones here. */
    private InputSubset(String name) {
        super(name);
    }

    /** Latin letters. */
    public static final InputSubset LATIN = new InputSubset("LATIN");

    /** Latin digits. */
    public static final InputSubset LATIN_DIGITS = new InputSubset("LATIN_DIGITS");

    /** Traditional Han characters. */
    public static final InputSubset TRADITIONAL_HANZI = new InputSubset("TRADITIONAL_HANZI");

    /** Simplified Han characters. */
    public static final InputSubset SIMPLIFIED_HANZI = new InputSubset("SIMPLIFIED_HANZI");

    /** The Han characters used in Japanese. */
    public static final InputSubset KANJI = new InputSubset("KANJI");

    /** The Han characters used in Korean. */
    public static final InputSubset HANJA = new InputSubset("HANJA");

    /** Half-width katakana. */
    public static final InputSubset HALFWIDTH_KATAKANA = new InputSubset("HALFWIDTH_KATAKANA");

    /** Full-width Latin letters. */
    public static final InputSubset FULLWIDTH_LATIN = new InputSubset("FULLWIDTH_LATIN");

    /** Full-width digits. */
    public static final InputSubset FULLWIDTH_DIGITS = new InputSubset("FULLWIDTH_DIGITS");
}
