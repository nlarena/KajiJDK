package java.util.concurrent;

// Thrown when a task cannot be accepted for execution — typically because the executor has
// been shut down, or its queue is saturated.
public class RejectedExecutionException extends RuntimeException {

    public RejectedExecutionException() {
        super();
    }

    public RejectedExecutionException(String message) {
        super(message);
    }

    public RejectedExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RejectedExecutionException(Throwable cause) {
        super(cause);
    }
}
