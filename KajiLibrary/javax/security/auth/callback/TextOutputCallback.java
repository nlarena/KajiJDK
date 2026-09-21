package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.TextOutputCallback -- the only one that asks nothing.
 *
 * <p>It carries a message to show the user and its severity. It exists for the same reason as the
 * others: a login module that wants to warn "the password expires in three days" cannot write to
 * the terminal, because it does not know whether there is one. It sends it through the same channel
 * as its questions.
 */
public class TextOutputCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = 1689502495511663102L;

    /** An informative notice. */
    public static final int INFORMATION = 0;

    /** A warning. */
    public static final int WARNING = 1;

    /** An error. */
    public static final int ERROR = 2;

    private final int messageType;
    private final String message;

    /**
     * @throws IllegalArgumentException if the type is not one of the three, or the message is null
     *     or empty: an empty message tells nobody anything
     */
    public TextOutputCallback(int messageType, String message) {
        if ((messageType != INFORMATION && messageType != WARNING && messageType != ERROR)
                || message == null || message.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.messageType = messageType;
        this.message = message;
    }

    public int getMessageType() {
        return this.messageType;
    }

    public String getMessage() {
        return this.message;
    }
}
