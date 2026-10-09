package java.sql;

/**
 * KajiLibrary's java.sql.Statement -- an SQL statement sent as text.
 *
 * <p><strong>The interface is complete.</strong> This note used to say the variants that return
 * generated keys and the `getMoreResults` with flags were left out; `getMoreResults(int)` and
 * `getGeneratedKeys` are declared below, and the section further down already describes them.
 *
 * <p>That it takes the statement as text is what makes it dangerous: concatenating a user's value
 * inside that text is exactly SQL injection. For values there is {@link PreparedStatement}, which
 * sends them **apart** from the statement and therefore cannot confuse them with code.
 */
public interface Statement extends Wrapper, AutoCloseable {

    /** Do not return the keys the database generated. */
    int NO_GENERATED_KEYS = 2;

    /** Devolverlas. */
    int RETURN_GENERATED_KEYS = 1;

    /** Close the open results before fetching the next. */
    int CLOSE_CURRENT_RESULT = 1;

    /** To leave them open. */
    int KEEP_CURRENT_RESULT = 2;

    /** Close every result of this statement. */
    int CLOSE_ALL_RESULTS = 3;

    /** The statement ran fine and returned no rows. */
    int SUCCESS_NO_INFO = -2;

    /** That statement of the batch failed. */
    int EXECUTE_FAILED = -3;

    /** It runs a query and returns its rows. */
    ResultSet executeQuery(String sql) throws SQLException;

    /** It runs a modification and returns how many rows it touched. */
    int executeUpdate(String sql) throws SQLException;

    /** The same, for counts that do not fit in an `int`. */
    long executeLargeUpdate(String sql) throws SQLException;

    /**
     * It runs any statement.
     *
     * @return `true` if the first thing it returned is rows; they are then asked for with
     *         {@link #getResultSet}
     */
    boolean execute(String sql) throws SQLException;

    /** The current result's rows, or `null` if the current one is a count. */
    ResultSet getResultSet() throws SQLException;

    /** The current result's count, or -1 if the current one is rows. */
    int getUpdateCount() throws SQLException;

    /** It moves on to the next result. */
    boolean getMoreResults() throws SQLException;

    // ---- batches --------------------------------------------------------------------------------
    //
    // They exist because of latency: sending a thousand `insert`s one at a time is a thousand round
    // trips. The batch sends them together, and that is why it returns **an array** of counts and
    // not a single one.

    void addBatch(String sql) throws SQLException;

    void clearBatch() throws SQLException;

    int[] executeBatch() throws SQLException;

    long[] executeLargeBatch() throws SQLException;

    // ---- limits ---------------------------------------------------------------------------------

    /** The maximum bytes a large column returns; zero for no limit. */
    void setMaxFieldSize(int max) throws SQLException;

    int getMaxFieldSize() throws SQLException;

    /** The maximum rows; zero for no limit. */
    void setMaxRows(int max) throws SQLException;

    int getMaxRows() throws SQLException;

    /** Seconds before cancelling; zero for no limit. */
    void setQueryTimeout(int seconds) throws SQLException;

    int getQueryTimeout() throws SQLException;

    void setFetchSize(int rows) throws SQLException;

    int getFetchSize() throws SQLException;

    void setFetchDirection(int direction) throws SQLException;

    int getFetchDirection() throws SQLException;

    /** Whether the database should interpret the `{fn ...}` escape sequences. */
    void setEscapeProcessing(boolean enable) throws SQLException;

    /** The cursor name for the results it produces. */
    void setCursorName(String name) throws SQLException;

    // ---- life cycle -----------------------------------------------------------------------------

    /** It cancels the execution in progress, **from another thread**. */
    void cancel() throws SQLException;

    void close() throws SQLException;

    boolean isClosed() throws SQLException;

    /** That it close itself when its last result closes. */
    void closeOnCompletion() throws SQLException;

    boolean isCloseOnCompletion() throws SQLException;

    /** A hint that it will not be reused, so the pool discards it. */
    default void setPoolable(boolean poolable) throws SQLException {
        throw new UnsupportedOperationException("setPoolable not implemented");
    }

    default boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException("isPoolable not implemented");
    }

    int getResultSetType() throws SQLException;

    int getResultSetConcurrency() throws SQLException;

    int getResultSetHoldability() throws SQLException;

    /** The connection that created it. */
    Connection getConnection() throws SQLException;

    SQLWarning getWarnings() throws SQLException;

    void clearWarnings() throws SQLException;

    // ---- generated keys and quoting -------------------------------------------------------------
    //
    // The variants of `execute`/`executeUpdate` with a second argument are all the same question:
    // what to do with the keys the database generated by itself. All of them can be asked for
    // ({@link #RETURN_GENERATED_KEYS}), or the ones of interest named by index or by column name;
    // they are then read with {@link #getGeneratedKeys}. Without this one would have to do an extra
    // `select` and guess which row is the one just inserted.
    //
    // The `enquote*` are the late answer --Java 9-- to the only way of putting an identifier into a
    // statement being to concatenate it. They are still not as safe as a parameter: an identifier
    // **cannot** be a parameter, so quoting well is the best that can be done.
    //
    // The `large*` duplicate methods that returned `int` because a table can have more than two
    // billion rows, and the `int` overflowed them in silence.

    boolean execute(java.lang.String sql, int autoGeneratedKeys) throws java.sql.SQLException;

    boolean execute(java.lang.String sql, int[] columnIndexes) throws java.sql.SQLException;

    boolean execute(java.lang.String sql, java.lang.String[] columnNames) throws java.sql.SQLException;

    boolean getMoreResults(int current) throws java.sql.SQLException;

    default boolean isSimpleIdentifier(java.lang.String identifier) throws java.sql.SQLException {
        if (identifier == null || identifier.length() == 0 || identifier.length() > 128) {
            return false;
        }
        // Simple = it starts with a letter and carries on with letters, digits or underscores.
        // Nothing else is enough to go into a statement unquoted.
        char c = identifier.charAt(0);
        if (!Character.isLetter(c)) {
            return false;
        }
        int i = 1;
        while (i < identifier.length()) {
            char d = identifier.charAt(i);
            if (!Character.isLetterOrDigit(d) && d != '_') {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    int executeUpdate(java.lang.String sql, int autoGeneratedKeys) throws java.sql.SQLException;

    int executeUpdate(java.lang.String sql, int[] columnIndexes) throws java.sql.SQLException;

    int executeUpdate(java.lang.String sql, java.lang.String[] columnNames) throws java.sql.SQLException;

    default java.lang.String enquoteIdentifier(java.lang.String identifier, boolean alwaysQuote) throws java.sql.SQLException {
        if (identifier == null || identifier.length() == 0 || identifier.length() > 128) {
            throw new java.sql.SQLException("identificador invalido");
        }
        if (!alwaysQuote && this.isSimpleIdentifier(identifier)) {
            return identifier;
        }
        // An identifier goes between **double** quotes; a double quote inside is doubled. A zero
        // inside cannot be quoted in any way.
        if (identifier.indexOf('\u0000') >= 0) {
            throw new java.sql.SQLException("Invalid name");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    default java.lang.String enquoteLiteral(java.lang.String val) throws java.sql.SQLException {
        // A single quote inside is written by doubling it. It is the only rule, and it is what
        // makes the operation safe.
        return "'" + val.replace("'", "''") + "'";
    }

    default java.lang.String enquoteNCharLiteral(java.lang.String val) throws java.sql.SQLException {
        return "N" + this.enquoteLiteral(val);
    }

    java.sql.ResultSet getGeneratedKeys() throws java.sql.SQLException;

    default long executeLargeUpdate(java.lang.String sql, int autoGeneratedKeys) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("executeLargeUpdate not implemented");
    }

    default long executeLargeUpdate(java.lang.String sql, int[] columnIndexes) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("executeLargeUpdate not implemented");
    }

    default long executeLargeUpdate(java.lang.String sql, java.lang.String[] columnNames) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("executeLargeUpdate not implemented");
    }

    default long getLargeMaxRows() throws java.sql.SQLException {
        return (long) this.getMaxRows();
    }

    default long getLargeUpdateCount() throws java.sql.SQLException {
        throw new UnsupportedOperationException("getLargeUpdateCount not implemented");
    }

    default void setLargeMaxRows(long max) throws java.sql.SQLException {
        throw new UnsupportedOperationException("setLargeMaxRows not implemented");
    }
}
