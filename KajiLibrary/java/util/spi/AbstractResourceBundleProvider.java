package java.util.spi;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * KajiLibrary's java.util.spi.AbstractResourceBundleProvider -- the provider that already knows how
 * to build the name.
 *
 * <p>It implements {@link ResourceBundleProvider} by doing the one thing almost everybody needs:
 * building the resource's name out of the base name and the locale, and loading it. A concrete
 * provider normally only declares <b>which formats</b> it handles and writes nothing else.
 *
 * <h2>The name is built with underscores, and the gaps count</h2>
 *
 * <p>{@link #toBundleName} produces {@code Msg_es_AR} for {@code ("Msg", es-AR)}: language, script,
 * country and variant, in that order, separated by {@code _}. A root locale gives the bare base
 * name.
 *
 * <p>The two cases that are not obvious are the <b>gaps</b>. With a script, the language is written
 * even when it is empty -- otherwise {@code Msg_Latn_AR} would be indistinguishable from
 * {@code Msg_es_AR}. And a variant with no country leaves the gap in plain sight:
 * {@code Msg_es__POSIX}, with two underscores.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #getBundle} <b>loads nothing</b> and returns null. Loading from a provider asks for the
 * module system: the resource has to be looked for <b>in the provider's module</b>, which is exactly
 * what distinguishes this route from the old one, and this library does not have it. Null is what
 * the contract defines as "I do not have it", so a caller falls back to the by-convention search
 * without finding anything odd -- which is better than returning a bundle taken from the classpath
 * and saying it came from the module.
 *
 * <p>{@link #toBundleName} IS implemented for real: it is string arithmetic and depends on
 * nothing.
 */
public abstract class AbstractResourceBundleProvider implements ResourceBundleProvider {

    private final String[] formats;

    /** With no formats declared. */
    protected AbstractResourceBundleProvider() {
        this.formats = new String[0];
    }

    /**
     * With the formats this provider handles: {@code "java.class"}, {@code "java.properties"}.
     *
     * @throws IllegalArgumentException if one of them is not one of those two
     */
    protected AbstractResourceBundleProvider(String... formats) {
        if (formats == null) {
            throw new NullPointerException("formats is null");
        }
        int i = 0;
        while (i < formats.length) {
            if (!"java.class".equals(formats[i]) && !"java.properties".equals(formats[i])) {
                throw new IllegalArgumentException("unknown format: " + formats[i]);
            }
            i = i + 1;
        }
        String[] copy = new String[formats.length];
        System.arraycopy(formats, 0, copy, 0, formats.length);
        this.formats = copy;
    }

    /**
     * The resource's name for that base name and that locale.
     *
     * <p>See the class's note for the parts' order and the two gaps.
     */
    protected String toBundleName(String baseName, Locale locale) {
        if (Locale.ROOT.equals(locale)) {
            return baseName;
        }
        String language = locale.getLanguage();
        String script = locale.getScript();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        if (language.length() == 0 && script.length() == 0 && country.length() == 0) {
            return baseName;
        }
        StringBuilder sb = new StringBuilder(baseName);
        sb.append("_");
        if (script.length() > 0) {
            // The language goes in even when it is empty: see the class's note.
            sb.append(language).append("_").append(script);
            if (country.length() > 0) {
                sb.append("_").append(country);
                if (variant.length() > 0) {
                    sb.append("_").append(variant);
                }
            }
            return sb.toString();
        }
        sb.append(language);
        if (country.length() > 0) {
            sb.append("_").append(country);
            if (variant.length() > 0) {
                sb.append("_").append(variant);
            }
        } else if (variant.length() > 0) {
            sb.append("__").append(variant);
        }
        return sb.toString();
    }

    /** The formats declared on construction. A copy. */
    protected final String[] declaredFormats() {
        String[] copy = new String[this.formats.length];
        System.arraycopy(this.formats, 0, copy, 0, this.formats.length);
        return copy;
    }

    /** It returns null: see the class's note. */
    public ResourceBundle getBundle(String baseName, Locale locale) {
        return null;
    }
}
