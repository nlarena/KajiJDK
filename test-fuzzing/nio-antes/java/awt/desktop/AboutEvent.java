package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AboutEvent -- the user asked to see the {@code About} box.
 *
 * <p>It is handed over by {@link AboutHandler}. Registering one makes the system use the program's
 * own box instead of the one the desktop puts together.
 */
public final class AboutEvent extends AppEvent {

    private static final long serialVersionUID = -5987180734802756477L;

    /** No data: the event is the notice. */
    public AboutEvent() {
    }
}
