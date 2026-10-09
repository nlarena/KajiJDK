package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetListener -- finds out what happens to a {@link RowSet}.
 *
 * <p>The three notices go from smaller to larger, and they are separate because reacting costs
 * differently: moving the cursor may only need repainting one row, changing a row needs repainting
 * it, and changing the whole set needs drawing everything again.
 */
public interface RowSetListener extends java.util.EventListener {

    /** The cursor moved. */
    void cursorMoved(RowSetEvent event);

    /** The current row changed. */
    void rowChanged(RowSetEvent event);

    /** The whole set changed. */
    void rowSetChanged(RowSetEvent event);
}
