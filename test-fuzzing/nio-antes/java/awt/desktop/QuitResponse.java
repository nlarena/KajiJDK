package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.QuitResponse -- the answer to a request to close.
 *
 * <p>It exists because the decision to close may take time. If {@link QuitHandler} returned a
 * boolean, it would have to be decided <b>on the spot</b>; with this, the handler can put up a "save
 * changes", come straight back, and answer once the user replies.
 *
 * <p>What has to be honoured is simple and gets forgotten: <b>one of the two methods has to be
 * called</b>. If not, the system is left waiting for an answer that never comes, and on several
 * desktops that leaves the shutdown dialog hanging.
 *
 * <p>{@link #performQuit} does not return: it closes the program.
 */
public interface QuitResponse {

    /** Go ahead. Does not return. */
    void performQuit();

    /** Do not close. */
    void cancelQuit();
}
