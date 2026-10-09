package java.sql;

/**
 * KajiLibrary's java.sql.ResultSet -- the rows a query returned, walked one at a time.
 *
 * <p>A cursor and not a list, and that is the design decision that explains the whole interface: a
 * query's result may not fit in memory, so it is walked by asking for the next row. Hence `next()`
 * being at once "advance" and "is there more" -- two questions in one method, because separating
 * them would force fetching the row twice.
 *
 * <p><strong>The interface is complete.</strong> This note used to say the conversion families that
 * drag in SQL types of their own --`getBlob`, `getClob`, `getArray`, `getRef`, `getSQLXML`,
 * `getRowId`-- and the updating half (`updateXxx`) were left out. Each of those six is here in both
 * overloads and there are 87 `updateXxx`; the sections further down already describe them, so the
 * header was contradicting the rest of the file.
 *
 * <p>The columns are numbered **from one**.
 */
public interface ResultSet extends Wrapper, AutoCloseable {

    // ---- direction, type and concurrency --------------------------------------------------------

    /** The rows will be read forwards. */
    int FETCH_FORWARD = 1000;

    /** They will be read backwards. */
    int FETCH_REVERSE = 1001;

    /** In what order is not known. */
    int FETCH_UNKNOWN = 1002;

    /** The cursor only goes forwards. */
    int TYPE_FORWARD_ONLY = 1003;

    /** It can move in any direction; it does not see others' changes. */
    int TYPE_SCROLL_INSENSITIVE = 1004;

    /** It can move in any direction; it **does** see others' changes. */
    int TYPE_SCROLL_SENSITIVE = 1005;

    /** It cannot be updated through the cursor. */
    int CONCUR_READ_ONLY = 1007;

    /** It can be updated through the cursor. */
    int CONCUR_UPDATABLE = 1008;

    /** The cursor survives a `commit`. */
    int HOLD_CURSORS_OVER_COMMIT = 1;

    /** The cursor closes at the `commit`. */
    int CLOSE_CURSORS_AT_COMMIT = 2;

    // ---- navigation -----------------------------------------------------------------------------

    /** It advances to the next row; `false` when none are left. */
    boolean next() throws SQLException;

    /** It steps back one row. */
    boolean previous() throws SQLException;

    boolean first() throws SQLException;

    boolean last() throws SQLException;

    /** Before the first: `next()` lands on the first. */
    void beforeFirst() throws SQLException;

    /** After the last. */
    void afterLast() throws SQLException;

    /** To row `row`; a negative counts from the end. */
    boolean absolute(int row) throws SQLException;

    /** `rows` rows beyond where it is. */
    boolean relative(int rows) throws SQLException;

    /** Which row it is on, from one; zero if it is on none. */
    int getRow() throws SQLException;

    boolean isBeforeFirst() throws SQLException;

    boolean isAfterLast() throws SQLException;

    boolean isFirst() throws SQLException;

    boolean isLast() throws SQLException;

    // ---- life cycle -----------------------------------------------------------------------------

    void close() throws SQLException;

    boolean isClosed() throws SQLException;

    /**
     * Whether the last value read was null.
     *
     * <p>It is needed because the primitive accessors cannot return `null`: `getInt` of a null
     * column returns zero, which is indistinguishable from a real zero. It is asked **after**
     * reading, not before.
     */
    boolean wasNull() throws SQLException;

    // ---- accessors by index ---------------------------------------------------------------------

    String getString(int columnIndex) throws SQLException;

    boolean getBoolean(int columnIndex) throws SQLException;

    byte getByte(int columnIndex) throws SQLException;

    short getShort(int columnIndex) throws SQLException;

    int getInt(int columnIndex) throws SQLException;

    long getLong(int columnIndex) throws SQLException;

    float getFloat(int columnIndex) throws SQLException;

    double getDouble(int columnIndex) throws SQLException;

    byte[] getBytes(int columnIndex) throws SQLException;

    Object getObject(int columnIndex) throws SQLException;

    java.math.BigDecimal getBigDecimal(int columnIndex) throws SQLException;

    /** The value converted to `type`; the typed form, which saves the cast. */
    <T> T getObject(int columnIndex, Class<T> type) throws SQLException;

    // ---- accessors by name ----------------------------------------------------------------------
    //
    // The same ones by column label. They cost one lookup more than the index and in exchange do
    // not break when somebody adds a column to the query.

    String getString(String columnLabel) throws SQLException;

    boolean getBoolean(String columnLabel) throws SQLException;

    byte getByte(String columnLabel) throws SQLException;

    short getShort(String columnLabel) throws SQLException;

    int getInt(String columnLabel) throws SQLException;

    long getLong(String columnLabel) throws SQLException;

    float getFloat(String columnLabel) throws SQLException;

    double getDouble(String columnLabel) throws SQLException;

    byte[] getBytes(String columnLabel) throws SQLException;

    Object getObject(String columnLabel) throws SQLException;

    java.math.BigDecimal getBigDecimal(String columnLabel) throws SQLException;

    <T> T getObject(String columnLabel, Class<T> type) throws SQLException;

    /** That column's index, from one. */
    int findColumn(String columnLabel) throws SQLException;

    // ---- shape and state ------------------------------------------------------------------------

    /** What columns there are. */
    ResultSetMetaData getMetaData() throws SQLException;

    /** The statement that produced this result, or `null` if there was none. */
    Statement getStatement() throws SQLException;

    /** The cursor's name, for an `update ... where current of`. */
    String getCursorName() throws SQLException;

    int getType() throws SQLException;

    int getConcurrency() throws SQLException;

    int getHoldability() throws SQLException;

    void setFetchDirection(int direction) throws SQLException;

    int getFetchDirection() throws SQLException;

    /** How many rows to fetch per trip: a performance hint, not a limit. */
    void setFetchSize(int rows) throws SQLException;

    int getFetchSize() throws SQLException;

    SQLWarning getWarnings() throws SQLException;

    void clearWarnings() throws SQLException;

    // ---- the rest of the accessors, and the updating half ---------------------------------------
    //
    // The `getXxx` that were missing are the large types and the date ones. The variants with a
    // `Calendar` exist for a concrete reason: a `TIMESTAMP` column with no time zone does not
    // designate an instant until somebody chooses the zone to read it in, and without this argument
    // that choice is made by the machine running the program -- which turns a datum into something
    // that changes from server to server. The variants with a `Map` translate SQL types of their
    // own into Java classes.
    //
    // The `updateXxx` are the interface's other half, and the less used one: an updatable cursor
    // allows writing **through the row**, without writing an `update`. The values are changed,
    // `updateRow` is called, and the database translates. `insertRow` goes with `moveToInsertRow`,
    // which moves the cursor to a row that does not exist yet.
    //
    // `getUnicodeStream` has been deprecated since 1999, like its twin in `PreparedStatement`.

    boolean rowDeleted() throws java.sql.SQLException;

    boolean rowInserted() throws java.sql.SQLException;

    boolean rowUpdated() throws java.sql.SQLException;

    java.io.InputStream getAsciiStream(int columnIndex) throws java.sql.SQLException;

    java.io.InputStream getAsciiStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.io.InputStream getBinaryStream(int columnIndex) throws java.sql.SQLException;

    java.io.InputStream getBinaryStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.io.InputStream getUnicodeStream(int columnIndex) throws java.sql.SQLException;

    java.io.InputStream getUnicodeStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.io.Reader getCharacterStream(int columnIndex) throws java.sql.SQLException;

    java.io.Reader getCharacterStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.io.Reader getNCharacterStream(int columnIndex) throws java.sql.SQLException;

    java.io.Reader getNCharacterStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.lang.Object getObject(int columnIndex, java.util.Map x) throws java.sql.SQLException;

    java.lang.Object getObject(java.lang.String columnLabel, java.util.Map x) throws java.sql.SQLException;

    java.lang.String getNString(int columnIndex) throws java.sql.SQLException;

    java.lang.String getNString(java.lang.String columnLabel) throws java.sql.SQLException;

    java.math.BigDecimal getBigDecimal(int columnIndex, int x) throws java.sql.SQLException;

    java.math.BigDecimal getBigDecimal(java.lang.String columnLabel, int x) throws java.sql.SQLException;

    java.net.URL getURL(int columnIndex) throws java.sql.SQLException;

    java.net.URL getURL(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Array getArray(int columnIndex) throws java.sql.SQLException;

    java.sql.Array getArray(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Blob getBlob(int columnIndex) throws java.sql.SQLException;

    java.sql.Blob getBlob(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Clob getClob(int columnIndex) throws java.sql.SQLException;

    java.sql.Clob getClob(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Date getDate(int columnIndex) throws java.sql.SQLException;

    java.sql.Date getDate(int columnIndex, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.Date getDate(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Date getDate(java.lang.String columnLabel, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.NClob getNClob(int columnIndex) throws java.sql.SQLException;

    java.sql.NClob getNClob(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Ref getRef(int columnIndex) throws java.sql.SQLException;

    java.sql.Ref getRef(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.RowId getRowId(int columnIndex) throws java.sql.SQLException;

    java.sql.RowId getRowId(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.SQLXML getSQLXML(int columnIndex) throws java.sql.SQLException;

    java.sql.SQLXML getSQLXML(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Time getTime(int columnIndex) throws java.sql.SQLException;

    java.sql.Time getTime(int columnIndex, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.Time getTime(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Time getTime(java.lang.String columnLabel, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(int columnIndex) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(int columnIndex, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(java.lang.String columnLabel, java.util.Calendar cal) throws java.sql.SQLException;

    void cancelRowUpdates() throws java.sql.SQLException;

    void deleteRow() throws java.sql.SQLException;

    void insertRow() throws java.sql.SQLException;

    void moveToCurrentRow() throws java.sql.SQLException;

    void moveToInsertRow() throws java.sql.SQLException;

    void refreshRow() throws java.sql.SQLException;

    void updateArray(int columnIndex, java.sql.Array x) throws java.sql.SQLException;

    void updateArray(java.lang.String columnLabel, java.sql.Array x) throws java.sql.SQLException;

    void updateAsciiStream(int columnIndex, java.io.InputStream x) throws java.sql.SQLException;

    void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) throws java.sql.SQLException;

    void updateAsciiStream(int columnIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateAsciiStream(java.lang.String columnLabel, java.io.InputStream x) throws java.sql.SQLException;

    void updateAsciiStream(java.lang.String columnLabel, java.io.InputStream x, int length) throws java.sql.SQLException;

    void updateAsciiStream(java.lang.String columnLabel, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBigDecimal(int columnIndex, java.math.BigDecimal x) throws java.sql.SQLException;

    void updateBigDecimal(java.lang.String columnLabel, java.math.BigDecimal x) throws java.sql.SQLException;

    void updateBinaryStream(int columnIndex, java.io.InputStream x) throws java.sql.SQLException;

    void updateBinaryStream(int columnIndex, java.io.InputStream x, int length) throws java.sql.SQLException;

    void updateBinaryStream(int columnIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBinaryStream(java.lang.String columnLabel, java.io.InputStream x) throws java.sql.SQLException;

    void updateBinaryStream(java.lang.String columnLabel, java.io.InputStream x, int length) throws java.sql.SQLException;

    void updateBinaryStream(java.lang.String columnLabel, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBlob(int columnIndex, java.io.InputStream x) throws java.sql.SQLException;

    void updateBlob(int columnIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBlob(int columnIndex, java.sql.Blob x) throws java.sql.SQLException;

    void updateBlob(java.lang.String columnLabel, java.io.InputStream x) throws java.sql.SQLException;

    void updateBlob(java.lang.String columnLabel, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBlob(java.lang.String columnLabel, java.sql.Blob x) throws java.sql.SQLException;

    void updateBoolean(int columnIndex, boolean x) throws java.sql.SQLException;

    void updateBoolean(java.lang.String columnLabel, boolean x) throws java.sql.SQLException;

    void updateByte(int columnIndex, byte x) throws java.sql.SQLException;

    void updateByte(java.lang.String columnLabel, byte x) throws java.sql.SQLException;

    void updateBytes(int columnIndex, byte[] x) throws java.sql.SQLException;

    void updateBytes(java.lang.String columnLabel, byte[] x) throws java.sql.SQLException;

    void updateCharacterStream(int columnIndex, java.io.Reader x) throws java.sql.SQLException;

    void updateCharacterStream(int columnIndex, java.io.Reader x, int length) throws java.sql.SQLException;

    void updateCharacterStream(int columnIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateCharacterStream(java.lang.String columnLabel, java.io.Reader x) throws java.sql.SQLException;

    void updateCharacterStream(java.lang.String columnLabel, java.io.Reader x, int length) throws java.sql.SQLException;

    void updateCharacterStream(java.lang.String columnLabel, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateClob(int columnIndex, java.io.Reader x) throws java.sql.SQLException;

    void updateClob(int columnIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateClob(int columnIndex, java.sql.Clob x) throws java.sql.SQLException;

    void updateClob(java.lang.String columnLabel, java.io.Reader x) throws java.sql.SQLException;

    void updateClob(java.lang.String columnLabel, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateClob(java.lang.String columnLabel, java.sql.Clob x) throws java.sql.SQLException;

    void updateDate(int columnIndex, java.sql.Date x) throws java.sql.SQLException;

    void updateDate(java.lang.String columnLabel, java.sql.Date x) throws java.sql.SQLException;

    void updateDouble(int columnIndex, double x) throws java.sql.SQLException;

    void updateDouble(java.lang.String columnLabel, double x) throws java.sql.SQLException;

    void updateFloat(int columnIndex, float x) throws java.sql.SQLException;

    void updateFloat(java.lang.String columnLabel, float x) throws java.sql.SQLException;

    void updateInt(int columnIndex, int x) throws java.sql.SQLException;

    void updateInt(java.lang.String columnLabel, int x) throws java.sql.SQLException;

    void updateLong(int columnIndex, long x) throws java.sql.SQLException;

    void updateLong(java.lang.String columnLabel, long x) throws java.sql.SQLException;

    void updateNCharacterStream(int columnIndex, java.io.Reader x) throws java.sql.SQLException;

    void updateNCharacterStream(int columnIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateNCharacterStream(java.lang.String columnLabel, java.io.Reader x) throws java.sql.SQLException;

    void updateNCharacterStream(java.lang.String columnLabel, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateNClob(int columnIndex, java.io.Reader x) throws java.sql.SQLException;

    void updateNClob(int columnIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateNClob(int columnIndex, java.sql.NClob x) throws java.sql.SQLException;

    void updateNClob(java.lang.String columnLabel, java.io.Reader x) throws java.sql.SQLException;

    void updateNClob(java.lang.String columnLabel, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateNClob(java.lang.String columnLabel, java.sql.NClob x) throws java.sql.SQLException;

    void updateNString(int columnIndex, java.lang.String x) throws java.sql.SQLException;

    void updateNString(java.lang.String columnLabel, java.lang.String x) throws java.sql.SQLException;

    void updateNull(int columnIndex) throws java.sql.SQLException;

    void updateNull(java.lang.String columnLabel) throws java.sql.SQLException;

    void updateObject(int columnIndex, java.lang.Object x) throws java.sql.SQLException;

    void updateObject(int columnIndex, java.lang.Object x, int scaleOrLength) throws java.sql.SQLException;

    default void updateObject(int columnIndex, java.lang.Object x, java.sql.SQLType targetSqlType) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("updateObject not implemented");
    }

    default void updateObject(int columnIndex, java.lang.Object x, java.sql.SQLType targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("updateObject not implemented");
    }

    void updateObject(java.lang.String columnLabel, java.lang.Object x) throws java.sql.SQLException;

    void updateObject(java.lang.String columnLabel, java.lang.Object x, int scaleOrLength) throws java.sql.SQLException;

    default void updateObject(java.lang.String columnLabel, java.lang.Object x, java.sql.SQLType targetSqlType) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("updateObject not implemented");
    }

    default void updateObject(java.lang.String columnLabel, java.lang.Object x, java.sql.SQLType targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("updateObject not implemented");
    }

    void updateRef(int columnIndex, java.sql.Ref x) throws java.sql.SQLException;

    void updateRef(java.lang.String columnLabel, java.sql.Ref x) throws java.sql.SQLException;

    void updateRow() throws java.sql.SQLException;

    void updateRowId(int columnIndex, java.sql.RowId x) throws java.sql.SQLException;

    void updateRowId(java.lang.String columnLabel, java.sql.RowId x) throws java.sql.SQLException;

    void updateSQLXML(int columnIndex, java.sql.SQLXML x) throws java.sql.SQLException;

    void updateSQLXML(java.lang.String columnLabel, java.sql.SQLXML x) throws java.sql.SQLException;

    void updateShort(int columnIndex, short x) throws java.sql.SQLException;

    void updateShort(java.lang.String columnLabel, short x) throws java.sql.SQLException;

    void updateString(int columnIndex, java.lang.String x) throws java.sql.SQLException;

    void updateString(java.lang.String columnLabel, java.lang.String x) throws java.sql.SQLException;

    void updateTime(int columnIndex, java.sql.Time x) throws java.sql.SQLException;

    void updateTime(java.lang.String columnLabel, java.sql.Time x) throws java.sql.SQLException;

    void updateTimestamp(int columnIndex, java.sql.Timestamp x) throws java.sql.SQLException;

    void updateTimestamp(java.lang.String columnLabel, java.sql.Timestamp x) throws java.sql.SQLException;
}
