package javax.print;

/**
 * KajiLibrary's javax.print.CancelablePrintJob -- a job that can be cancelled.
 *
 * <p>It is a separate interface and not a method of {@link DocPrintJob} because not every job can
 * be cancelled: some services hand over to a system queue and then lose control. A program asks
 * with {@code instanceof} before offering a cancel button.
 *
 * <p>Cancelling is asynchronous: {@link #cancel} returns at once and the {@code JOB_CANCELED}
 * arrives later. It may fail if the job already finished, or if it came too late -- in which case
 * what arrives is {@code JOB_COMPLETE}.
 */
public interface CancelablePrintJob extends DocPrintJob {

    /**
     * Asks to cancel it.
     *
     * @throws PrintException if it no longer can be
     */
    void cancel() throws PrintException;
}
