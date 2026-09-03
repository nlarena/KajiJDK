package java.text.spi;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.DecimalFormatSymbolsProvider -- los signos de un numero.
 *
 * <p>El separador decimal, el de miles, el signo de menos, el de porcentaje, el infinito. El primero
 * es el que rompe cosas: en gran parte del mundo la coma separa decimales y el punto agrupa miles,
 * al reves que en ingles. Un programa que arma numeros concatenando texto produce importes que
 * significan mil veces mas o mil veces menos segun quien los lea.
 */
public abstract class DecimalFormatSymbolsProvider extends LocaleServiceProvider {

    protected DecimalFormatSymbolsProvider() {
    }

    /** Los signos de ese local. */
    public abstract DecimalFormatSymbols getInstance(Locale locale);
}
