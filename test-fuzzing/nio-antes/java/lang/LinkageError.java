package java.lang;

// KajiLibrary's java.lang.LinkageError — an Error in a class's dependency: something it
// needs failed to link (was missing, incompatible, or malformed). Parent of the
// resolution failures below.
public class LinkageError extends Error {

    public LinkageError() {
    }

    public LinkageError(String message) {
        super(message);
    }

    public LinkageError(String message, Throwable cause) {
        super(message, cause);
    }
}
