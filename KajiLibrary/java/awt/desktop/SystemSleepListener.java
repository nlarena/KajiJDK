package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.SystemSleepListener -- escucha la suspension de la maquina.
 *
 * <p>Se registra con {@code Desktop.addAppEventListener}. Ver {@link SystemSleepEvent}.
 */
public interface SystemSleepListener extends SystemEventListener {

    /** La maquina se va a suspender. */
    void systemAboutToSleep(SystemSleepEvent e);

    /** La maquina desperto. */
    void systemAwoke(SystemSleepEvent e);
}
