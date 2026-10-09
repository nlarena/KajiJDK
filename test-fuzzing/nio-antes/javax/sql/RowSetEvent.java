package javax.sql;

/** KajiLibrary's javax.sql.RowSetEvent -- something changed in a {@link RowSet}. */
public class RowSetEvent extends java.util.EventObject {

    public RowSetEvent(RowSet source) {
        super(source);
    }
}
