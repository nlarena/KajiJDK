package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppForegroundListener -- listens for foreground changes.
 *
 * <p>It is registered with {@code Desktop.addAppEventListener}. See {@link AppForegroundEvent}.
 */
public interface AppForegroundListener extends SystemEventListener {

    /** It came to the foreground. */
    void appRaisedToForeground(AppForegroundEvent e);

    /** It went to the background. */
    void appMovedToBackground(AppForegroundEvent e);
}
