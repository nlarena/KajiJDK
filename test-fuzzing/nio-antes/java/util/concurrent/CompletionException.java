package java.util.concurrent;

// The unchecked twin of {@link ExecutionException}: it wraps the failure of a task whose
// result is read through an API that cannot declare a checked exception — a lambda in a
// completion pipeline, say. Same cause-carrying job, no `throws` clause imposed on callers.
public class CompletionException extends RuntimeException {

    protected CompletionException() {
        super();
    }

    protected CompletionException(String message) {
        super(message);
    }

    public CompletionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompletionException(Throwable cause) {
        super(cause);
    }
}
