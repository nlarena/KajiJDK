package java.util.concurrent;

// Thrown when the result of a task is retrieved but the task was cancelled.
public class CancellationException extends IllegalStateException {

    public CancellationException() {
        super();
    }

    public CancellationException(String message) {
        super(message);
    }
}
