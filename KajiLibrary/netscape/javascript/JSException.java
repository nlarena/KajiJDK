package netscape.javascript;

/**
 * What comes out when the JavaScript engine on the other side fails.
 *
 * <p>It is a {@link RuntimeException} and not a checked one, and that is not carelessness: the
 * methods of {@link JSObject} name a member or evaluate an expression <em>by its name in text</em>,
 * so any of them can fail for reasons the Java compiler has no way of seeing. Forcing it to be
 * declared on every call would add no safety, only noise.
 *
 * @deprecated the bridge from applets to JavaScript was left with no uses when the applet model
 *     fell out of use. It still lives because {@link JSObject} names it in every signature.
 */
@Deprecated(since = "9", forRemoval = true)
public class JSException extends RuntimeException {

    private static final long serialVersionUID = -7132931832235736974L;

    /** With no detail: the engine failed and did not say why. */
    public JSException() {
        super();
    }

    /** With the message the engine gave. */
    public JSException(String s) {
        super(s);
    }

    /**
     * Wrapping what really failed.
     *
     * <p>The cause is kept whole: if the engine threw something of its own, it is underneath and is
     * read with {@link Throwable#getCause}.
     */
    public JSException(Throwable cause) {
        super(cause);
    }
}
