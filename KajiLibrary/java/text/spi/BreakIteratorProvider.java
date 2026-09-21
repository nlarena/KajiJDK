package java.text.spi;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.BreakIteratorProvider -- where a text may be cut.
 *
 * <p>The four methods are four different questions, and the one almost always needed is the one
 * least used:
 *
 * <ul>
 *   <li><b>character</b> -- the boundaries of a <b>perceived character</b>, which is neither a
 *       {@code char} nor a code point: a vowel with a combining accent, or an emoji with a skin-tone
 *       modifier, are several code points and one single character to whoever reads them. It is the
 *       one to use for moving a cursor or cutting a string without splitting a symbol.
 *   <li><b>word</b> -- word boundaries. Japanese and Thai have no spaces, so this is genuinely
 *       language-dependent and cannot be approximated with a {@code split}.
 *   <li><b>line</b> -- where a line <b>may</b> be broken. It does not break: it says where one
 *       could.
 *   <li><b>sentence</b> -- sentence boundaries, which is not "cut at the full stop": a full stop in
 *       an abbreviation ends nothing.
 * </ul>
 *
 * <p>A provider returning null for a locale it declared it supports breaks the contract; for the
 * ones it does not support, the runtime does not even call it.
 */
public abstract class BreakIteratorProvider extends LocaleServiceProvider {

    protected BreakIteratorProvider() {
    }

    /** Word boundaries. See the class's note: several languages have no spaces. */
    public abstract BreakIterator getWordInstance(Locale locale);

    /** Where a line <b>may</b> be broken. It does not break by itself. */
    public abstract BreakIterator getLineInstance(Locale locale);

    /** The boundaries of a perceived character, which is not a {@code char}. See the class's note. */
    public abstract BreakIterator getCharacterInstance(Locale locale);

    /** Sentence boundaries, taking abbreviations into account. */
    public abstract BreakIterator getSentenceInstance(Locale locale);
}
