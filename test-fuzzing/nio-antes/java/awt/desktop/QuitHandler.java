package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.QuitHandler -- decides whether the program closes.
 *
 * <p>It is registered with {@code Desktop.setQuitHandler}. It is the package's only handler that can
 * <b>refuse</b>, and that is why its method receives a {@link QuitResponse} apart.
 *
 * <p>See there why the answer is not the return value.
 */
public interface QuitHandler {

    /**
     * The system wants to close the program.
     *
     * <p>Either {@code performQuit} or {@code cancelQuit} has to be called on the response, sooner or
     * later. Calling neither leaves the system waiting.
     */
    void handleQuitRequestWith(QuitEvent e, QuitResponse response);
}
