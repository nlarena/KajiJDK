package java.util.spi;

import java.util.ResourceBundle;

/**
 * KajiLibrary's java.util.spi.ResourceBundleControlProvider -- it changes how bundles are looked up.
 *
 * <p>It is the <b>old</b> route, and it is discouraged for a concrete reason: a
 * {@code ResourceBundle.Control} changes the lookup strategy for <b>the whole process</b>, so two
 * libraries with different strategies collide and whichever loaded first wins.
 * {@link ResourceBundleProvider} solves the same thing without that global effect, because there
 * each module provides its own and nobody decides for the rest.
 *
 * <p>It is still in the API because there is code that uses it. A provider returns null for the names
 * it does not care about, and there the default strategy applies.
 */
public interface ResourceBundleControlProvider {

    /**
     * The {@code Control} for that bundle name, or null not to intervene.
     *
     * @param baseName the bundle's full name
     */
    ResourceBundle.Control getControl(String baseName);
}
