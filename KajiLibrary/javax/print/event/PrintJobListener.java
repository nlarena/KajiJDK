package javax.print.event;

/**
 * KajiLibrary's javax.print.event.PrintJobListener -- escucha los cambios de estado de un trabajo.
 *
 * <p>Los seis metodos corresponden a las seis constantes de {@link PrintJobEvent}; ver ahi el orden en
 * que pueden llegar, y sobre todo por que hay que implementar
 * {@link #printJobNoMoreEvents}.
 *
 * <p>Si solo interesan algunos, {@link PrintJobAdapter} los trae todos vacios.
 */
public interface PrintJobListener {

    /** El servicio ya leyo el documento entero. */
    void printDataTransferCompleted(PrintJobEvent pje);

    /** Termino bien. */
    void printJobCompleted(PrintJobEvent pje);

    /** Fallo. */
    void printJobFailed(PrintJobEvent pje);

    /** Lo cancelaron. */
    void printJobCanceled(PrintJobEvent pje);

    /** No se informa mas. Ver {@link PrintJobEvent#NO_MORE_EVENTS}. */
    void printJobNoMoreEvents(PrintJobEvent pje);

    /** Papel trabado, sin tinta, algo asi. */
    void printJobRequiresAttention(PrintJobEvent pje);
}
