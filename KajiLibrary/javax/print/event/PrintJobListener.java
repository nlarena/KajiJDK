package javax.print.event;

/**
 * KajiLibrary's javax.print.event.PrintJobListener -- listens to a job's changes of state.
 *
 * <p>The six methods correspond to the six constants of {@link PrintJobEvent}; see there the order
 * in which they may arrive, and above all why {@link #printJobNoMoreEvents} has to be implemented.
 *
 * <p>If only some are of interest, {@link PrintJobAdapter} brings them all empty.
 */
public interface PrintJobListener {

    /** The service already read the whole document. */
    void printDataTransferCompleted(PrintJobEvent pje);

    /** It finished well. */
    void printJobCompleted(PrintJobEvent pje);

    /** It failed. */
    void printJobFailed(PrintJobEvent pje);

    /** It was cancelled. */
    void printJobCanceled(PrintJobEvent pje);

    /** Nothing more is reported. See {@link PrintJobEvent#NO_MORE_EVENTS}. */
    void printJobNoMoreEvents(PrintJobEvent pje);

    /** A paper jam, no ink, something like that. */
    void printJobRequiresAttention(PrintJobEvent pje);
}
