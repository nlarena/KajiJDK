package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.UserSessionListener -- escucha los cambios de sesion de usuario.
 *
 * <p>Se registra con {@code Desktop.addAppEventListener}. Ver {@link UserSessionEvent}.
 */
public interface UserSessionListener extends SystemEventListener {

    /** La sesion dejo de estar activa. */
    void userSessionDeactivated(UserSessionEvent e);

    /** La sesion volvio a estar activa. */
    void userSessionActivated(UserSessionEvent e);
}
