package java.lang;

// KajiLibrary's java.lang.AssertionError — what a failed `assert` throws. The wide set of
// constructors is not API bloat: `assert cond : detail` accepts a detail expression of any
// type, and javac picks the overload by that expression's static type, so each primitive
// needs its own. Every one of them only builds the message string — the value is not kept.
//
// The Object overload is the interesting one: if the detail happens to be a Throwable it is
// also wired up as the cause, so `assert x != null : someException` keeps the stack context.
public class AssertionError extends Error {

    public AssertionError() {
    }

    public AssertionError(Object detailMessage) {
        super(String.valueOf(detailMessage));
        if (detailMessage instanceof Throwable) {
            initCause((Throwable) detailMessage);
        }
    }

    public AssertionError(boolean detailMessage) {
        super(stringOf(detailMessage));
    }

    public AssertionError(char detailMessage) {
        super(stringOf(detailMessage));
    }

    public AssertionError(int detailMessage) {
        super(stringOf(detailMessage));
    }

    public AssertionError(long detailMessage) {
        super(stringOf(detailMessage));
    }

    public AssertionError(float detailMessage) {
        super(Float.toString(detailMessage));
    }

    public AssertionError(double detailMessage) {
        super(Double.toString(detailMessage));
    }

    public AssertionError(String message, Throwable cause) {
        super(message, cause);
    }

    // A super(...) argument may not touch `this`, so the primitive → String conversions go
    // through these statics rather than an instance helper.

    private static String stringOf(boolean b) {
        if (b) {
            return "true";
        }
        return "false";
    }

    private static String stringOf(char c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c);
        return sb.toString();
    }

    private static String stringOf(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append(i);
        return sb.toString();
    }

    private static String stringOf(long l) {
        StringBuilder sb = new StringBuilder();
        sb.append(l);
        return sb.toString();
    }
}
