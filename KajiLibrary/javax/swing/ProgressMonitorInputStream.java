package javax.swing;

import java.awt.Component;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * An input stream that shows the reading's progress.
 *
 * <h2>It is wrapped, not configured</h2>
 *
 * <p>It is passed the real stream and this one is read from. Each reading advances the
 * {@link ProgressMonitor}, and the monitor decides on its own whether it is worth showing a
 * notice -- see its note, which explains the two delays.
 *
 * <p>The maximum comes from {@code available()}, which for a file is its size. For something
 * that does not know it -- a network connection -- it gives zero and the bar stays still: there
 * is nothing to work out how much is left from, and this class does not invent it.
 *
 * <h2>Cancelling cuts the reading off</h2>
 *
 * <p>And it does it properly: by throwing {@link InterruptedIOException}, which is an
 * {@link IOException} and therefore is caught by anybody who was already handling reading
 * errors. It is the only way for a cancellation not to be lost in a {@code catch} that only
 * looks at disk problems.
 */
public class ProgressMonitorInputStream extends FilterInputStream {

    private final ProgressMonitor monitor;
    private int nread = 0;
    private int size = 0;

    Component parentComponent;
    Object message;

    /**
     * It wraps that stream.
     *
     * <p>It reads {@code available()} in order to know the total; if the stream does not know it,
     * it is left at zero. See the class note.
     */
    public ProgressMonitorInputStream(Component parentComponent, Object message,
            InputStream in) {
        super(in);
        this.parentComponent = parentComponent;
        this.message = message;
        try {
            size = in.available();
        } catch (IOException ioe) {
            // A stream that does not know how much it has is not an error: the bar stays still.
            size = 0;
        }
        monitor = new ProgressMonitor(parentComponent, message, null, 0, size);
    }

    /**
     * The monitor, in case a delay has to be changed or whether it was cancelled has to be read.
     */
    public ProgressMonitor getProgressMonitor() {
        return monitor;
    }

    /**
     * @throws InterruptedIOException if the user cancelled
     * @throws IOException if the reading fails
     */
    public int read() throws IOException {
        int c = in.read();
        if (c >= 0) {
            nread = nread + 1;
            monitor.setProgress(nread);
        }
        checkCancel();
        return c;
    }

    /**
     * @throws InterruptedIOException if the user cancelled
     * @throws IOException if the reading fails
     */
    public int read(byte[] b) throws IOException {
        int nr = in.read(b);
        if (nr > 0) {
            nread = nread + nr;
            monitor.setProgress(nread);
        }
        checkCancel();
        return nr;
    }

    /**
     * @throws InterruptedIOException if the user cancelled
     * @throws IOException if the reading fails
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int nr = in.read(b, off, len);
        if (nr > 0) {
            nread = nread + nr;
            monitor.setProgress(nread);
        }
        checkCancel();
        return nr;
    }

    /**
     * It skips bytes; they count as progress too.
     *
     * @throws IOException if it fails
     */
    public long skip(long n) throws IOException {
        long nr = in.skip(n);
        if (nr > 0) {
            nread = nread + (int) nr;
            monitor.setProgress(nread);
        }
        return nr;
    }

    /**
     * It closes the stream and the notice.
     *
     * @throws IOException if the closing fails
     */
    public void close() throws IOException {
        in.close();
        monitor.close();
    }

    /**
     * It goes back to the beginning; the progress goes back with it.
     *
     * @throws IOException if the stream does not support going back
     */
    public synchronized void reset() throws IOException {
        in.reset();
        nread = size - in.available();
        monitor.setProgress(nread);
    }

    /**
     * @throws InterruptedIOException if the user cancelled
     */
    private void checkCancel() throws InterruptedIOException {
        if (monitor.isCanceled()) {
            InterruptedIOException exc = new InterruptedIOException("progress");
            exc.bytesTransferred = nread;
            monitor.close();
            throw exc;
        }
    }
}
