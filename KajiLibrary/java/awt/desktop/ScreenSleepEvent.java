package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.ScreenSleepEvent -- the screen is about to go off, or came back on.
 *
 * <p>It is handed over by {@link ScreenSleepListener}. It is the screen alone: the machine keeps
 * running, so what should be paused is what is drawn and not what is computed. That is what
 * {@link SystemSleepEvent} is for.
 */
public final class ScreenSleepEvent extends AppEvent {

    private static final long serialVersionUID = 7521606180376544150L;

    /** No data: the event is the notice. */
    public ScreenSleepEvent() {
    }
}
