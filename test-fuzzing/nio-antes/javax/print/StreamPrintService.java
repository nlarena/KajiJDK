package javax.print;

import java.io.OutputStream;

/**
 * KajiLibrary's javax.print.StreamPrintService -- a "printer" that writes to a stream.
 *
 * <p>It converts instead of printing: it is given a document in one format and writes another to
 * the stream. That is how a PDF or a PostScript is generated from the same code that would print on
 * paper.
 *
 * <p>It is a complete {@link PrintService} --it accepts jobs, has attributes, can be asked what it
 * supports-- with two differences:
 *
 * <ul>
 *   <li>it has {@link #getOutputFormat}, which says what it writes;
 *   <li>it serves <b>only once</b>. {@link #dispose} closes the job; after that nothing more can be
 *       sent to it, even if the stream is still open.
 * </ul>
 *
 * <p>{@link #dispose} <b>does not close the stream</b>: whoever passed it opened it, and closing it
 * is their business. That is what allows writing several documents one after the other into the
 * same file, with a new service for each one.
 *
 * <p>For the same reason, {@code PrintServiceLookup.registerService} rejects these: they are not
 * printers somebody should find by chance.
 */
public abstract class StreamPrintService implements PrintService {

    /** Where it writes. */
    private final OutputStream outStream;

    /** Whether the job was already closed. */
    private boolean disposed = false;

    /** It is not built without a stream; the no-argument constructor does not exist on purpose. */
    protected StreamPrintService(OutputStream out) {
        this.outStream = out;
    }

    /** Where it writes. */
    public OutputStream getOutputStream() {
        return this.outStream;
    }

    /** Which format it writes, as a MIME type. */
    public abstract String getOutputFormat();

    /** Closes the job. It does not close the stream; see the class note. */
    public void dispose() {
        this.disposed = true;
    }

    /** Whether it was already closed. */
    public boolean isDisposed() {
        return this.disposed;
    }
}
