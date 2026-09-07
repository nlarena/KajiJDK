package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.SystemSleepEvent -- the machine is about to suspend, or woke up.
 *
 * <p>It is handed over by {@link SystemSleepListener}. Unlike {@link ScreenSleepEvent}, here
 * everything stops: connections have to be closed and pending work saved, because on waking the clock
 * has jumped and the open connections are almost certainly dead.
 *
 * <p>The advance notice arrives with little margin and the system does not wait: whatever does not
 * get done, does not get done.
 */
public final class SystemSleepEvent extends AppEvent {

    private static final long serialVersionUID = 11372269824930549L;

    /** No data: the event is the notice. */
    public SystemSleepEvent() {
    }
}
