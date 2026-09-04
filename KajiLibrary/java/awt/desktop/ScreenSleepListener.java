package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.ScreenSleepListener -- escucha el apagado de pantalla.
 *
 * <p>Se registra con {@code Desktop.addAppEventListener}. Ver {@link ScreenSleepEvent}.
 */
public interface ScreenSleepListener extends SystemEventListener {

    /** La pantalla se va a apagar. */
    void screenAboutToSleep(ScreenSleepEvent e);

    /** La pantalla se encendio. */
    void screenAwoke(ScreenSleepEvent e);
}
