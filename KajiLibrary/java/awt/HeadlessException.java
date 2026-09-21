package java.awt;

/**
 * Something that needs a keyboard, mouse or screen was asked for in an environment that has none.
 *
 * <p>In KajiLibrary there is no window system, so any {@code java.awt} class that needs one throws
 * this where it would. (This note said that for that reason only the data classes of {@code
 * java.awt} --geometry, colours, layout constants-- are written; components, windows and the
 * toolkit are written too.)
 *
 * <p>The JDK's {@code getMessage()} appends to the message a suffix describing why the environment
 * is headless, built by {@code GraphicsEnvironment.getHeadlessMessage()}. This note said that class
 * does not exist here; it does, and so does the method, but this {@code getMessage()} does not call
 * it and returns the message as is.
 */
public class HeadlessException extends UnsupportedOperationException {

    private static final long serialVersionUID = 167183644944358563L;

    public HeadlessException() {
    }

    public HeadlessException(String msg) {
        super(msg);
    }

    public String getMessage() {
        return super.getMessage();
    }
}
