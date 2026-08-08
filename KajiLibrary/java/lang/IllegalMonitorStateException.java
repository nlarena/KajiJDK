package java.lang;

// KajiLibrary's java.lang.IllegalMonitorStateException — thrown when `wait`/`notify` is
// called on an object whose monitor the current thread does not hold.
public class IllegalMonitorStateException extends RuntimeException {

    public IllegalMonitorStateException() {
    }

    public IllegalMonitorStateException(String message) {
        super(message);
    }
}
