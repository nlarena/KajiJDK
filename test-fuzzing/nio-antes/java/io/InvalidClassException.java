package java.io;

import java.io.ObjectStreamException;

// The class on the reading side cannot be used to deserialize what the stream holds:
// serialVersionUID mismatch, no accessible no-arg constructor in a non-serializable
// superclass, and so on. `classname` is a separate field rather than baked into the message
// because the message is composed from both, and a caller wants the class name unparsed.
public class InvalidClassException extends ObjectStreamException {

    public String classname;

    // Same reason as in WriteAbortedException: getMessage() is overridden to prepend the
    // class name, so the plain reason has to be kept here rather than read back from
    // Throwable (our javac has no `super.` method calls yet).
    private String reason;

    public InvalidClassException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public InvalidClassException(String classname, String reason) {
        super(reason);
        this.reason = reason;
        this.classname = classname;
    }

    public InvalidClassException(String reason, Throwable cause) {
        super(reason, cause);
        this.reason = reason;
    }

    public InvalidClassException(String classname, String reason, Throwable cause) {
        super(reason, cause);
        this.reason = reason;
        this.classname = classname;
    }

    public String getMessage() {
        if (this.classname == null) {
            return this.reason;
        }
        return this.classname + "; " + this.reason;
    }
}
