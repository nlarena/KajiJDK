package javax.imageio;

import java.io.IOException;

/**
 * KajiLibrary's javax.imageio.IIOException -- an image read or write operation failed.
 *
 * <p>It is an {@link IOException} and not a separate exception, and that decision explains how the
 * API is used: a method that reads an image already declares {@code IOException}, so adding this
 * forces no signature to change.
 *
 * <p>Readers and writers use it to tell "the file is broken or the format is not understood" from
 * "the disk failed". Both come out as {@code IOException}; only the first is an
 * {@code IIOException}.
 *
 * <p>The constructor with a cause is the important one: a decoder that fails inside almost always
 * has something more concrete to say, and wrapping it keeps that trace.
 */
public class IIOException extends IOException {

    private static final long serialVersionUID = -3216210718638985251L;

    /** @param message what happened */
    public IIOException(String message) {
        super(message);
    }

    /**
     * @param message what happened
     * @param cause the original
     */
    public IIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
