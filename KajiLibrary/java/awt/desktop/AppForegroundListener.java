package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppForegroundListener -- escucha los cambios de primer plano.
 *
 * <p>Se registra con {@code Desktop.addAppEventListener}. Ver {@link AppForegroundEvent}.
 */
public interface AppForegroundListener extends SystemEventListener {

    /** Paso a primer plano. */
    void appRaisedToForeground(AppForegroundEvent e);

    /** Se fue al fondo. */
    void appMovedToBackground(AppForegroundEvent e);
}
