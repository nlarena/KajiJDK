package java.text.spi;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.NumberFormatProvider -- how a number is written.
 *
 * <p>Four mandatory forms and one with a default, and the four are genuinely different and not just
 * decoratively so: the currency one puts the symbol where that culture puts it --before in English,
 * after in French-- and with the decimals that currency uses; the integer one rounds instead of
 * truncating; the percentage one multiplies by a hundred.
 *
 * <p>{@link #getCompactNumberInstance} has a default and it throws: it is the one that writes
 * {@code "1,2 M"} instead of {@code "1200000"}, it arrived long after the rest, and an old provider
 * that does not know it has to go on compiling. Throwing --instead of returning the ordinary
 * format-- is the right thing: returning {@code "1200000"} where the compact form was asked for
 * breaks the layout of whoever asked for it, and does so in silence.
 */
public abstract class NumberFormatProvider extends LocaleServiceProvider {

    protected NumberFormatProvider() {
    }

    /** With the currency symbol, where that culture puts it. */
    public abstract NumberFormat getCurrencyInstance(Locale locale);

    /** Integer. It rounds, it does not truncate. */
    public abstract NumberFormat getIntegerInstance(Locale locale);

    /** The general-purpose one. */
    public abstract NumberFormat getNumberInstance(Locale locale);

    /** Percentage: it multiplies by a hundred and adds the sign. */
    public abstract NumberFormat getPercentInstance(Locale locale);

    /**
     * The compact form.
     *
     * @throws UnsupportedOperationException by default; see the class's note for why it throws
     *     instead of falling back to the ordinary format
     */
    public NumberFormat getCompactNumberInstance(Locale locale, NumberFormat.Style formatStyle) {
        throw new UnsupportedOperationException(
            "The " + getClass().getName() + " should override this method");
    }
}
