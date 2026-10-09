package java.net;

import java.io.IOException;

// An HTTP request has to be repeated, but it cannot be done automatically.
//
// The typical case: a POST with a body was sent, the server answered 3xx, and retrying would mean
// sending the body again -- which may be an already consumed stream, or an operation that is not
// idempotent. Instead of deciding for the caller, the JDK aborts and hands over the data the caller
// would need in order to decide: the code, the reason, and where it was redirecting to.
//
// Hence the three accessors: without them the exception would say "retry" without saying what or
// where, and would be of no use.
public class HttpRetryException extends IOException {

    private static final long serialVersionUID = -9186022286469111381L;

    private final int responseCode;
    private final String location;

    public HttpRetryException(String detail, int code) {
        super(detail);
        this.responseCode = code;
        this.location = null;
    }

    public HttpRetryException(String detail, int code, String location) {
        super(detail);
        this.responseCode = code;
        this.location = location;
    }

    public int responseCode() {
        return this.responseCode;
    }

    // It is the detail message, not a separate field: the JDK reuses `getMessage()` here. It is left
    // as it is because the public contract is "the reason", and the reason is what was passed as the
    // detail.
    public String getReason() {
        return super.getMessage();
    }

    /** The response's {@code Location}, or null if there was none. */
    public String getLocation() {
        return this.location;
    }
}
