package java.lang;

// Thrown when a thread operation is asked for in a state that doesn't allow it — the classic
// case being `start()` on a thread that was already started (a Thread is single-use: once it
// has run, it can't be restarted). Unchecked, because it signals a programming mistake rather
// than a condition the caller could reasonably recover from.
// The JDK puts it under IllegalArgumentException and not under RuntimeException: a wrong thread
// state IS an invalid argument, and `catch (IllegalArgumentException)` has to catch it.
public class IllegalThreadStateException extends IllegalArgumentException {

    public IllegalThreadStateException() {
    }

    public IllegalThreadStateException(String s) {
        super(s);
    }
}
