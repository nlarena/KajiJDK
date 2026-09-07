package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.ScreenSleepListener -- listens for the screen going off.
 *
 * <p>It is registered with {@code Desktop.addAppEventListener}. See {@link ScreenSleepEvent}.
 */
public interface ScreenSleepListener extends SystemEventListener {

    /** The screen is about to go off. */
    void screenAboutToSleep(ScreenSleepEvent e);

    /** The screen came back on. */
    void screenAwoke(ScreenSleepEvent e);
}
