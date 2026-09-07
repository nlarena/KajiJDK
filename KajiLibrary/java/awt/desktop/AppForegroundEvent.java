package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppForegroundEvent -- the program came to the foreground or went to
 * the background.
 *
 * <p>It is handed over by {@link AppForegroundListener}, which has one method for each direction; the
 * event itself does not say which of the two it was.
 *
 * <p>It serves to slow down animations or polling when nobody is looking.
 */
public final class AppForegroundEvent extends AppEvent {

    private static final long serialVersionUID = -5513582555740533911L;

    /** No data: the event is the notice. */
    public AppForegroundEvent() {
    }
}
