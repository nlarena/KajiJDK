package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.QuitEvent -- the system asks for the program to be closed.
 *
 * <p>It is handed over by {@link QuitHandler}, together with a {@link QuitResponse}. See there why
 * the answer travels apart.
 */
public final class QuitEvent extends AppEvent {

    private static final long serialVersionUID = -256100795532403146L;

    /** No data: the event is the notice. */
    public QuitEvent() {
    }
}
