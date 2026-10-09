package javax.swing;

/**
 * What to do when the user closes a window.
 *
 * <p>It is a constants-only interface, and it is implemented by {@link JFrame}, `JDialog` and
 * `JInternalFrame` so that the constants can be named unqualified from a subclass. It is an old
 * pattern -- today it would be done with an `enum` -- and it survives because changing it would
 * break everything compiled against it.
 *
 * <p>The four are mutually exclusive: the window does only one of these things.
 */
public interface WindowConstants {

    /** Do nothing: the program decides, listening to the closing event. */
    int DO_NOTHING_ON_CLOSE = 0;

    /** Hide it. It goes on existing and can be shown again. */
    int HIDE_ON_CLOSE = 1;

    /**
     * Hide it and release its native resources. It cannot be shown again without recreating them.
     */
    int DISPOSE_ON_CLOSE = 2;

    /**
     * End the program.
     *
     * <p>Only for an application's main window: in an applet or in an embedded component it takes
     * whatever hosts it down with it.
     */
    int EXIT_ON_CLOSE = 3;
}
