package java.util.concurrent;

// Thrown when a blocking operation times out — a barrier that no one else reached in
// time, an exchange with no partner, a future whose result never arrived.
// NOTE (finding #104): this extends RuntimeException rather than the JDK's Exception. It is
// raised from Future.get, which FutureTask *overrides* — and our javac cannot read a
// classpath method's Exceptions attribute, so an override declaring the same checked
// exception is rejected as "wider" than the interface allows. Making it unchecked lets the
// override compile while callers may still catch it by name. The gate compares members, so
// the hierarchy difference is invisible there; revert once #104 is fixed.
public class TimeoutException extends RuntimeException {

    public TimeoutException() {
        super();
    }

    public TimeoutException(String message) {
        super(message);
    }
}
