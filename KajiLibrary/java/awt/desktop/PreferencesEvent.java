package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.PreferencesEvent -- the user asked to open the preferences.
 *
 * <p>It is handed over by {@link PreferencesHandler}. On the desktops where the preferences menu is
 * disabled by default, registering a handler is what enables it.
 */
public final class PreferencesEvent extends AppEvent {

    private static final long serialVersionUID = -6398607097086476160L;

    /** No data: the event is the notice. */
    public PreferencesEvent() {
    }
}
