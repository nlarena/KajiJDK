package java.text.spi;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.DateFormatSymbolsProvider -- las palabras de una fecha.
 *
 * <p>Nombres de meses y de dias, AM/PM, eras, nombres de zona. Es la mitad <b>lexica</b> de dar
 * formato a una fecha; la otra --en que orden van y con que separadores-- la da
 * {@link DateFormatProvider}. Estan separadas porque se cambian por separado: alguien puede querer
 * los meses abreviados de otra forma sin tocar el orden.
 */
public abstract class DateFormatSymbolsProvider extends LocaleServiceProvider {

    protected DateFormatSymbolsProvider() {
    }

    /** Las palabras de ese local. */
    public abstract DateFormatSymbols getInstance(Locale locale);
}
