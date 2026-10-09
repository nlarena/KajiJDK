package java.awt;

/**
 * The AWT is in a state there is no coming back from --the toolkit could not be loaded, for
 * example--. It inherits from Error and not from Exception precisely because there is nothing
 * sensible to do on catching it.
 */
public class AWTError extends Error {

    private static final long serialVersionUID = -1819846354050686206L;

    public AWTError(String msg) {
        super(msg);
    }
}
