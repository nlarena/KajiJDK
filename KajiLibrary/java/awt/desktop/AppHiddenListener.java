package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppHiddenListener -- escucha cuando el programa se oculta.
 *
 * <p>Se registra con {@code Desktop.addAppEventListener}. Ver {@link AppHiddenEvent}.
 */
public interface AppHiddenListener extends SystemEventListener {

    /** Se oculto. */
    void appHidden(AppHiddenEvent e);

    /** Se volvio a mostrar. */
    void appUnhidden(AppHiddenEvent e);
}
