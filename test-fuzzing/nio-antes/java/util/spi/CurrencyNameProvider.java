package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.CurrencyNameProvider -- what a currency is called and how it is
 * written.
 *
 * <p>The two methods answer different questions about the same thing: {@link #getSymbol} gives what
 * goes against the number ({@code $}, {@code €}) and {@link #getDisplayName} the name to read
 * ({@code "Argentine peso"}).
 *
 * <p>The symbol depends on the locale <b>doing the looking</b>, not on the currency's: the United
 * States dollar is {@code $} to a reader in the United States and {@code US$} to one in Argentina,
 * where {@code $} already means something else. A provider that returns the symbol "of the currency"
 * and not "for that reader" produces ambiguous amounts.
 */
public abstract class CurrencyNameProvider extends LocaleServiceProvider {

    protected CurrencyNameProvider() {
    }

    /**
     * The symbol that goes against the number.
     *
     * @param currencyCode the three-letter ISO 4217 code, in upper case
     * @return null if this provider does not have it
     * @throws IllegalArgumentException if the code does not have the shape of an ISO 4217 one
     */
    public abstract String getSymbol(String currencyCode, Locale locale);

    /**
     * The name to read.
     *
     * <p>The default returns null, which means "I do not have it": a provider may know the symbol and
     * not the name, and forcing it to invent one would be worse.
     */
    public String getDisplayName(String currencyCode, Locale locale) {
        if (currencyCode == null || locale == null) {
            throw new NullPointerException();
        }
        // The arguments are validated all the same even though none is used: a provider that
        // overrides this method inherits the contract, and the contract says a malformed code is
        // rejected.
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
