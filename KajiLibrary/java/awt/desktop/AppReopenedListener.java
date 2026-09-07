package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppReopenedListener -- listens for relaunches.
 *
 * <p>It is registered with {@code Desktop.addAppEventListener}. See {@link AppReopenedEvent}.
 */
public interface AppReopenedListener extends SystemEventListener {

    /** Someone launched it again. */
    void appReopened(AppReopenedEvent e);
}
