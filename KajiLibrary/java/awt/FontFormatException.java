package java.awt;

/**
 * The file passed to {@code Font.createFont} is not a font in a format that is understood.
 *
 * <p>It is a checked exception, and its signature mentions no type of the window system, so it
 * depends on nothing. (This note said it was written although {@code Font} did not exist yet; it
 * exists now.)
 */
public class FontFormatException extends Exception {

    private static final long serialVersionUID = -4481290147811361272L;

    public FontFormatException(String reason) {
        super(reason);
    }
}
