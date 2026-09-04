package javax.print.event;

/**
 * KajiLibrary's javax.print.event.PrintJobAdapter -- {@link PrintJobListener} con los seis metodos
 * vacios.
 *
 * <p>Para redefinir solo los que interesen. Es abstracta aunque no tenga metodos abstractos: lo es a
 * proposito, porque una instancia que no redefina nada no hace nada y casi seguro es un error.
 */
public abstract class PrintJobAdapter implements PrintJobListener {

    /** Para las subclases. */
    protected PrintJobAdapter() {
    }

    /** No hace nada. */
    @Override
    public void printDataTransferCompleted(PrintJobEvent pje) {
    }

    /** No hace nada. */
    @Override
    public void printJobCompleted(PrintJobEvent pje) {
    }

    /** No hace nada. */
    @Override
    public void printJobFailed(PrintJobEvent pje) {
    }

    /** No hace nada. */
    @Override
    public void printJobCanceled(PrintJobEvent pje) {
    }

    /** No hace nada. */
    @Override
    public void printJobNoMoreEvents(PrintJobEvent pje) {
    }

    /** No hace nada. */
    @Override
    public void printJobRequiresAttention(PrintJobEvent pje) {
    }
}
