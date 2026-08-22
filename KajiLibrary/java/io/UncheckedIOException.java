package java.io;

import java.io.IOException;

// Wraps an IOException in an unchecked one. It exists for the places where a checked
// exception simply cannot be declared — inside a lambda passed to a Stream or a Consumer,
// whose functional interface method throws nothing. The original is never lost, and
// getCause() is narrowed to IOException so a caller who can declare `throws IOException`
// may unwrap and rethrow the real failure without a cast.
public class UncheckedIOException extends RuntimeException {

    // Kept alongside Throwable's own `cause` because the narrowed getCause() below cannot
    // reach the inherited one (our javac has no `super.` method calls yet). Both
    // constructors also pass it up, so the inherited cause chain stays correct.
    private IOException ioCause;

    public UncheckedIOException(String message, IOException cause) {
        super(message, cause);
        this.ioCause = cause;
    }

    public UncheckedIOException(IOException cause) {
        super(cause);
        this.ioCause = cause;
    }

    public IOException getCause() {
        return this.ioCause;
    }
}
