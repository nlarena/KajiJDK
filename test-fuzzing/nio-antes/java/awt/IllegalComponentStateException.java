package java.awt;

/**
 * A component was asked for something it cannot answer in its current state --the on-screen
 * position of something not on screen yet, for example--.
 *
 * <p>It inherits from {@code IllegalStateException} and adds nothing: it exists only so that
 * whoever catches can tell the AWT case from the rest.
 */
public class IllegalComponentStateException extends IllegalStateException {

    private static final long serialVersionUID = -1889339587208144238L;

    public IllegalComponentStateException() {
        super();
    }

    public IllegalComponentStateException(String s) {
        super(s);
    }
}
