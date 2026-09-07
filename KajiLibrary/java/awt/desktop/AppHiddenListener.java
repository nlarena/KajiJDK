package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppHiddenListener -- listens for the program being hidden.
 *
 * <p>It is registered with {@code Desktop.addAppEventListener}. See {@link AppHiddenEvent}.
 */
public interface AppHiddenListener extends SystemEventListener {

    /** It was hidden. */
    void appHidden(AppHiddenEvent e);

    /** It was shown again. */
    void appUnhidden(AppHiddenEvent e);
}
