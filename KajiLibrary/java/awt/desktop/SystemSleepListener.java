package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.SystemSleepListener -- listens for the machine suspending.
 *
 * <p>It is registered with {@code Desktop.addAppEventListener}. See {@link SystemSleepEvent}.
 */
public interface SystemSleepListener extends SystemEventListener {

    /** The machine is about to suspend. */
    void systemAboutToSleep(SystemSleepEvent e);

    /** The machine woke up. */
    void systemAwoke(SystemSleepEvent e);
}
