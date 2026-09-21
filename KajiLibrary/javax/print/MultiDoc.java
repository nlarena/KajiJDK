package javax.print;

import java.io.IOException;

/**
 * KajiLibrary's javax.print.MultiDoc -- several documents in a single job.
 *
 * <p>It is a linked list and not a collection, and that draws attention. The reason is that the
 * documents may arrive little by little: {@link #next} may <b>block</b> waiting for the next one,
 * and the count may not be known beforehand. With a {@code List} one would have to have them all
 * before starting.
 *
 * <p>{@link #next} returns null when there are no more.
 *
 * <p>Like {@link Doc}, both methods have to return always the same: walking it twice has to give
 * the same objects.
 */
public interface MultiDoc {

    /**
     * The current document.
     *
     * @throws IOException if it could not be obtained
     */
    Doc getDoc() throws IOException;

    /**
     * The rest, or null if this was the last one. It may block.
     *
     * @throws IOException if it could not be obtained
     */
    MultiDoc next() throws IOException;
}
