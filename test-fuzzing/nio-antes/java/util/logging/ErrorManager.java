package java.util.logging;

/**
 * KajiLibrary's java.util.logging.ErrorManager -- what to do when **the logging itself** fails.
 *
 * <p>It exists because of a circular problem: if writing a message fails, the failure cannot be
 * reported by writing a message. And throwing is no use either -- an application should not crash
 * because the log's disk filled up. The JDK's way out is to report **once only** to standard error
 * and then keep quiet, which is what this implementation does.
 */
public class ErrorManager {

    /** An unclassified failure. */
    public static final int GENERIC_FAILURE = 0;

    /** A failure while writing. */
    public static final int WRITE_FAILURE = 1;

    /** A failure while flushing the buffer. */
    public static final int FLUSH_FAILURE = 2;

    /** A failure while closing. */
    public static final int CLOSE_FAILURE = 3;

    /** A failure while opening. */
    public static final int OPEN_FAILURE = 4;

    /** A failure while applying the configuration. */
    public static final int FORMAT_FAILURE = 5;

    private boolean alreadyReported = false;

    /** It reports the failure; only the first one gets out. */
    public synchronized void error(String msg, Exception ex, int code) {
        if (this.alreadyReported) {
            return;
        }
        this.alreadyReported = true;
        System.err.println("java.util.logging.ErrorManager: " + code
                + (msg != null ? ": " + msg : ""));
        if (ex != null) {
            ex.printStackTrace();
        }
    }
}
