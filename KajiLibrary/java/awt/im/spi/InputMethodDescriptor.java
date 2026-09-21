package java.awt.im.spi;

import java.awt.AWTException;
import java.awt.Image;
import java.util.Locale;

/**
 * The record card of an {@link InputMethod}: what can be known about it **without loading it**.
 *
 * <p>That is the whole reason it exists. A system can have many input methods installed, and
 * building the menu to choose them cannot cost loading and starting each one: that would be paying
 * for all of them to use one. The descriptor is lightweight --name, icon, locales-- and the method
 * is created only when someone chooses it.
 *
 * <p>It is discovered through {@link java.util.ServiceLoader}: an input method is installed by
 * declaring its descriptor as a provider of this service.
 */
public interface InputMethodDescriptor {

    /**
     * The locales this input method supports.
     *
     * <p>A locale in the list does not promise that it is available right now --it may depend on a
     * dictionary installed separately-- but it does promise that {@link InputMethod#setLocale}
     * makes sense for it.
     *
     * @throws AWTException if it could not be found out
     */
    Locale[] getAvailableLocales() throws AWTException;

    /**
     * Whether the list of locales can change while the program runs.
     *
     * <p>With `true`, the framework asks again instead of keeping the first answer: it is the case
     * of a method whose dictionaries can be installed on the fly.
     */
    boolean hasDynamicLocaleList();

    /**
     * The name to display, in the language it is to be displayed in.
     *
     * @param inputLocale the locale that would be written with it, or `null` if it does not matter
     * @param displayLanguage the language the name is wanted in
     */
    String getInputMethodDisplayName(Locale inputLocale, Locale displayLanguage);

    /**
     * The icon, 16x16, or `null` if it has none.
     *
     * @param inputLocale the locale that would be written with it, or `null` if it does not matter
     */
    Image getInputMethodIcon(Locale inputLocale);

    /**
     * Creates the input method.
     *
     * <p>It is the only expensive point, and that is why it is separate from the rest of the
     * interface.
     *
     * @throws Exception whatever fails when creating it; the framework reports it and goes on with
     *     the other installed methods
     */
    InputMethod createInputMethod() throws Exception;
}
