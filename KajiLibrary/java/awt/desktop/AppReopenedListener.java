package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppReopenedListener -- escucha los relanzamientos.
 *
 * <p>Se registra con {@code Desktop.addAppEventListener}. Ver {@link AppReopenedEvent}.
 */
public interface AppReopenedListener extends SystemEventListener {

    /** Alguien volvio a lanzarlo. */
    void appReopened(AppReopenedEvent e);
}
