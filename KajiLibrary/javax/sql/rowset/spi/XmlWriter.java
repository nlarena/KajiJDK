package javax.sql.rowset.spi;

import java.io.Writer;
import java.sql.SQLException;

import javax.sql.RowSetWriter;
import javax.sql.rowset.WebRowSet;

/**
 * The writer that dumps a {@link WebRowSet} to XML.
 *
 * <p>It is the counterpart of {@link XmlReader} and writes the same thing that one expects:
 * properties, metadata, current rows and original values of the modified ones.
 *
 * <p>Unlike an ordinary {@link RowSetWriter}, this synchronizes with nothing: it does not return
 * the changes to the source, it <strong>serializes</strong> them. The destination is a document,
 * not a database.
 *
 * @since 1.5
 */
public interface XmlWriter extends RowSetWriter {

    /**
     * Writes the whole set as XML.
     *
     * @param caller the set to write
     * @param writer where to write it
     * @throws SQLException if it could not be written
     */
    void writeXML(WebRowSet caller, Writer writer) throws SQLException;
}
