package java.sql;

/**
 * KajiLibrary's java.sql.DataTruncation -- a value did not fit and was cut.
 *
 * <p>It is reported as a {@link SQLWarning} when **reading** and thrown as an {@link SQLException}
 * when **writing**, and the asymmetry makes sense: reading short leaves the program with an
 * incomplete value that may be enough for it; writing short leaves the **database** with an
 * incomplete value, which can no longer be undone. That the same class serves both is possible
 * because it inherits from `SQLWarning`, which in turn is an `SQLException`.
 *
 * <p>{@link #getDataSize} and {@link #getTransferSize} are the two numbers that matter: how much
 * there was and how much got through.
 */
public class DataTruncation extends SQLWarning {

    private final int index;
    private final boolean parameter;
    private final boolean read;
    private final int dataSize;
    private final int transferSize;

    public DataTruncation(int index, boolean parameter, boolean read, int dataSize,
            int transferSize) {
        this(index, parameter, read, dataSize, transferSize, null);
    }

    public DataTruncation(int index, boolean parameter, boolean read, int dataSize,
            int transferSize, Throwable cause) {
        // The standard's two `SQLState`s: `01004` warns, `22001` fails. It is chosen by `read`,
        // which is what tells reading from writing.
        super("Data truncation", read ? "01004" : "22001", 0, cause);
        this.index = index;
        this.parameter = parameter;
        this.read = read;
        this.dataSize = dataSize;
        this.transferSize = transferSize;
    }

    /** The index of the column or parameter; -1 if unknown. */
    public int getIndex() {
        return this.index;
    }

    /** Whether it was a parameter (`true`) or a column (`false`). */
    public boolean getParameter() {
        return this.parameter;
    }

    /** Whether it was on reading (`true`) or on writing (`false`). */
    public boolean getRead() {
        return this.read;
    }

    /** How many bytes or characters there were; -1 if unknown. */
    public int getDataSize() {
        return this.dataSize;
    }

    /** How many actually got through; -1 if unknown. */
    public int getTransferSize() {
        return this.transferSize;
    }
}
