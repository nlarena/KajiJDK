package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.CurrencyNameProvider -- como se llama y como se escribe una moneda.
 *
 * <p>Los dos metodos contestan preguntas distintas sobre lo mismo: {@link #getSymbol} da lo que va
 * pegado al numero ({@code $}, {@code €}) y {@link #getDisplayName} el nombre para leer
 * ({@code "peso argentino"}).
 *
 * <p>El simbolo depende del local <b>que mira</b>, no del de la moneda: el dolar estadounidense es
 * {@code $} para un lector de Estados Unidos y {@code US$} para uno de Argentina, donde {@code $} ya
 * significa otra cosa. Un proveedor que devuelva el simbolo "de la moneda" y no "para ese lector"
 * produce importes ambiguos.
 */
public abstract class CurrencyNameProvider extends LocaleServiceProvider {

    protected CurrencyNameProvider() {
    }

    /**
     * El simbolo que va pegado al numero.
     *
     * @param currencyCode el codigo ISO 4217 de tres letras, en mayusculas
     * @return null si este proveedor no lo tiene
     * @throws IllegalArgumentException si el codigo no tiene la forma de un ISO 4217
     */
    public abstract String getSymbol(String currencyCode, Locale locale);

    /**
     * El nombre para leer.
     *
     * <p>El default devuelve null, que quiere decir "no lo tengo": un proveedor puede saber el
     * simbolo y no el nombre, y obligarlo a inventar uno seria peor.
     */
    public String getDisplayName(String currencyCode, Locale locale) {
        if (currencyCode == null || locale == null) {
            throw new NullPointerException();
        }
        // Se validan igual los argumentos aunque no se use ninguno: un proveedor que sobrescriba
        // este metodo hereda el contrato, y el contrato dice que un codigo mal formado se rechaza.
        if (!isAlpha3(currencyCode)) {
            throw new IllegalArgumentException("currencyCode is not a supported ISO 4217 code");
        }
        if (!isSupportedLocale(locale)) {
            throw new IllegalArgumentException("locale is not one of the supported locales");
        }
        return null;
    }

    private static boolean isAlpha3(String s) {
        if (s.length() != 3) {
            return false;
        }
        int i = 0;
        while (i < 3) {
            char c = s.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
            i = i + 1;
        }
        return true;
    }
}
