package java.awt.print;

import java.io.IOException;

/**
 * KajiLibrary's java.awt.print.PrinterIOException -- the job failed because of an input/output
 * problem.
 *
 * <p>It wraps the original {@link IOException} so it can go out through a signature that only
 * declares {@link PrinterException}.
 *
 * <h2>Two ways to get the same exception</h2>
 *
 * <p>{@link #getIOException} and {@link #getCause} return <b>the same thing</b>. The first is from
 * 1998; the second appeared in 1.4 with the general chained-cause mechanism, and was overridden so
 * that tools that print stack traces would find the cause the standard way.
 *
 * <p>{@code getMessage()} returns null: the constructor takes no message, and it does not inherit
 * the cause's.
 */
public class PrinterIOException extends PrinterException {

    private static final long serialVersionUID = 5850870712125932846L;

    /** The original. */
    private final IOException mException;

    /** @param exception the one to wrap */
    public PrinterIOException(IOException exception) {
        initCause(null);
        this.mException = exception;
    }

    /** The original. The same as {@link #getCause}. */
    public IOException getIOException() {
        return this.mException;
    }

    /** The original. See the class note. */
    @Override
    public Throwable getCause() {
        return this.mException;
    }
}
