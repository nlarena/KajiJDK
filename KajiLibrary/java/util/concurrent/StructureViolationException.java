package java.util.concurrent;

// Thrown when a structured-concurrency construct is used out of order — closed on a thread
// other than the one that opened it, closed while nested scopes are still open, or a scoped
// value rebound across a boundary that owns it. It reports a broken *nesting invariant*,
// not a task failure, which is why it is unchecked and final: there is nothing sensible for
// a caller to recover, and no subclass could refine the diagnosis.
public final class StructureViolationException extends RuntimeException {

    public StructureViolationException() {
        super();
    }

    public StructureViolationException(String message) {
        super(message);
    }
}
