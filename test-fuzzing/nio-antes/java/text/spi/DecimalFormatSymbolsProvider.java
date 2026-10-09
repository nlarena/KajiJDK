package java.text.spi;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.DecimalFormatSymbolsProvider -- a number's signs.
 *
 * <p>The decimal separator, the thousands one, the minus sign, the percent sign, infinity. The first
 * is the one that breaks things: in much of the world the comma separates decimals and the point
 * groups thousands, the other way round from English. A program that builds numbers by
 * concatenating text produces amounts that mean a thousand times more or a thousand times less
 * depending on who reads them.
 */
public abstract class DecimalFormatSymbolsProvider extends LocaleServiceProvider {

    protected DecimalFormatSymbolsProvider() {
    }

    /** That locale's signs. */
    public abstract DecimalFormatSymbols getInstance(Locale locale);
}
