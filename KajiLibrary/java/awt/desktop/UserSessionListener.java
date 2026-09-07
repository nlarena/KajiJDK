package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.UserSessionListener -- listens for user session changes.
 *
 * <p>It is registered with {@code Desktop.addAppEventListener}. See {@link UserSessionEvent}.
 */
public interface UserSessionListener extends SystemEventListener {

    /** The session stopped being active. */
    void userSessionDeactivated(UserSessionEvent e);

    /** The session became active again. */
    void userSessionActivated(UserSessionEvent e);
}
