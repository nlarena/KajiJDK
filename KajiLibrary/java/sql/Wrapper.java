package java.sql;

/**
 * KajiLibrary's java.sql.Wrapper -- reaching the real implementation underneath a JDBC object.
 *
 * <p>It exists because of wrappers: a connection pool hands out a `Connection` that **is not** the
 * driver's but one that wraps it so as to return it to the pool on closing. Whoever needs a
 * function of the driver's own stays on the outside, and this pair of methods is the door -- with
 * the advantage that one can **ask first** ({@link #isWrapperFor}) instead of trying and catching.
 */
public interface Wrapper {

    /**
     * This object seen as `iface`, going through the wrappers.
     *
     * @throws SQLException if there is nothing underneath that implements `iface`
     */
    <T> T unwrap(Class<T> iface) throws SQLException;

    /** Whether {@link #unwrap} with that type will work. */
    boolean isWrapperFor(Class<?> iface) throws SQLException;
}
