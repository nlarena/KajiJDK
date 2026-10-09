package java.io;

import java.io.ObjectStreamException;

// Read side of a failed write: the writer hit an exception partway through, so it recorded
// that fact in the stream instead of the object. The reader then throws this, carrying the
// writer's original exception in `detail` — a failure that happened in another place, and
// possibly another process, replayed here.
public class WriteAbortedException extends ObjectStreamException {

    public Exception detail;

    // The plain reason, kept separately because getMessage() is overridden to append the
    // detail and so cannot read the inherited message back (our javac has no `super.` calls).
    private String reason;

    public WriteAbortedException(String message, Exception detail) {
        super(message);
        this.reason = message;
        this.detail = detail;
    }

    public String getMessage() {
        if (this.detail == null) {
            return this.reason;
        }
        return this.reason + "; " + this.detail.toString();
    }

    public Throwable getCause() {
        return this.detail;
    }
}
