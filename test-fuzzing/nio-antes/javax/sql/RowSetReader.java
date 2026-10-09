package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetReader -- fills a disconnected {@link RowSet}.
 *
 * <p>A single method, and that is why the set need not know where its data comes from: it can be a
 * query, a file or none of that.
 */
public interface RowSetReader {

    /** Fills that set. */
    void readData(RowSetInternal caller) throws java.sql.SQLException;
}
