package java.nio.file;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

// An `IOException` that turned up in the middle of a `for` over a `DirectoryStream`.
//
// **Why it inherits from `ConcurrentModificationException` and not from something of I/O.**
// `DirectoryStream`'s iterator implements `Iterator`, whose methods declare no `IOException`; the
// only way out is an unchecked exception. `ConcurrentModificationException` is the one that already
// means "the walk was interrupted by something outside", which is exactly the case.
//
// **The cause is narrowed to `IOException`**, and the constructor requires it: wrapping a
// `RuntimeException` in this would say nothing, because that one propagates on its own already.
//
// This note used to say KajiJDK never raises it because there is no working `DirectoryStream`;
// there is one now (`KajiDirectoryStream`), so a failure while walking arrives here.
public final class DirectoryIteratorException extends ConcurrentModificationException {

    private static final long serialVersionUID = -6012699886086212874L;

    // The cause, kept apart from the one `Throwable` already carries. It is redundant in the JDK
    // --there `getCause()` is `super.getCause()` with a cast-- but this VM has a bug with
    // `invokespecial` on a method the named superclass *inherits* rather than declares
    // (`getCause()` is declared in `Throwable`, not in `ConcurrentModificationException`): it blows
    // up with `getfield: bad FieldRef`. A field of its own gives the same result without depending
    // on it.
    private final IOException ioCause;

    /**
     * @param cause the `IOException` that cut the walk
     * @throws NullPointerException if `cause` is `null`
     */
    public DirectoryIteratorException(IOException cause) {
        super(cause);
        if (cause == null) {
            throw new NullPointerException();
        }
        this.ioCause = cause;
    }

    /**
     * The cause, already at the narrow type.
     *
     * <p>Returning `IOException` and not `Throwable` is the point of the class: whoever catches it
     * wants to rethrow the original I/O one without casting.
     */
    public IOException getCause() {
        return this.ioCause;
    }
}
