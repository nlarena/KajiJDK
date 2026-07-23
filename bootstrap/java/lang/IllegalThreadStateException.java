package java.lang;

// Thrown when a thread operation is attempted from a state that doesn't allow it — for us,
// calling `start()` on a thread that was already started. (In the real JDK this extends
// IllegalArgumentException; flattened to RuntimeException here, which is enough for it to
// be caught by RuntimeException/Exception/Throwable.)
public class IllegalThreadStateException extends RuntimeException {
    public IllegalThreadStateException() {
    }
}
