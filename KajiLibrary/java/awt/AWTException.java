package java.awt;

/**
 * Something in the AWT went wrong in a way the caller has to deal with: it is a checked exception,
 * not a programming error.
 */
public class AWTException extends Exception {

    private static final long serialVersionUID = -1900414231151323879L;

    public AWTException(String msg) {
        super(msg);
    }
}
