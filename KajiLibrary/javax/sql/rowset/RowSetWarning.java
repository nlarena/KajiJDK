package javax.sql.rowset;

import java.sql.SQLException;

/**
 * A warning of a {@code RowSet}: something worth knowing that did not stop things going on.
 *
 * <h2>Why it is an exception nobody throws</h2>
 *
 * <p>Because it inherits from {@link SQLException} to reuse its shape —message, SQL state, error
 * code— but it is <strong>returned</strong>, not thrown. If it were thrown, the operation would be
 * cut short, and the definition of a warning is precisely that it did not cut it short.
 *
 * <h2>The chain</h2>
 *
 * <p>An operation can generate several warnings and they are chained with {@link #setNextWarning}.
 * Whoever queries gets the first and walks with {@link #getNextWarning} until it returns {@code
 * null}.
 *
 * <p>It is the same shape {@code SQLWarning} uses, and it exists separately because a {@code
 * RowSet} warning can come from the disconnected layer —a row that could not be synchronized— and
 * not from the database.
 *
 * @since 1.5
 */
public class RowSetWarning extends SQLException {

    private static final long serialVersionUID = 6242892457637296120L;

    private RowSetWarning rwarning;

    /**
     * With a message.
     *
     * @param reason the message
     */
    public RowSetWarning(String reason) {
        super(reason);
    }

    /** Without detail. */
    public RowSetWarning() {
        super();
    }

    /**
     * With message and SQL state.
     *
     * @param reason the message
     * @param SQLState the SQL state
     */
    public RowSetWarning(String reason, String SQLState) {
        super(reason, SQLState);
    }

    /**
     * With message, SQL state and error code.
     *
     * @param reason the message
     * @param SQLState the SQL state
     * @param vendorCode the vendor's code
     */
    public RowSetWarning(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    /**
     * The next warning of the chain.
     *
     * @return the next one, or {@code null} if this is the last
     */
    public RowSetWarning getNextWarning() {
        return rwarning;
    }

    /**
     * Chains another warning after this one.
     *
     * @param warning the next warning
     */
    public void setNextWarning(RowSetWarning warning) {
        rwarning = warning;
    }
}
