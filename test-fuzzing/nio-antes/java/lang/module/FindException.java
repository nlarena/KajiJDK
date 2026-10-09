package java.lang.module;

// KajiLibrary's java.lang.module.FindException -- thrown when locating a module fails.
public class FindException extends RuntimeException {

    public FindException() {
    }

    public FindException(String msg) {
        super(msg);
    }

    public FindException(Throwable cause) {
        super(cause);
    }

    public FindException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
