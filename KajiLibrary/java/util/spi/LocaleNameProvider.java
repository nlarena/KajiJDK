package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.LocaleNameProvider -- como se llama un idioma <b>en otro idioma</b>.
 *
 * <p>Los dos argumentos de cada metodo son el codigo que se traduce y el local <b>en el que se lo
 * escribe</b>: {@code getDisplayLanguage("de", Locale.forLanguageTag("es"))} es {@code "aleman"} y
 * con {@code "fr"} es {@code "allemand"}. Confundirlos da la lista de idiomas escrita cada uno en el
 * suyo, que es justo lo que un selector de idioma no quiere.
 *
 * <p>Los cuatro con default devuelven null --"no lo tengo"-- y no una cadena vacia: un proveedor
 * puede conocer los idiomas y no los tipos de extension Unicode, y devolver vacio se mostraria como
 * un hueco en la interfaz.
 */
public abstract class LocaleNameProvider extends LocaleServiceProvider {

    protected LocaleNameProvider() {
    }

    /**
     * El nombre del idioma.
     *
     * @param languageCode el codigo ISO 639 en minusculas
     * @return null si este proveedor no lo tiene
     */
    public abstract String getDisplayLanguage(String languageCode, Locale locale);

    /**
     * El nombre del sistema de escritura ({@code "Latn"}, {@code "Cyrl"}).
     *
     * <p>Con default porque los scripts llegaron despues que el resto del API: un proveedor viejo
     * sigue compilando.
     */
    public String getDisplayScript(String scriptCode, Locale locale) {
        return null;
    }

    /**
     * El nombre del pais o region.
     *
     * @param countryCode el codigo ISO 3166 en mayusculas
     * @return null si este proveedor no lo tiene
     */
    public abstract String getDisplayCountry(String countryCode, Locale locale);

    /** El nombre de la variante. */
    public abstract String getDisplayVariant(String variant, Locale locale);

    /** El nombre de una clave de extension Unicode ({@code "ca"} para calendario). */
    public String getDisplayUnicodeExtensionKey(String key, Locale locale) {
        return null;
    }

    /** El nombre de un valor de extension Unicode ({@code "buddhist"} para la clave {@code "ca"}). */
    public String getDisplayUnicodeExtensionType(String type, String key, Locale locale) {
        return null;
    }
}
