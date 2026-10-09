package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppReopenedEvent -- someone launched the program again while it was
 * already running.
 *
 * <p>It is handed over by {@link AppReopenedListener}. Instead of starting a second copy, the desktop
 * tells the one already there; the usual answer is to show the main window.
 */
public final class AppReopenedEvent extends AppEvent {

    private static final long serialVersionUID = 1503238361530407990L;

    /** No data: the event is the notice. */
    public AppReopenedEvent() {
    }
}
