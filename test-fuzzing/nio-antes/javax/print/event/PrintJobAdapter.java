package javax.print.event;

/**
 * KajiLibrary's javax.print.event.PrintJobAdapter -- {@link PrintJobListener} with the six methods
 * empty.
 *
 * <p>To override only the ones of interest. It is abstract although it has no abstract methods: it
 * is so on purpose, because an instance that overrides nothing does nothing and is almost surely a
 * mistake.
 */
public abstract class PrintJobAdapter implements PrintJobListener {

    /** For subclasses. */
    protected PrintJobAdapter() {
    }

    /** Does nothing. */
    @Override
    public void printDataTransferCompleted(PrintJobEvent pje) {
    }

    /** Does nothing. */
    @Override
    public void printJobCompleted(PrintJobEvent pje) {
    }

    /** Does nothing. */
    @Override
    public void printJobFailed(PrintJobEvent pje) {
    }

    /** Does nothing. */
    @Override
    public void printJobCanceled(PrintJobEvent pje) {
    }

    /** Does nothing. */
    @Override
    public void printJobNoMoreEvents(PrintJobEvent pje) {
    }

    /** Does nothing. */
    @Override
    public void printJobRequiresAttention(PrintJobEvent pje) {
    }
}
