package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.QuitStrategy -- what to do when the system asks to close.
 *
 * <p>It is set with {@code Desktop.setQuitStrategy} and decides what happens <b>after</b> a
 * {@link QuitHandler} accepts the closing.
 *
 * <p>The difference between the two matters: {@link #CLOSE_ALL_WINDOWS} sends a closing event to each
 * window, so each one can save what is its own; {@link #NORMAL_EXIT} calls {@code System.exit(0)}
 * straight away and the windows never find out.
 */
public enum QuitStrategy {

    /** Calls {@code System.exit(0)}. The windows never find out. */
    NORMAL_EXIT,

    /** Sends a closing event to each window. */
    CLOSE_ALL_WINDOWS
}
