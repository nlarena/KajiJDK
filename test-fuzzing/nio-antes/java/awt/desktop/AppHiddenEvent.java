package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppHiddenEvent -- the program was hidden or shown again.
 *
 * <p>It is handed over by {@link AppHiddenListener}. Being hidden is not the same as being in the
 * background: a hidden program has no visible window at all, one in the background does.
 */
public final class AppHiddenEvent extends AppEvent {

    private static final long serialVersionUID = 2637465279476429224L;

    /** No data: the event is the notice. */
    public AppHiddenEvent() {
    }
}
