package java.sql;

/**
 * KajiLibrary's java.sql.BatchUpdateException -- a failure while running a batch.
 *
 * <p>It carries **the counts** of what did run, and that is its whole reason to be: a batch of a
 * thousand inserts that fails at number six hundred is neither a success nor a failure, and an
 * exception without that array would leave whoever catches it no way to know where it stopped. Each
 * position holds the number of rows that statement touched, or {@link Statement#EXECUTE_FAILED} if
 * it failed.
 *
 * <p>The array may be shorter than the batch --as far as the driver got-- and that is information
 * too.
 */
public class BatchUpdateException extends SQLException {

    // The `long` version is stored and the `int` one derived: the other way round, the large counts
    // would be lost.
    private final long[] largeUpdateCounts;

    public BatchUpdateException() {
        this(null, null, 0, (long[]) null, null);
    }

    public BatchUpdateException(Throwable cause) {
        this(null, null, 0, (long[]) null, cause);
    }

    public BatchUpdateException(int[] updateCounts) {
        this(null, null, 0, toLongArray(updateCounts), null);
    }

    public BatchUpdateException(int[] updateCounts, Throwable cause) {
        this(null, null, 0, toLongArray(updateCounts), cause);
    }

    public BatchUpdateException(String reason, int[] updateCounts) {
        this(reason, null, 0, toLongArray(updateCounts), null);
    }

    public BatchUpdateException(String reason, int[] updateCounts, Throwable cause) {
        this(reason, null, 0, toLongArray(updateCounts), cause);
    }

    public BatchUpdateException(String reason, String SQLState, int[] updateCounts) {
        this(reason, SQLState, 0, toLongArray(updateCounts), null);
    }

    public BatchUpdateException(String reason, String SQLState, int[] updateCounts,
            Throwable cause) {
        this(reason, SQLState, 0, toLongArray(updateCounts), cause);
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode,
            int[] updateCounts) {
        this(reason, SQLState, vendorCode, toLongArray(updateCounts), null);
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode, int[] updateCounts,
            Throwable cause) {
        this(reason, SQLState, vendorCode, toLongArray(updateCounts), cause);
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode,
            long[] updateCounts, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
        this.largeUpdateCounts = updateCounts == null ? null : updateCounts.clone();
    }

    /**
     * The counts, truncated to `int`.
     *
     * <p>Truly truncated: a count that does not fit in an `int` comes out wrong, and that is why
     * {@link #getLargeUpdateCounts} exists.
     */
    public int[] getUpdateCounts() {
        if (this.largeUpdateCounts == null) {
            return null;
        }
        int[] out = new int[this.largeUpdateCounts.length];
        int i = 0;
        while (i < out.length) {
            out[i] = (int) this.largeUpdateCounts[i];
            i = i + 1;
        }
        return out;
    }

    /** The counts, untruncated. */
    public long[] getLargeUpdateCounts() {
        return this.largeUpdateCounts == null ? null : this.largeUpdateCounts.clone();
    }

    private static long[] toLongArray(int[] counts) {
        if (counts == null) {
            return null;
        }
        long[] out = new long[counts.length];
        int i = 0;
        while (i < counts.length) {
            out[i] = counts[i];
            i = i + 1;
        }
        return out;
    }
}
