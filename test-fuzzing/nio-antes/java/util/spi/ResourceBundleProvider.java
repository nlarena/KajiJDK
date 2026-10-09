package java.util.spi;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * KajiLibrary's java.util.spi.ResourceBundleProvider -- where the translated texts come from.
 *
 * <p>It is the modern route for packaging translations: instead of {@code ResourceBundle.getBundle}
 * looking for files on the classpath by name convention, a module declares that it provides such a
 * package's texts and this method hands them over.
 *
 * <p>The practical difference is that the by-convention search has to <b>guess</b> -- try
 * {@code Msg_es_AR}, then {@code Msg_es}, then {@code Msg} -- whereas a provider knows what it has.
 * That is why it can return null at no cost: it is "I do not have it", not "I did not find it".
 */
public interface ResourceBundleProvider {

    /**
     * The bundle by that name for that locale, or null if this provider does not have it.
     *
     * @param baseName the bundle's <b>full</b> name, with the package
     */
    ResourceBundle getBundle(String baseName, Locale locale);
}
