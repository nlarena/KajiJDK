package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.LocaleNameProvider -- what a language is called <b>in another
 * language</b>.
 *
 * <p>Each method's two arguments are the code being translated and the locale <b>it is written
 * in</b>: {@code getDisplayLanguage("de", Locale.forLanguageTag("es"))} is {@code "aleman"} and with
 * {@code "fr"} it is {@code "allemand"}. Confusing them gives the list of languages each written in
 * its own, which is exactly what a language picker does not want.
 *
 * <p>The four with a default return null --"I do not have it"-- and not an empty string: a provider
 * may know the languages and not the Unicode extension types, and returning empty would show up as a
 * gap in the interface.
 */
public abstract class LocaleNameProvider extends LocaleServiceProvider {

    protected LocaleNameProvider() {
    }

    /**
     * The language's name.
     *
     * @param languageCode the ISO 639 code in lower case
     * @return null if this provider does not have it
     */
    public abstract String getDisplayLanguage(String languageCode, Locale locale);

    /**
     * The writing system's name ({@code "Latn"}, {@code "Cyrl"}).
     *
     * <p>With a default because scripts arrived after the rest of the API: an old provider goes on
     * compiling.
     */
    public String getDisplayScript(String scriptCode, Locale locale) {
        return null;
    }

    /**
     * The country's or region's name.
     *
     * @param countryCode the ISO 3166 code in upper case
     * @return null if this provider does not have it
     */
    public abstract String getDisplayCountry(String countryCode, Locale locale);

    /** The variant's name. */
    public abstract String getDisplayVariant(String variant, Locale locale);

    /** The name of a Unicode extension key ({@code "ca"} for calendar). */
    public String getDisplayUnicodeExtensionKey(String key, Locale locale) {
        return null;
    }

    /** The name of a Unicode extension value ({@code "buddhist"} for the key {@code "ca"}). */
    public String getDisplayUnicodeExtensionType(String type, String key, Locale locale) {
        return null;
    }
}
