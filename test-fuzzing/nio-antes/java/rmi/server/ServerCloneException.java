package java.rmi.server;

/**
 * A failure to clone an exported remote object.
 *
 * <p>Cloning one of these is not copying fields: the copy has to <strong>be exported too</strong>,
 * because a remote object that is not exported is not reachable. That export is what can fail, and
 * that is why there is a cloning exception of its own.
 *
 * <p>The public field {@link #detail} and the two methods that use it predate {@link Throwable}
 * having causes. They are kept for compatibility; {@link #getCause} returns the same thing.
 */
public class ServerCloneException extends CloneNotSupportedException {

    private static final long serialVersionUID = 6617456357664815945L;

    /** The cause, in the old form. */
    public Exception detail;

    /** With a message. */
    public ServerCloneException(String s) {
        super(s);
    }

    /** With a message and the cause. */
    public ServerCloneException(String s, Exception cause) {
        super(s);
        this.detail = cause;
    }

    /** The message, with the cause's appended if there is one. */
    public String getMessage() {
        if (this.detail == null) {
            return super.getMessage();
        }
        return super.getMessage() + "; nested exception is: "
                + this.detail.toString();
    }

    /** The cause; it is {@link #detail}. */
    public Throwable getCause() {
        return this.detail;
    }
}
