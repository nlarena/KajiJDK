package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.LocaleServiceProvider -- the root of the locale data providers.
 *
 * <p>It is what lets an application add support for a language the runtime does not ship, or correct
 * the one it does. Everything language-dependent in {@code java.util} and {@code java.text} --month
 * names, currency symbols, collation rules-- can be replaced by this route, and none of the classes
 * that use them has to find out.
 *
 * <h2>Why isSupportedLocale exists besides getAvailableLocales</h2>
 *
 * <p>It looks redundant and it is not. {@code getAvailableLocales()} returns a <b>finite</b> list,
 * and there are locales that cannot be enumerated: the ones carrying Unicode extensions
 * ({@code es-AR-u-ca-buddhist}) form an infinite set. {@link #isSupportedLocale}'s default compares
 * against the list after stripping the extensions, which is right for almost everybody; a provider
 * that can answer by extension overrides it.
 *
 * <p><b>This library registers no provider.</b> The classes are here so that one somebody writes
 * fits.
 */
public abstract class LocaleServiceProvider {

    protected LocaleServiceProvider() {
    }

    /**
     * The locales this provider has data for.
     *
     * <p>It may or may not include {@code Locale.ROOT}; what it cannot do is return null.
     */
    public abstract Locale[] getAvailableLocales();

    /**
     * Whether this provider serves that locale.
     *
     * <p>The default looks the locale up in {@link #getAvailableLocales()} <b>without its
     * extensions</b>. See the class's note for why the list alone is not enough.
     */
    public boolean isSupportedLocale(Locale locale) {
        // Without extensions: `es-AR-u-ca-buddhist` is served by the same provider as `es-AR`.
        locale = locale.stripExtensions();
        Locale[] available = getAvailableLocales();
        int i = 0;
        while (i < available.length) {
            if (locale.equals(available[i].stripExtensions())) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
