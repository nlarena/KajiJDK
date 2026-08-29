package java.text;

import java.util.Comparator;
import java.util.Locale;

/**
 * Compares strings the way a human sorting a list would, rather than the way {@code String}
 * does.
 *
 * <p>{@link String#compareTo} compares UTF-16 code units, which is fast, stable, and wrong for
 * every purpose a person can see: it puts {@code "Z"} before {@code "a"}, and
 * {@code "r\u00e9sum\u00e9"} nowhere near {@code "resume"}. What a reader expects instead is a
 * <em>layered</em> comparison, and that layering is the whole design of this class:
 *
 * <ul>
 * <li><b>Primary</b> -- different base letters. {@code a} vs {@code b}. Always significant.</li>
 * <li><b>Secondary</b> -- same letter, different accent. {@code e} vs {@code \u00e9}.</li>
 * <li><b>Tertiary</b> -- same letter and accent, different case. {@code a} vs {@code A}.</li>
 * <li><b>Identical</b> -- everything else, down to the exact code points.</li>
 * </ul>
 *
 * <p>A comparison walks the levels in order and returns at the first one that differs. Setting
 * {@link #setStrength} to {@code PRIMARY} makes it stop early, which is how a search box is made
 * to treat {@code "resume"} and {@code "R\u00c9SUM\u00c9"} as the same word -- the levels are not decoration,
 * they are the knob that says how picky "equal" should be.
 *
 * <p>The class implements {@code Comparator<Object>}, so a collator can be handed straight to a
 * sort. When the same list is sorted repeatedly, {@link #getCollationKey} is the faster route: it
 * pays the analysis once per string instead of once per comparison.
 *
 * @implNote A KajiLibrary subset. {@code clone()} is omitted -- the JDK implements it with
 *           {@code super.clone()} and {@code Object.clone} does not exist in this library; an
 *           abstract class cannot copy a subclass it does not know. {@link RuleBasedCollator}
 *           carries its own {@code clone()}, which is where the JDK's real work happens anyway.
 *
 * @implNote The locale factories return one rule set for every locale. Real per-locale tailoring
 *           (Swedish sorting {@code v} and {@code w} together, Spanish once treating {@code ch} as
 *           a letter) is a matter of rule <em>strings</em>, not of new code: pass one to
 *           {@code new RuleBasedCollator(...)} and the machinery is already there.
 */
public abstract class Collator implements Comparator<Object>, Cloneable {

    /**
     * Only base-letter differences are significant.
     */
    public static final int PRIMARY = 0;

    /**
     * Base letters and accents are significant; case is not.
     */
    public static final int SECONDARY = 1;

    /**
     * Base letters, accents and case are all significant. The default.
     */
    public static final int TERTIARY = 2;

    /**
     * Everything is significant, down to the exact code points.
     */
    public static final int IDENTICAL = 3;

    /**
     * Do not decompose accented characters before comparing. Fastest, and correct only for text
     * already in a composed form.
     */
    public static final int NO_DECOMPOSITION = 0;

    /**
     * Decompose canonically before comparing, so that a precomposed {@code \u00e9} and a decomposed
     * {@code e}+{@code \u0301} compare equal. The default.
     */
    public static final int CANONICAL_DECOMPOSITION = 1;

    /**
     * Decompose canonically and compatibly, so that {@code \ufb01} also matches {@code fi}.
     */
    public static final int FULL_DECOMPOSITION = 2;

    private int strength;
    private int decmp;

    /**
     * For subclasses. Starts at TERTIARY strength with canonical decomposition, the JDK's defaults.
     */
    protected Collator() {
        // 2 and 1 rather than TERTIARY and CANONICAL_DECOMPOSITION: with the frozen compiler a
        // `static final` primitive read from its own class is emitted as a `getstatic` against a
        // field that only exists as a `ConstantValue`, and reads back 0 at runtime (finding #112).
        // The DECLARATIONS above are the API and stay; the reads are spelled out until `bin/` is
        // refreshed, so the defaults are the ones documented rather than two zeroes.
        this.strength = 2;
        this.decmp = 1;
    }

    /**
     * Returns a collator for the default locale.
     *
     * @return a new collator
     */
    public static Collator getInstance() {
        return Collator.getInstance(Locale.getDefault());
    }

    /**
     * Returns a collator for the given locale.
     *
     * @param desiredLocale accepted and, in this subset, not consulted
     * @return a new collator
     */
    public static Collator getInstance(Locale desiredLocale) {
        return RuleBasedCollator.defaultCollator();
    }

    /**
     * Compares two strings under this collator's rules.
     *
     * @param source the first string
     * @param target the second string
     * @return negative, zero or positive as {@code source} sorts before, with, or after
     *         {@code target}
     */
    public abstract int compare(String source, String target);

    /**
     * The {@link Comparator} entry point; both arguments must be strings.
     *
     * @param o1 the first string
     * @param o2 the second string
     * @return the same answer as {@link #compare(String, String)}
     * @throws ClassCastException if either argument is not a {@code String}
     */
    public int compare(Object o1, Object o2) {
        return this.compare((String) o1, (String) o2);
    }

    /**
     * Reduces a string to a sort key, for when the same strings are compared many times.
     *
     * @param source the string
     * @return its key under this collator's current rules and strength
     */
    public abstract CollationKey getCollationKey(String source);

    /**
     * Reports whether two strings compare equal under this collator -- which, below
     * {@code IDENTICAL} strength, is a weaker question than {@code String.equals}.
     *
     * @param source the first string
     * @param target the second string
     * @return {@code true} if they sort together
     */
    public boolean equals(String source, String target) {
        return this.compare(source, target) == 0;
    }

    /**
     * Returns the current strength.
     *
     * @return one of {@code PRIMARY}, {@code SECONDARY}, {@code TERTIARY}, {@code IDENTICAL}
     */
    public synchronized int getStrength() {
        return this.strength;
    }

    /**
     * Sets how picky comparison is.
     *
     * @param newStrength one of the four strength constants
     * @throws IllegalArgumentException if it is not one of them
     */
    public synchronized void setStrength(int newStrength) {
        if (newStrength < 0 || newStrength > 3) {
            throw new IllegalArgumentException("Incorrect comparison level.");
        }
        this.strength = newStrength;
    }

    /**
     * Returns the current decomposition mode.
     *
     * @return one of the three decomposition constants
     */
    public synchronized int getDecomposition() {
        return this.decmp;
    }

    /**
     * Sets how much normalization happens before comparison.
     *
     * @param decompositionMode one of the three decomposition constants
     * @throws IllegalArgumentException if it is not one of them
     */
    public synchronized void setDecomposition(int decompositionMode) {
        if (decompositionMode < 0 || decompositionMode > 2) {
            throw new IllegalArgumentException("Wrong decomposition mode.");
        }
        this.decmp = decompositionMode;
    }

    /**
     * Returns the locales for which collation rules are installed.
     *
     * @return the supported locales
     * @implNote One rule set serves every locale here, so the honest answer is the one locale whose
     *           rules were actually written down.
     */
    public static Locale[] getAvailableLocales() {
        return new Locale[] {Locale.US};
    }

    /**
     * Two collators are equal when they are the same class and carry the same settings.
     *
     * @param that the object to compare with
     * @return {@code true} if they would sort identically as far as this class can tell
     */
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (!(that instanceof Collator)) {
            return false;
        }
        Collator other = (Collator) that;
        return this.strength == other.strength && this.decmp == other.decmp;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Left abstract, exactly as the JDK does: a collator's identity is its rule table,
     *           which lives in the subclass, and a base-class hash over two small ints would be a
     *           worse answer than no answer.
     */
    public abstract int hashCode();
}
