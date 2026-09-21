package java.text.spi;

import java.text.DateFormat;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.DateFormatProvider -- how a date or a time is written.
 *
 * <p>The styles --{@code FULL}, {@code LONG}, {@code MEDIUM}, {@code SHORT}-- are not "more or less
 * long": they are <b>four different formats</b> that each culture defines for itself. The United
 * States' short one is month/day/year and almost everywhere else's is day/month/year, so the same
 * string {@code "03/04/2026"} is two different dates depending on who reads it. There is no way of
 * getting it right without knowing the locale, and that is the whole point of this class.
 */
public abstract class DateFormatProvider extends LocaleServiceProvider {

    protected DateFormatProvider() {
    }

    /**
     * The time alone.
     *
     * @param style one of {@code DateFormat}'s four
     */
    public abstract DateFormat getTimeInstance(int style, Locale locale);

    /** The date alone. */
    public abstract DateFormat getDateInstance(int style, Locale locale);

    /** Both, each with its own style. */
    public abstract DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale locale);
}
