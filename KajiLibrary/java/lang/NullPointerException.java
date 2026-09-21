package java.lang;

// KajiLibrary's java.lang.NullPointerException — the VM throws it when code uses a null
// reference (null receiver of a field/method access, null array, etc.).
//
// It overrides getMessage()/fillInStackTrace() because of the JDK's "helpful NPE messages"
// feature: when the exception carries no explicit message, getMessage() lazily asks the VM to
// reconstruct one from the bytecode at the throw site (e.g. `Cannot invoke "..." because "x" is
// null`), and fillInStackTrace() clears that cached message since a fresh trace would describe a
// different site. KajiJDK has no such native reconstruction, so getMessage() falls back to the
// explicit message (null when none was given) and the overrides just keep the JDK's surface.
public class NullPointerException extends RuntimeException {

    // The extended message, computed lazily by the JDK out of the bytecode. With no native
    // reconstruction it stays null for ever.
    private String extendedMessage;

    public NullPointerException() {
    }

    public NullPointerException(String message) {
        super(message);
    }

    public synchronized Throwable fillInStackTrace() {
        // A new stack trace would invalidate the computed extended message: it is discarded.
        this.extendedMessage = null;
        return super.fillInStackTrace();
    }

    public String getMessage() {
        String message = super.getMessage();
        if (message == null) {
            return this.extendedMessage;
        }
        return message;
    }
}
