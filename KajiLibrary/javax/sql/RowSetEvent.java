package javax.sql;

/** KajiLibrary's javax.sql.RowSetEvent -- algo cambio en un {@link RowSet}. */
public class RowSetEvent extends java.util.EventObject {

    public RowSetEvent(RowSet source) {
        super(source);
    }
}
