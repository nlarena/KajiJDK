package java.lang;

// Thrown when a thread operation is asked for in a state that doesn't allow it — the classic
// case being `start()` on a thread that was already started (a Thread is single-use: once it
// has run, it can't be restarted). Unchecked, because it signals a programming mistake rather
// than a condition the caller could reasonably recover from.
// El JDK la pone bajo IllegalArgumentException, no bajo RuntimeException: un estado de hilo
// equivocado ES un argumento invalido, y `catch (IllegalArgumentException)` tiene que atraparla.
public class IllegalThreadStateException extends IllegalArgumentException {

    public IllegalThreadStateException() {
    }
}
