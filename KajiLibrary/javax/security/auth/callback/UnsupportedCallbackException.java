package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.UnsupportedCallbackException -- that callback cannot
 * be answered.
 *
 * <p>It carries <b>which one</b>, and not only a message: whoever catches it usually has several
 * callbacks in flight and needs to know which one was left unanswered to decide whether it can go
 * on without it. See {@link CallbackHandler} for the difference from an input/output failure.
 */
public class UnsupportedCallbackException extends Exception {

    private static final long serialVersionUID = -6873556327310220378L;

    private final Callback callback;

    public UnsupportedCallbackException(Callback callback) {
        super();
        this.callback = callback;
    }

    public UnsupportedCallbackException(Callback callback, String msg) {
        super(msg);
        this.callback = callback;
    }

    /** The callback that could not be answered. */
    public Callback getCallback() {
        return this.callback;
    }
}
