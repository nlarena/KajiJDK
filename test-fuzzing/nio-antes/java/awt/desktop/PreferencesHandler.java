package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.PreferencesHandler -- answers the preferences request.
 *
 * <p>It is registered with {@code Desktop.setPreferencesHandler}. There can be only one: unlike the
 * {@link SystemEventListener}s, this is not a notice but a responsibility, and it would make no sense
 * for two parts of the program to take it.
 */
public interface PreferencesHandler {

    /** Opens the program's preferences. */
    void handlePreferences(PreferencesEvent e);
}
