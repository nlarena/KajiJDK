package java.text;

import java.util.Locale;

/**
 * Finds the places in text where it is legal to cut: between characters, words, sentences, or
 * lines.
 *
 * <p>Every one of those looks trivial in English and is not. A "character" the user can delete with
 * one backspace may be several {@code char}s -- a surrogate pair, or a letter followed by combining
 * accents. A "word" is not "text between spaces", because {@code "isn't"} is one word and
 * {@code "hello,world"} is two. A "sentence" does not simply end at a period, because
 * {@code "Mr. Smith"} does not. A line break may not fall just anywhere a space appears. So the
 * class exists to put those rules in one place, behind an interface that is deliberately
 * <em>positional</em> rather than list-returning:
 *
 * <pre>
 *     BreakIterator it = BreakIterator.getWordInstance();
 *     it.setText("hello, world");
 *     int start = it.first();
 *     for (int end = it.next(); end != BreakIterator.DONE; end = it.next()) {
 *         // [start, end) is one segment
 *         start = end;
 *     }
 * </pre>
 *
 * <p>The cursor shape is what lets a text editor ask "where does the word under the caret end?"
 * without segmenting the whole document, which is the case the class is designed for.
 *
 * <p>All four kinds are obtained from static factories rather than constructors, because which
 * rules apply depends on the locale, and the caller should not have to know which class implements
 * them.
 *
 * @implNote A KajiLibrary subset. The JDK loads its rules from locale data compiled into the
 *           runtime image; this implementation carries one rule set, applied to every locale, and
 *           the locale-taking factories accept the argument and ignore it -- which is what the JDK
 *           itself does for a locale it has no data for. The rules implemented are documented on
 *           the package-private implementation class.
 *
 * @implNote {@code clone()} is omitted. The JDK declares it concrete and implements it as
 *           {@code super.clone()}; {@code Object.clone} does not exist in this library, and an
 *           abstract class cannot copy a subclass it does not know. Declaring it abstract instead
 *           would be a different API -- a subset is legal, a changed signature is not.
 */
public abstract class BreakIterator implements Cloneable {

    /**
     * Returned by {@link #next}, {@link #previous}, {@link #following} and {@link #preceding} when
     * there is no boundary in the requested direction.
     *
     * <p>It is {@code -1} and not, say, the text length, because it has to be distinguishable from
     * a real offset, and {@code -1} is the only integer no offset can take.
     */
    public static final int DONE = -1;

    /**
     * For subclasses.
     */
    protected BreakIterator() {
    }

    /**
     * Moves the cursor to the first boundary, which is always the start of the text.
     *
     * @return the first boundary offset
     */
    public abstract int first();

    /**
     * Moves the cursor to the last boundary, which is always the end of the text.
     *
     * @return the last boundary offset
     */
    public abstract int last();

    /**
     * Moves the cursor {@code n} boundaries forward, or backward if {@code n} is negative.
     *
     * @param n how many boundaries to move
     * @return the offset landed on, or {@link #DONE} if that runs off either end
     */
    public abstract int next(int n);

    /**
     * Moves the cursor to the next boundary.
     *
     * @return the offset landed on, or {@link #DONE} if the cursor was already at the end
     */
    public abstract int next();

    /**
     * Moves the cursor to the previous boundary.
     *
     * @return the offset landed on, or {@link #DONE} if the cursor was already at the start
     */
    public abstract int previous();

    /**
     * Moves the cursor to the first boundary strictly after {@code offset}.
     *
     * @param offset the offset to search from
     * @return that boundary, or {@link #DONE} if there is none
     * @throws IllegalArgumentException if {@code offset} is outside the text
     */
    public abstract int following(int offset);

    /**
     * Moves the cursor to the last boundary strictly before {@code offset}.
     *
     * @param offset the offset to search from
     * @return that boundary, or {@link #DONE} if there is none
     * @throws IllegalArgumentException if {@code offset} is outside the text
     * @implSpec Defined in terms of {@link #following}: walk forward from the beginning and keep
     *           the last boundary that is still below {@code offset}. Concrete here -- and abstract
     *           for {@code following} -- because a rule set only has to know how to move forward.
     */
    public int preceding(int offset) {
        // -1 rather than DONE: with the frozen compiler a `static final` primitive read from its
        // own class is emitted as a `getstatic` against a field that exists only as a
        // `ConstantValue`, and evaluates to 0 at runtime (finding #112) -- which would make this
        // loop compare against 0 and never terminate correctly. The DECLARATION of DONE above is
        // the API and stays; only the reads are spelled out, and they go back when `bin/` is
        // refreshed.
        int position = this.following(offset);
        while (position >= offset && position != -1) {
            position = this.previous();
        }
        return position;
    }

    /**
     * Reports whether {@code offset} is a boundary. The start of the text always is; the end always
     * is.
     *
     * @param offset the offset to test
     * @return {@code true} if a segment begins or ends there
     * @throws IllegalArgumentException if {@code offset} is outside the text
     */
    public boolean isBoundary(int offset) {
        if (offset == 0) {
            return true;
        }
        return this.following(offset - 1) == offset;
    }

    /**
     * Returns the offset the cursor is currently on.
     *
     * @return the current offset
     */
    public abstract int current();

    /**
     * Returns the text being scanned.
     *
     * @return the iterator over the text
     */
    public abstract CharacterIterator getText();

    /**
     * Sets the text to scan and resets the cursor to the start.
     *
     * @param newText the text
     */
    public void setText(String newText) {
        this.setText(new StringCharacterIterator(newText));
    }

    /**
     * Sets the text to scan and resets the cursor to the start.
     *
     * @param newText the text
     */
    public abstract void setText(CharacterIterator newText);

    /**
     * Returns an iterator over word boundaries for the default locale.
     *
     * @return a new word iterator
     */
    public static BreakIterator getWordInstance() {
        return BreakIterator.getWordInstance(Locale.getDefault());
    }

    /**
     * Returns an iterator over word boundaries.
     *
     * @param locale accepted and, in this subset, not consulted
     * @return a new word iterator
     */
    public static BreakIterator getWordInstance(Locale locale) {
        return SimpleBreakIterator.word();
    }

    /**
     * Returns an iterator over line-break opportunities for the default locale.
     *
     * @return a new line iterator
     */
    public static BreakIterator getLineInstance() {
        return BreakIterator.getLineInstance(Locale.getDefault());
    }

    /**
     * Returns an iterator over line-break opportunities.
     *
     * @param locale accepted and, in this subset, not consulted
     * @return a new line iterator
     */
    public static BreakIterator getLineInstance(Locale locale) {
        return SimpleBreakIterator.line();
    }

    /**
     * Returns an iterator over user-perceived characters for the default locale.
     *
     * @return a new character iterator
     */
    public static BreakIterator getCharacterInstance() {
        return BreakIterator.getCharacterInstance(Locale.getDefault());
    }

    /**
     * Returns an iterator over user-perceived characters.
     *
     * @param locale accepted and, in this subset, not consulted
     * @return a new character iterator
     */
    public static BreakIterator getCharacterInstance(Locale locale) {
        return SimpleBreakIterator.character();
    }

    /**
     * Returns an iterator over sentence boundaries for the default locale.
     *
     * @return a new sentence iterator
     */
    public static BreakIterator getSentenceInstance() {
        return BreakIterator.getSentenceInstance(Locale.getDefault());
    }

    /**
     * Returns an iterator over sentence boundaries.
     *
     * @param locale accepted and, in this subset, not consulted
     * @return a new sentence iterator
     */
    public static BreakIterator getSentenceInstance(Locale locale) {
        return SimpleBreakIterator.sentence();
    }

    /**
     * Returns the locales for which break rules are installed.
     *
     * @return the supported locales
     * @implNote One rule set serves every locale here, so the honest answer is the one locale whose
     *           rules were actually written down.
     */
    public static Locale[] getAvailableLocales() {
        return new Locale[] {Locale.US};
    }
}
