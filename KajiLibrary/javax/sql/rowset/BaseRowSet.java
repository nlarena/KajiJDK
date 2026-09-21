package javax.sql.rowset;

import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.sql.RowSetEvent;
import javax.sql.RowSetListener;

/**
 * The base of every {@code RowSet}: it keeps the properties and the parameters, and notifies the
 * listeners.
 *
 * <h2>What it does and what it does not</h2>
 *
 * <p>This class <strong>has no rows</strong>. It keeps the query, the URL, the user, the cursor
 * type and the parameters that are going to replace the question marks — everything that is needed
 * <em>before</em> fetching data. The data is the subclass's problem.
 *
 * <p>That division is what allows a {@code CachedRowSet} and a {@code JdbcRowSet}, which keep their
 * rows in completely different ways, to share the hundred-odd properties and setters they have in
 * common.
 *
 * <h2>The parameters, and why they are kept instead of applied</h2>
 *
 * <p>A {@code RowSet} is a JavaBeans-style component: first it is configured, then it is executed.
 * When somebody calls {@code setInt(1, 42)} there may still be no connection nor prepared statement
 * to put that 42 in, so it is kept in an indexed map and the subclass retrieves it with
 * {@link #getParams} at execution time.
 *
 * <p>Two visible oddities come from that. Indices are 1-based on the outside and 0-based in the
 * map, which is the only subtraction in the whole class. And the parameters that carry more than
 * one datum —{@code setNull} with its SQL type, {@code setObject} with a scale, the streams with
 * their length— are kept as an {@code Object[]}, because the map has a single slot per position.
 *
 * <h2>Parameters by name are not here</h2>
 *
 * <p>The three dozen {@code setXxx(String, ...)} methods throw {@link
 * SQLFeatureNotSupportedException}. It is not a gap of this library: it is what the JDK does. They
 * are declared because {@code RowSet} declares them, and they do not work because a JDBC {@code
 * PreparedStatement} does not accept parameters by name — there would be nowhere to send them.
 *
 * <h2>The four {@code protected} stream fields</h2>
 *
 * <p>{@link #binaryStream}, {@link #unicodeStream}, {@link #asciiStream} and {@link #charStream}
 * are a leftover of an earlier version, when the last stream set was kept apart for the subclass to
 * reach. Today the streams go into the parameter map like everything else. They are kept because
 * they are {@code protected} API and some outside subclass may be reading them.
 *
 * @since 1.5
 */
public abstract class BaseRowSet implements Serializable, Cloneable {

    private static final long serialVersionUID = 4886719666485113312L;

    /** Marks that a stream parameter is of Unicode characters. */
    public static final int UNICODE_STREAM_PARAM = 0;

    /** Marks that a stream parameter is binary. */
    public static final int BINARY_STREAM_PARAM = 1;

    /** Marks that a stream parameter is of ASCII characters. */
    public static final int ASCII_STREAM_PARAM = 2;

    /** The last binary stream set; see the class note. */
    protected InputStream binaryStream;

    /** The last Unicode stream set; see the class note. */
    protected InputStream unicodeStream;

    /** The last ASCII stream set; see the class note. */
    protected InputStream asciiStream;

    /** The last character stream set; see the class note. */
    protected Reader charStream;

    private String command;
    private String url;
    private String dataSource;
    private transient String username;
    private transient String password;

    private int rowSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;
    private int concurrency = ResultSet.CONCUR_UPDATABLE;
    private boolean readOnly;
    private boolean escapeProcessing = true;
    private int isolation = Connection.TRANSACTION_READ_COMMITTED;
    private int fetchDir = ResultSet.FETCH_FORWARD;
    private int fetchSize;
    private int maxFieldSize;
    private int maxRows;
    private int queryTimeout;
    private boolean showDeleted;
    private Map<String, Class<?>> map;

    private transient Vector<RowSetListener> listeners = new Vector<RowSetListener>();

    /** Parameters by position, 0-based; see the class note. */
    private Hashtable<Integer, Object> params;

    /** For the subclasses. */
    public BaseRowSet() {
    }

    /**
     * Prepares the parameter map, emptying it if it already existed.
     *
     * <p>It has to be called before the first {@code setXxx}. The setters do not do it by
     * themselves on purpose: they fail saying it is missing, which is easier to diagnose than a map
     * that appears halfway through.
     */
    protected void initParams() {
        params = new Hashtable<Integer, Object>();
    }

    private Hashtable<Integer, Object> paramsFor(final String caller) throws SQLException {
        if (params == null) {
            throw new SQLException("Set initParams() before " + caller);
        }
        return params;
    }

    /** Parameter indices are 1-based, as in all of JDBC. */
    private void checkParamIndex(final int idx) throws SQLException {
        if (idx < 1) {
            throw new SQLException("the parameter index has to be greater than or equal to 1");
        }
    }

    private void store(final int idx, final Object v, final String caller) throws SQLException {
        checkParamIndex(idx);
        paramsFor(caller).put(Integer.valueOf(idx - 1), v);
    }

    private static SQLFeatureNotSupportedException byNameUnsupported() {
        return new SQLFeatureNotSupportedException("Feature not supported");
    }

    // ---- listeners ----

    /**
     * Adds a listener.
     *
     * @param listener the listener
     */
    public void addRowSetListener(final RowSetListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener the listener
     */
    public void removeRowSetListener(final RowSetListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies the listeners that the cursor moved.
     *
     * <p>The three notices walk a copy of the list: a listener that unregisters itself while being
     * notified would leave the iteration over a list that changed underneath.
     */
    protected void notifyCursorMoved() {
        final RowSetEvent e = new RowSetEvent((javax.sql.RowSet) this);
        for (final RowSetListener l : listenersCopy()) {
            l.cursorMoved(e);
        }
    }

    /** Notifies the listeners that the current row changed. */
    protected void notifyRowChanged() {
        final RowSetEvent e = new RowSetEvent((javax.sql.RowSet) this);
        for (final RowSetListener l : listenersCopy()) {
            l.rowChanged(e);
        }
    }

    /** Notifies the listeners that the whole set changed. */
    protected void notifyRowSetChanged() {
        final RowSetEvent e = new RowSetEvent((javax.sql.RowSet) this);
        for (final RowSetListener l : listenersCopy()) {
            l.rowSetChanged(e);
        }
    }

    private RowSetListener[] listenersCopy() {
        synchronized (listeners) {
            return listeners.toArray(new RowSetListener[listeners.size()]);
        }
    }

    // ---- properties ----

    /**
     * The query to execute.
     *
     * @return the query, or {@code null}
     */
    public String getCommand() {
        return command;
    }

    /**
     * Sets the query and discards whatever parameters there were.
     *
     * <p>It discards them because they belonged to the previous query: the question marks of the
     * new query are elsewhere and mean something else. Keeping them would be passing values to
     * positions that no longer correspond to them.
     *
     * @param cmd the query
     * @throws SQLException if the query is an empty string
     */
    public void setCommand(final String cmd) throws SQLException {
        if (cmd != null && cmd.trim().length() == 0) {
            throw new SQLException("the query cannot be empty");
        }
        command = cmd;
        if (params != null) {
            initParams();
        }
    }

    /**
     * The JDBC URL.
     *
     * @return the URL, or {@code null}
     * @throws SQLException never
     */
    public String getUrl() throws SQLException {
        return url;
    }

    /**
     * Sets the JDBC URL.
     *
     * @param url the URL
     * @throws SQLException if it is an empty string
     */
    public void setUrl(final String url) throws SQLException {
        if (url != null && url.trim().length() == 0) {
            throw new SQLException("the URL cannot be empty");
        }
        this.url = url;
    }

    /**
     * The JNDI name of the data source.
     *
     * @return the name, or {@code null}
     */
    public String getDataSourceName() {
        return dataSource;
    }

    /**
     * Sets the JNDI name of the data source and forgets the URL.
     *
     * <p>They are two mutually exclusive ways of reaching the database, and having both set would
     * leave undefined which one wins. Setting one erases the other.
     *
     * @param name the JNDI name
     * @throws SQLException if it is an empty string
     */
    public void setDataSourceName(final String name) throws SQLException {
        if (name != null && name.trim().length() == 0) {
            throw new SQLException("the data source name cannot be empty");
        }
        dataSource = name;
        url = null;
    }

    /**
     * The user.
     *
     * @return the user, or {@code null}
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user.
     *
     * @param name the user
     */
    public void setUsername(final String name) {
        username = name;
    }

    /**
     * The password.
     *
     * @return the password, or {@code null}
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * <p>The field is {@code transient}, like the user's: a {@code RowSet} is serialized and
     * travels, and the credentials should not travel with it.
     *
     * @param pass the password
     */
    public void setPassword(final String pass) {
        password = pass;
    }

    /**
     * Sets the cursor type.
     *
     * @param type one of the {@code TYPE_} constants of {@code ResultSet}
     * @throws SQLException if the value is not one of them
     */
    public void setType(final int type) throws SQLException {
        if (type != ResultSet.TYPE_FORWARD_ONLY && type != ResultSet.TYPE_SCROLL_INSENSITIVE
                && type != ResultSet.TYPE_SCROLL_SENSITIVE) {
            throw new SQLException("invalid cursor type: " + type);
        }
        rowSetType = type;
    }

    /**
     * The cursor type.
     *
     * @return one of the {@code TYPE_} constants
     * @throws SQLException never
     */
    public int getType() throws SQLException {
        return rowSetType;
    }

    /**
     * Sets the concurrency.
     *
     * @param concurrency {@code CONCUR_READ_ONLY} or {@code CONCUR_UPDATABLE}
     * @throws SQLException if the value is not one of those two
     */
    public void setConcurrency(final int concurrency) throws SQLException {
        if (concurrency != ResultSet.CONCUR_READ_ONLY
                && concurrency != ResultSet.CONCUR_UPDATABLE) {
            throw new SQLException("concurrencia invalida: " + concurrency);
        }
        this.concurrency = concurrency;
    }

    /**
     * The concurrency.
     *
     * @return {@code CONCUR_READ_ONLY} or {@code CONCUR_UPDATABLE}
     * @throws SQLException never
     */
    public int getConcurrency() throws SQLException {
        return concurrency;
    }

    /**
     * Whether the set is read-only.
     *
     * @return whether it is
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Marks the set as read-only.
     *
     * @param value whether to mark it
     */
    public void setReadOnly(final boolean value) {
        readOnly = value;
    }

    /**
     * The transaction isolation level.
     *
     * @return one of the {@code TRANSACTION_} constants of {@code Connection}
     */
    public int getTransactionIsolation() {
        return isolation;
    }

    /**
     * Sets the isolation level.
     *
     * @param level one of the {@code TRANSACTION_} constants
     * @throws SQLException if the value is not one of them
     */
    public void setTransactionIsolation(final int level) throws SQLException {
        if (level != Connection.TRANSACTION_NONE
                && level != Connection.TRANSACTION_READ_UNCOMMITTED
                && level != Connection.TRANSACTION_READ_COMMITTED
                && level != Connection.TRANSACTION_REPEATABLE_READ
                && level != Connection.TRANSACTION_SERIALIZABLE) {
            throw new SQLException("invalid isolation level: " + level);
        }
        isolation = level;
    }

    /**
     * The map from SQL types to Java classes.
     *
     * @return the map, or {@code null} if none was set
     */
    public Map<String, Class<?>> getTypeMap() {
        return map;
    }

    /**
     * Sets the type map.
     *
     * @param map the map
     */
    public void setTypeMap(final Map<String, Class<?>> map) {
        this.map = map;
    }

    /**
     * The cap on bytes per column.
     *
     * @return the cap; zero is no cap
     * @throws SQLException never
     */
    public int getMaxFieldSize() throws SQLException {
        return maxFieldSize;
    }

    /**
     * Sets the cap on bytes per column.
     *
     * @param max the cap; zero for no cap
     * @throws SQLException if it is negative
     */
    public void setMaxFieldSize(final int max) throws SQLException {
        if (max < 0) {
            throw new SQLException("the maximum field size cannot be negative");
        }
        maxFieldSize = max;
    }

    /**
     * The cap on rows.
     *
     * @return the cap; zero is no cap
     * @throws SQLException never
     */
    public int getMaxRows() throws SQLException {
        return maxRows;
    }

    /**
     * Sets the cap on rows.
     *
     * @param max the cap; zero for no cap
     * @throws SQLException if it is negative or less than the fetch size already set
     */
    public void setMaxRows(final int max) throws SQLException {
        if (max < 0) {
            throw new SQLException("the maximum number of rows cannot be negative");
        }
        if (max != 0 && max < fetchSize) {
            throw new SQLException(
                    "the maximum rows cannot be less than the fetch size " + fetchSize);
        }
        maxRows = max;
    }

    /**
     * Turns the processing of SQL escape sequences on or off.
     *
     * @param enable whether to process them
     * @throws SQLException never
     */
    public void setEscapeProcessing(final boolean enable) throws SQLException {
        escapeProcessing = enable;
    }

    /**
     * Whether escape sequences are processed.
     *
     * @return whether they are processed
     * @throws SQLException never
     */
    public boolean getEscapeProcessing() throws SQLException {
        return escapeProcessing;
    }

    /**
     * How many seconds the query is waited for.
     *
     * @return the seconds; zero is no limit
     * @throws SQLException never
     */
    public int getQueryTimeout() throws SQLException {
        return queryTimeout;
    }

    /**
     * Sets how many seconds to wait.
     *
     * @param seconds the seconds; zero for no limit
     * @throws SQLException if it is negative
     */
    public void setQueryTimeout(final int seconds) throws SQLException {
        if (seconds < 0) {
            throw new SQLException("the timeout cannot be negative");
        }
        queryTimeout = seconds;
    }

    /**
     * Whether deleted rows are seen when walking.
     *
     * @return whether they are seen
     * @throws SQLException never
     */
    public boolean getShowDeleted() throws SQLException {
        return showDeleted;
    }

    /**
     * Shows or hides the deleted rows.
     *
     * @param value whether to show them
     * @throws SQLException never
     */
    public void setShowDeleted(final boolean value) throws SQLException {
        showDeleted = value;
    }

    /**
     * Sets in which direction the rows are going to be read.
     *
     * @param direction one of the {@code FETCH_} constants of {@code ResultSet}
     * @throws SQLException if the value is not one of them, or if the cursor is forward-only and
     *     another direction is asked for
     */
    public void setFetchDirection(final int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD && direction != ResultSet.FETCH_REVERSE
                && direction != ResultSet.FETCH_UNKNOWN) {
            throw new SQLException("invalid fetch direction: " + direction);
        }
        // A forward-only cursor cannot read backwards nor admit "unknown": the only direction
        // coherent with its type is forward.
        if (rowSetType == ResultSet.TYPE_FORWARD_ONLY && direction != ResultSet.FETCH_FORWARD) {
            throw new SQLException("a forward-only cursor only admits FETCH_FORWARD");
        }
        fetchDir = direction;
    }

    /**
     * The reading direction.
     *
     * @return one of the {@code FETCH_} constants
     * @throws SQLException never
     */
    public int getFetchDirection() throws SQLException {
        return fetchDir;
    }

    /**
     * How many rows are fetched at a time.
     *
     * @param rows the fetch size; zero lets the driver decide
     * @throws SQLException if it is negative or exceeds the cap on rows
     */
    public void setFetchSize(final int rows) throws SQLException {
        if (rows < 0) {
            throw new SQLException("the fetch size cannot be negative");
        }
        if (maxRows != 0 && rows > maxRows) {
            throw new SQLException(
                    "the fetch size cannot exceed the maximum rows " + maxRows);
        }
        fetchSize = rows;
    }

    /**
     * The fetch size.
     *
     * @return the size
     * @throws SQLException never
     */
    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    // ---- parameters by position ----

    /**
     * The parameters set, ordered by position.
     *
     * <p>It is what the subclass uses when executing. A position nobody set is left {@code null},
     * which cannot be told apart from a {@code setNull}; that is why {@code setNull} keeps an array
     * with the SQL type inside instead of keeping a bare {@code null}.
     *
     * @return the parameters
     * @throws SQLException never
     */
    public Object[] getParams() throws SQLException {
        if (params == null) {
            initParams();
            return new Object[0];
        }
        final Object[] out = new Object[params.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = params.get(Integer.valueOf(i));
        }
        return out;
    }

    /**
     * Erases all the parameters.
     *
     * @throws SQLException never
     */
    public void clearParameters() throws SQLException {
        if (params != null) {
            params.clear();
        }
    }

    /**
     * A null parameter, with its SQL type.
     *
     * @param parameterIndex the position, from 1
     * @param sqlType the SQL type
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        store(parameterIndex, new Object[] { null, Integer.valueOf(sqlType) }, "setNull");
    }

    /**
     * A null parameter of a user-defined type.
     *
     * @param parameterIndex the position, from 1
     * @param sqlType the SQL type
     * @param typeName the name of the type
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setNull(final int parameterIndex, final int sqlType, final String typeName)
            throws SQLException {
        store(parameterIndex, new Object[] { null, Integer.valueOf(sqlType), typeName }, "setNull");
    }

    /**
     * A boolean parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
        store(parameterIndex, Boolean.valueOf(x), "setBoolean");
    }

    /**
     * A {@code byte} parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setByte(final int parameterIndex, final byte x) throws SQLException {
        store(parameterIndex, Byte.valueOf(x), "setByte");
    }

    /**
     * A {@code short} parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setShort(final int parameterIndex, final short x) throws SQLException {
        store(parameterIndex, Short.valueOf(x), "setShort");
    }

    /**
     * An {@code int} parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setInt(final int parameterIndex, final int x) throws SQLException {
        store(parameterIndex, Integer.valueOf(x), "setInt");
    }

    /**
     * A {@code long} parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setLong(final int parameterIndex, final long x) throws SQLException {
        store(parameterIndex, Long.valueOf(x), "setLong");
    }

    /**
     * A {@code float} parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setFloat(final int parameterIndex, final float x) throws SQLException {
        store(parameterIndex, Float.valueOf(x), "setFloat");
    }

    /**
     * A {@code double} parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setDouble(final int parameterIndex, final double x) throws SQLException {
        store(parameterIndex, Double.valueOf(x), "setDouble");
    }

    /**
     * A decimal parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
        store(parameterIndex, x, "setBigDecimal");
    }

    /**
     * A text parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setString(final int parameterIndex, final String x) throws SQLException {
        store(parameterIndex, x, "setString");
    }

    /**
     * A binary parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
        store(parameterIndex, x, "setBytes");
    }

    /**
     * A date parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setDate(final int parameterIndex, final Date x) throws SQLException {
        store(parameterIndex, x, "setDate");
    }

    /**
     * A time parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setTime(final int parameterIndex, final Time x) throws SQLException {
        store(parameterIndex, x, "setTime");
    }

    /**
     * A timestamp parameter.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
        store(parameterIndex, x, "setTimestamp");
    }

    /**
     * A date with the time zone of a calendar.
     *
     * <p>The calendar is needed because an SQL date has no time zone: interpreting its days without
     * saying in which zone would give a different day depending on where the program runs.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @param cal the calendar with the time zone
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setDate(final int parameterIndex, final Date x, final Calendar cal)
            throws SQLException {
        store(parameterIndex, new Object[] { x, cal }, "setDate");
    }

    /**
     * A time with the time zone of a calendar.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @param cal the calendar with the time zone
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setTime(final int parameterIndex, final Time x, final Calendar cal)
            throws SQLException {
        store(parameterIndex, new Object[] { x, cal }, "setTime");
    }

    /**
     * A timestamp with the time zone of a calendar.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @param cal the calendar with the time zone
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal)
            throws SQLException {
        store(parameterIndex, new Object[] { x, cal }, "setTimestamp");
    }

    /**
     * An ASCII stream of known length.
     *
     * @param parameterIndex the position, from 1
     * @param x the stream
     * @param length how many bytes to read
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setAsciiStream(final int parameterIndex, final InputStream x, final int length)
            throws SQLException {
        store(parameterIndex,
                new Object[] { x, Integer.valueOf(length), Integer.valueOf(ASCII_STREAM_PARAM) },
                "setAsciiStream");
        asciiStream = x;
    }

    /**
     * An ASCII stream of unknown length.
     *
     * @param parameterIndex the position, from 1
     * @param x the stream
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setAsciiStream(final int parameterIndex, final InputStream x) throws SQLException {
        store(parameterIndex, new Object[] { x, null, Integer.valueOf(ASCII_STREAM_PARAM) },
                "setAsciiStream");
        asciiStream = x;
    }

    /**
     * A binary stream of known length.
     *
     * @param parameterIndex the position, from 1
     * @param x the stream
     * @param length how many bytes to read
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setBinaryStream(final int parameterIndex, final InputStream x, final int length)
            throws SQLException {
        store(parameterIndex,
                new Object[] { x, Integer.valueOf(length), Integer.valueOf(BINARY_STREAM_PARAM) },
                "setBinaryStream");
        binaryStream = x;
    }

    /**
     * A binary stream of unknown length.
     *
     * @param parameterIndex the position, from 1
     * @param x the stream
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setBinaryStream(final int parameterIndex, final InputStream x)
            throws SQLException {
        store(parameterIndex, new Object[] { x, null, Integer.valueOf(BINARY_STREAM_PARAM) },
                "setBinaryStream");
        binaryStream = x;
    }

    /**
     * A stream of Unicode bytes.
     *
     * @param parameterIndex the position, from 1
     * @param x the stream
     * @param length how many bytes to read
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     * @deprecated Use {@link #setCharacterStream(int, Reader, int)}. (The note said the encoding is
     *     declared nowhere; JDBC's {@code setUnicodeStream} says the bytes must be Java UTF-8.)
     */
    @Deprecated
    public void setUnicodeStream(final int parameterIndex, final InputStream x, final int length)
            throws SQLException {
        store(parameterIndex,
                new Object[] { x, Integer.valueOf(length), Integer.valueOf(UNICODE_STREAM_PARAM) },
                "setUnicodeStream");
        unicodeStream = x;
    }

    /**
     * A character stream of known length.
     *
     * @param parameterIndex the position, from 1
     * @param reader the stream
     * @param length how many characters to read
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setCharacterStream(final int parameterIndex, final Reader reader, final int length)
            throws SQLException {
        store(parameterIndex, new Object[] { reader, Integer.valueOf(length) },
                "setCharacterStream");
        charStream = reader;
    }

    /**
     * A character stream of unknown length.
     *
     * @param parameterIndex the position, from 1
     * @param reader the stream
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setCharacterStream(final int parameterIndex, final Reader reader)
            throws SQLException {
        store(parameterIndex, new Object[] { reader, null }, "setCharacterStream");
        charStream = reader;
    }

    /**
     * An object, with SQL type and scale.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @param targetSqlType the target SQL type
     * @param scale the scale, for decimals
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType,
            final int scale) throws SQLException {
        store(parameterIndex,
                new Object[] { x, Integer.valueOf(targetSqlType), Integer.valueOf(scale) },
                "setObject");
    }

    /**
     * An object, with SQL type.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @param targetSqlType the target SQL type
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType)
            throws SQLException {
        store(parameterIndex, new Object[] { x, Integer.valueOf(targetSqlType) }, "setObject");
    }

    /**
     * An object, letting the driver choose the type.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setObject(final int parameterIndex, final Object x) throws SQLException {
        store(parameterIndex, x, "setObject");
    }

    /**
     * An SQL reference.
     *
     * @param parameterIndex the position, from 1
     * @param ref the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setRef(final int parameterIndex, final Ref ref) throws SQLException {
        store(parameterIndex, ref, "setRef");
    }

    /**
     * A binary large object.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setBlob(final int parameterIndex, final Blob x) throws SQLException {
        store(parameterIndex, x, "setBlob");
    }

    /**
     * A binary large object from a stream of known length.
     *
     * @param parameterIndex the position, from 1
     * @param inputStream the stream
     * @param length how many bytes to read
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setBlob(final int parameterIndex, final InputStream inputStream, final long length)
            throws SQLException {
        store(parameterIndex, new Object[] { inputStream, Long.valueOf(length) }, "setBlob");
    }

    /**
     * A binary large object from a stream of unknown length.
     *
     * @param parameterIndex the position, from 1
     * @param inputStream the stream
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setBlob(final int parameterIndex, final InputStream inputStream)
            throws SQLException {
        store(parameterIndex, new Object[] { inputStream, null }, "setBlob");
    }

    /**
     * A character large object.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setClob(final int parameterIndex, final Clob x) throws SQLException {
        store(parameterIndex, x, "setClob");
    }

    /**
     * A character large object from a stream of known length.
     *
     * @param parameterIndex the position, from 1
     * @param reader the stream
     * @param length how many characters to read
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setClob(final int parameterIndex, final Reader reader, final long length)
            throws SQLException {
        store(parameterIndex, new Object[] { reader, Long.valueOf(length) }, "setClob");
    }

    /**
     * A character large object from a stream of unknown length.
     *
     * @param parameterIndex the position, from 1
     * @param reader the stream
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setClob(final int parameterIndex, final Reader reader) throws SQLException {
        store(parameterIndex, new Object[] { reader, null }, "setClob");
    }

    /**
     * A national character large object.
     *
     * @param parameterIndex the position, from 1
     * @param value the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
        store(parameterIndex, value, "setNClob");
    }

    /**
     * A national character large object from a stream of known length.
     *
     * @param parameterIndex the position, from 1
     * @param reader the stream
     * @param length how many characters to read
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setNClob(final int parameterIndex, final Reader reader, final long length)
            throws SQLException {
        store(parameterIndex, new Object[] { reader, Long.valueOf(length) }, "setNClob");
    }

    /**
     * A national character large object from a stream of unknown length.
     *
     * @param parameterIndex the position, from 1
     * @param reader the stream
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setNClob(final int parameterIndex, final Reader reader) throws SQLException {
        store(parameterIndex, new Object[] { reader, null }, "setNClob");
    }

    /**
     * An SQL array.
     *
     * @param parameterIndex the position, from 1
     * @param array the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setArray(final int parameterIndex, final Array array) throws SQLException {
        store(parameterIndex, array, "setArray");
    }

    /**
     * An XML value.
     *
     * @param parameterIndex the position, from 1
     * @param xmlObject the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setSQLXML(final int parameterIndex, final SQLXML xmlObject) throws SQLException {
        store(parameterIndex, xmlObject, "setSQLXML");
    }

    /**
     * A row identifier.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setRowId(final int parameterIndex, final RowId x) throws SQLException {
        store(parameterIndex, x, "setRowId");
    }

    /**
     * A national character string.
     *
     * @param parameterIndex the position, from 1
     * @param value the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setNString(final int parameterIndex, final String value) throws SQLException {
        store(parameterIndex, value, "setNString");
    }

    /**
     * A national character stream of known length.
     *
     * @param parameterIndex the position, from 1
     * @param value the stream
     * @param length how many characters to read
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setNCharacterStream(final int parameterIndex, final Reader value,
            final long length) throws SQLException {
        store(parameterIndex, new Object[] { value, Long.valueOf(length) },
                "setNCharacterStream");
    }

    /**
     * A national character stream of unknown length.
     *
     * @param parameterIndex the position, from 1
     * @param value the stream
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setNCharacterStream(final int parameterIndex, final Reader value)
            throws SQLException {
        store(parameterIndex, new Object[] { value, null }, "setNCharacterStream");
    }

    /**
     * A URL.
     *
     * @param parameterIndex the position, from 1
     * @param x the value
     * @throws SQLException if the index is less than 1 or {@link #initParams} was not called
     */
    public void setURL(final int parameterIndex, final URL x) throws SQLException {
        store(parameterIndex, x, "setURL");
    }

    // ---- parameters by name: none is supported, see the class note ----

    /**
     * @param parameterName the name
     * @param sqlType the SQL type
     * @throws SQLFeatureNotSupportedException always
     */
    public void setNull(final String parameterName, final int sqlType) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param sqlType the SQL type
     * @param typeName the name of the type
     * @throws SQLFeatureNotSupportedException always
     */
    public void setNull(final String parameterName, final int sqlType, final String typeName)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setBoolean(final String parameterName, final boolean x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setByte(final String parameterName, final byte x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setShort(final String parameterName, final short x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setInt(final String parameterName, final int x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setLong(final String parameterName, final long x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setFloat(final String parameterName, final float x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setDouble(final String parameterName, final double x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setBigDecimal(final String parameterName, final BigDecimal x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setString(final String parameterName, final String x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setBytes(final String parameterName, final byte[] x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setDate(final String parameterName, final Date x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @param cal the calendar
     * @throws SQLFeatureNotSupportedException always
     */
    public void setDate(final String parameterName, final Date x, final Calendar cal)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setTime(final String parameterName, final Time x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @param cal the calendar
     * @throws SQLFeatureNotSupportedException always
     */
    public void setTime(final String parameterName, final Time x, final Calendar cal)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setTimestamp(final String parameterName, final Timestamp x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @param cal the calendar
     * @throws SQLFeatureNotSupportedException always
     */
    public void setTimestamp(final String parameterName, final Timestamp x, final Calendar cal)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the stream
     * @param length the length
     * @throws SQLFeatureNotSupportedException always
     */
    public void setAsciiStream(final String parameterName, final InputStream x, final int length)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the stream
     * @throws SQLFeatureNotSupportedException always
     */
    public void setAsciiStream(final String parameterName, final InputStream x)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the stream
     * @param length the length
     * @throws SQLFeatureNotSupportedException always
     */
    public void setBinaryStream(final String parameterName, final InputStream x, final int length)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the stream
     * @throws SQLFeatureNotSupportedException always
     */
    public void setBinaryStream(final String parameterName, final InputStream x)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param reader the stream
     * @param length the length
     * @throws SQLFeatureNotSupportedException always
     */
    public void setCharacterStream(final String parameterName, final Reader reader,
            final int length) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param reader the stream
     * @throws SQLFeatureNotSupportedException always
     */
    public void setCharacterStream(final String parameterName, final Reader reader)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param value the stream
     * @param length the length
     * @throws SQLFeatureNotSupportedException always
     */
    public void setNCharacterStream(final String parameterName, final Reader value,
            final long length) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param value the stream
     * @throws SQLFeatureNotSupportedException always
     */
    public void setNCharacterStream(final String parameterName, final Reader value)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @param targetSqlType the SQL type
     * @param scale the scale
     * @throws SQLFeatureNotSupportedException always
     */
    public void setObject(final String parameterName, final Object x, final int targetSqlType,
            final int scale) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @param targetSqlType the SQL type
     * @throws SQLFeatureNotSupportedException always
     */
    public void setObject(final String parameterName, final Object x, final int targetSqlType)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setObject(final String parameterName, final Object x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setBlob(final String parameterName, final Blob x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param inputStream the stream
     * @param length the length
     * @throws SQLFeatureNotSupportedException always
     */
    public void setBlob(final String parameterName, final InputStream inputStream,
            final long length) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param inputStream the stream
     * @throws SQLFeatureNotSupportedException always
     */
    public void setBlob(final String parameterName, final InputStream inputStream)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setClob(final String parameterName, final Clob x) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param reader the stream
     * @param length the length
     * @throws SQLFeatureNotSupportedException always
     */
    public void setClob(final String parameterName, final Reader reader, final long length)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param reader the stream
     * @throws SQLFeatureNotSupportedException always
     */
    public void setClob(final String parameterName, final Reader reader) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param value the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setNClob(final String parameterName, final NClob value) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param reader the stream
     * @param length the length
     * @throws SQLFeatureNotSupportedException always
     */
    public void setNClob(final String parameterName, final Reader reader, final long length)
            throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param reader the stream
     * @throws SQLFeatureNotSupportedException always
     */
    public void setNClob(final String parameterName, final Reader reader) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param value the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setNString(final String parameterName, final String value) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param xmlObject the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setSQLXML(final String parameterName, final SQLXML xmlObject) throws SQLException {
        throw byNameUnsupported();
    }

    /**
     * @param parameterName the name
     * @param x the value
     * @throws SQLFeatureNotSupportedException always
     */
    public void setRowId(final String parameterName, final RowId x) throws SQLException {
        throw byNameUnsupported();
    }
}
