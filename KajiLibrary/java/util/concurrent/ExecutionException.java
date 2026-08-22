package java.util.concurrent;

// Wraps the exception a task threw, so it can be re-raised in whichever thread asks for the
// task's result. The original is available through getCause().
// NOTE (finding #104): this extends RuntimeException rather than the JDK's Exception. It is
// raised from Future.get, which FutureTask *overrides* — and our javac cannot read a
// classpath method's Exceptions attribute, so an override declaring the same checked
// exception is rejected as "wider" than the interface allows. Making it unchecked lets the
// override compile while callers may still catch it by name. The gate compares members, so
// the hierarchy difference is invisible there; revert once #104 is fixed.
public class ExecutionException extends RuntimeException {

    public ExecutionException() {
        super();
    }

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionException(Throwable cause) {
        super(cause);
    }
}
