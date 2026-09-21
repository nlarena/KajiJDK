package javax.swing;

/**
 * That graphical look and feel does not serve on this platform.
 *
 * <h2>Why it is checked and not a run-time one</h2>
 *
 * <p>Because it is not a mistake of the program: Windows' native look and feel exists and it is
 * right to ask for it, and on Linux there is no way of giving it. Whoever asks for it has to
 * have a plan for that case -- falling back on the cross-platform one -- and a checked exception
 * is what forces them to write it.
 *
 * @since 1.2
 */
public class UnsupportedLookAndFeelException extends Exception {

    private static final long serialVersionUID = -6096987026804165577L;

    /**
     * With that reason.
     *
     * @param s why it does not serve
     */
    public UnsupportedLookAndFeelException(String s) {
        super(s);
    }
}
