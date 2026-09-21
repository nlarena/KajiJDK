package javax.print.event;

import javax.print.DocPrintJob;

/**
 * KajiLibrary's javax.print.event.PrintJobEvent -- a job's change of state.
 *
 * <p>The type is read with {@link #getPrintEventType} and is one of the six constants. It is worth
 * understanding in which order they may arrive, because it is not obvious:
 *
 * <ul>
 *   <li>{@link #DATA_TRANSFER_COMPLETE} says the service finished reading the document. It does not
 *       say it was printed: it says the {@code Doc} can now be closed or reused;
 *   <li>{@link #JOB_COMPLETE}, {@link #JOB_CANCELED} and {@link #JOB_FAILED} are the three possible
 *       endings, and they are mutually exclusive;
 *   <li>{@link #REQUIRES_ATTENTION} is a paper jam, no ink, an empty tray. It is not final: the job
 *       may go on when somebody solves it;
 *   <li>{@link #NO_MORE_EVENTS} is the important one and the one usually ignored. It means the
 *       service <b>stops reporting</b> on this job. It may arrive without any ending having
 *       arrived, because some queues lose sight of the job once handed over. A program that waits
 *       for {@code JOB_COMPLETE} without catching this hangs forever.
 * </ul>
 */
public class PrintJobEvent extends PrintEvent {

    private static final long serialVersionUID = -1711656903622072997L;

    /** Cancelled. */
    public static final int JOB_CANCELED = 101;

    /** Finished well. */
    public static final int JOB_COMPLETE = 102;

    /** Failed. */
    public static final int JOB_FAILED = 103;

    /** It needs intervention; it is not final. See the class note. */
    public static final int REQUIRES_ATTENTION = 104;

    /** Nothing more is reported on this job. See the class note. */
    public static final int NO_MORE_EVENTS = 105;

    /** The document was already read whole. See the class note. */
    public static final int DATA_TRANSFER_COMPLETE = 106;

    /** Which of the six. */
    private final int reason;

    /**
     * @param source the job
     * @param reason one of the six constants
     * @throws IllegalArgumentException if the job is null
     */
    public PrintJobEvent(DocPrintJob source, int reason) {
        super(source);
        this.reason = reason;
    }

    /** Which of the six constants. */
    public int getPrintEventType() {
        return this.reason;
    }

    /** The job. */
    public DocPrintJob getPrintJob() {
        return (DocPrintJob) getSource();
    }
}
