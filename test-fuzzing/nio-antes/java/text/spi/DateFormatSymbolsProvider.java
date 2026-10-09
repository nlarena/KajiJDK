package java.text.spi;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.DateFormatSymbolsProvider -- a date's words.
 *
 * <p>Month and day names, AM/PM, eras, zone names. It is the <b>lexical</b> half of formatting a
 * date; the other --what order they go in and with what separators-- is given by
 * {@link DateFormatProvider}. They are separate because they are changed separately: somebody may
 * want the months abbreviated differently without touching the order.
 */
public abstract class DateFormatSymbolsProvider extends LocaleServiceProvider {

    protected DateFormatSymbolsProvider() {
    }

    /** That locale's words. */
    public abstract DateFormatSymbols getInstance(Locale locale);
}
