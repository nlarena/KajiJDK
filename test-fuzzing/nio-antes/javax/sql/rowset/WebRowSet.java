package javax.sql.rowset;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A {@link CachedRowSet} that knows how to write and read itself as XML.
 *
 * <h2>What is serialized, and why the rows are not enough</h2>
 *
 * <p>The document carries three things: the <strong>properties</strong> of the set (the query, the
 * data source, the cursor type), the <strong>metadata</strong> of each column, and the
 * <strong>data</strong> — which in turn includes, for the modified rows, the original value as well
 * as the current one.
 *
 * <p>That last part is what makes it worthwhile. A set rebuilt from the XML can synchronize with
 * the database just as if it had never moved, because it has something to compare against.
 * Serializing only the rows would produce something that can be shown and cannot be written.
 *
 * <h2>What it is concretely for</h2>
 *
 * <p>To move a set between processes that do not share classes: a client asks for data, receives it
 * as XML, modifies it without a connection and returns the document; the server rebuilds it and
 * synchronizes it. It is the text version of what {@code Serializable} does in binary, with the
 * advantage that there may be no Java on the other side.
 *
 * <h2>The two forms of each method</h2>
 *
 * <p>There is a version with {@code Reader}/{@code Writer} and one with {@code InputStream}/{@code
 * OutputStream}. They are not interchangeable: the byte-stream one is the right one, because an XML
 * document declares its own encoding inside and it can only be honoured if bytes are read. The
 * character one forces the caller to have chosen the encoding correctly beforehand.
 *
 * @since 1.5
 */
public interface WebRowSet extends CachedRowSet {

    /** The public identifier of the XML schema of a {@code WebRowSet}. */
    String PUBLIC_XML_SCHEMA = "--//Oracle Corporation//XSD Schema//EN";

    /** Where the schema lives. */
    String SCHEMA_SYSTEM_ID = "http://java.sun.com/xml/ns/jdbc/webrowset.xsd";

    /**
     * Fills the set from an XML document.
     *
     * @param reader where to read from
     * @throws SQLException if the document is malformed or does not match the schema
     */
    void readXml(Reader reader) throws SQLException;

    /**
     * Fills the set from an XML document.
     *
     * @param iStream where to read from
     * @throws SQLException if the document does not match the schema
     * @throws IOException if it could not be read
     */
    void readXml(InputStream iStream) throws SQLException, IOException;

    /**
     * Writes the content of a {@code ResultSet} as XML.
     *
     * @param rs the result to write
     * @param writer where to write
     * @throws SQLException if the result could not be read or the document written
     */
    void writeXml(ResultSet rs, Writer writer) throws SQLException;

    /**
     * Writes the content of a {@code ResultSet} as XML.
     *
     * @param rs the result to write
     * @param oStream where to write
     * @throws SQLException if the result could not be read
     * @throws IOException if it could not be written
     */
    void writeXml(ResultSet rs, OutputStream oStream) throws SQLException, IOException;

    /**
     * Writes this set as XML.
     *
     * @param writer where to write
     * @throws SQLException if it could not be written
     */
    void writeXml(Writer writer) throws SQLException;

    /**
     * Writes this set as XML.
     *
     * @param oStream where to write
     * @throws SQLException if the document could not be built
     * @throws IOException if it could not be written
     */
    void writeXml(OutputStream oStream) throws SQLException, IOException;
}
