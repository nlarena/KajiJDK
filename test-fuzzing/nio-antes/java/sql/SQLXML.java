package java.sql;

/**
 * KajiLibrary's java.sql.SQLXML -- an XML value from the database.
 *
 * <p>It is read and written **only once**: as soon as the content has been asked for in one form,
 * the others are closed. That is what lets the implementation stream it instead of keeping it
 * whole.
 */
public interface SQLXML {

    /** The content as text. */
    String getString() throws SQLException;

    /** Sets the content from a text. */
    void setString(String value) throws SQLException;

    /** The content as a byte stream. */
    java.io.InputStream getBinaryStream() throws SQLException;

    /** A stream to write it. */
    java.io.OutputStream setBinaryStream() throws SQLException;

    /** The content as a reader. */
    java.io.Reader getCharacterStream() throws SQLException;

    /** A writer to write it. */
    java.io.Writer setCharacterStream() throws SQLException;

    /**
     * The content as whichever {@link javax.xml.transform.Source} class is asked for.
     *
     * <p>It receives the class instead of having one overload per representation, and it returns
     * **that** class and not the interface: that is what avoids the cast in the caller, which is
     * exactly where a cast would be a run-time error and not a compile-time one.
     *
     * @param sourceClass the class asked for, or `null` for whichever the driver prefers
     */
    <T extends javax.xml.transform.Source> T getSource(Class<T> sourceClass) throws SQLException;

    /** The mirror of the previous one, for writing. */
    <T extends javax.xml.transform.Result> T setResult(Class<T> resultClass) throws SQLException;

    /** Releases the pointer's resources. */
    void free() throws SQLException;
}
