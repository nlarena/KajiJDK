package javax.sql.rowset.spi;

import java.io.Reader;
import java.sql.SQLException;

import javax.sql.RowSetReader;
import javax.sql.rowset.WebRowSet;

/**
 * The reader that fills a {@link WebRowSet} from XML.
 *
 * <p>What it reads is not only the data: the document of a {@code WebRowSet} also carries the
 * properties of the set, its column metadata and —what makes it useful— the
 * <strong>original</strong> values of the modified rows. Without that, the rebuilt set could not
 * detect conflicts when going back to the source, because it would not know what to compare
 * against.
 *
 * <p>It is what allows a {@code RowSet} to travel over the network as text and arrive on the other
 * side able to synchronize just as if it had not moved.
 *
 * @since 1.5
 */
public interface XmlReader extends RowSetReader {

    /**
     * Fills the set with whatever there is in the stream.
     *
     * @param caller the set to fill
     * @param reader where to read the XML from
     * @throws SQLException if the document is malformed or does not match the schema
     */
    void readXML(WebRowSet caller, Reader reader) throws SQLException;
}
