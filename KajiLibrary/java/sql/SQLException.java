package java.sql;

/**
 * KajiLibrary's java.sql.SQLException -- what fails when talking to a database.
 *
 * <p>It carries **three** pieces of data and not one, and that is its whole shape: the message, a
 * five-character `SQLState` --a standardized code, the same for every database-- and an error code
 * **of the vendor's own**. The three exist because the standard is not enough: the `SQLState` says
 * "constraint violation" and the vendor code says *which*.
 *
 * <p>And they are **chainable** among themselves through {@link #setNextException}: a single
 * operation can fail for several reasons at once --a batch of inserts-- and squashing them into one
 * would lose all but the first. The chain is different from `getCause`'s, which says "why this
 * happened"; this one says "and this other thing happened too".
 */
public class SQLException extends Exception implements Iterable<Throwable> {

    private final String sqlState;
    private final int vendorCode;
    private volatile SQLException next;

    public SQLException() {
        this(null, null, 0, null);
    }

    public SQLException(String reason) {
        this(reason, null, 0, null);
    }

    public SQLException(String reason, String sqlState) {
        this(reason, sqlState, 0, null);
    }

    public SQLException(String reason, String sqlState, int vendorCode) {
        this(reason, sqlState, vendorCode, null);
    }

    public SQLException(Throwable cause) {
        this(null, null, 0, cause);
    }

    public SQLException(String reason, Throwable cause) {
        this(reason, null, 0, cause);
    }

    public SQLException(String reason, String sqlState, Throwable cause) {
        this(reason, sqlState, 0, cause);
    }

    public SQLException(String reason, String sqlState, int vendorCode, Throwable cause) {
        super(reason, cause);
        this.sqlState = sqlState;
        this.vendorCode = vendorCode;
    }

    /** The five-character standard code, or `null` if the vendor did not give one. */
    public String getSQLState() {
        return this.sqlState;
    }

    /** The **vendor's** error code; zero if it gave none. */
    public int getErrorCode() {
        return this.vendorCode;
    }

    /** The next exception in the chain, or `null`. */
    public SQLException getNextException() {
        return this.next;
    }

    /**
     * Adds `ex` **at the end** of the chain.
     *
     * <p>At the end and not at the start: the chain's order is the order in which the failures
     * happened, and putting them the other way round would make the first error reported the last
     * one to happen.
     */
    public void setNextException(SQLException ex) {
        SQLException current = this;
        synchronized (this) {
            while (current.next != null) {
                current = current.next;
            }
            current.next = ex;
        }
    }

    /**
     * Walks this exception, its causes, and the ones that follow it in the chain.
     *
     * <p>It walks **both** dimensions --the `next` chain and the `getCause` chain-- because both
     * carry different information and whoever diagnoses wants to see everything.
     */
    public java.util.Iterator<Throwable> iterator() {
        java.util.ArrayList<Throwable> all = new java.util.ArrayList<Throwable>();
        SQLException e = this;
        while (e != null) {
            all.add(e);
            Throwable cause = e.getCause();
            while (cause != null) {
                all.add(cause);
                cause = cause.getCause();
            }
            e = e.next;
        }
        return all.iterator();
    }
}
