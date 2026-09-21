package java.text.spi;

import java.text.Collator;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.CollatorProvider -- how a language's strings are ordered.
 *
 * <p>A single method, and behind it is cultural ordering, which has nothing to do with comparing
 * code points. Three examples of why:
 *
 * <ul>
 *   <li>In Spanish, n with a tilde goes <b>after</b> n, not at the end of the alphabet where its
 *       code point puts it.
 *   <li>In Swedish, a with a ring goes <b>at the end</b>, after z.
 *   <li>In German, a with a diaeresis sorts as "ae" in a phone book and as "a" in a dictionary: the
 *       <b>same</b> language with two orderings, which is what the locale's Unicode extensions are
 *       for.
 * </ul>
 *
 * <p>That is why a {@code String.compareTo} is never any use for showing somebody a sorted list, and
 * why this provider exists.
 */
public abstract class CollatorProvider extends LocaleServiceProvider {

    protected CollatorProvider() {
    }

    /** That locale's cultural comparator. */
    public abstract Collator getInstance(Locale locale);
}
