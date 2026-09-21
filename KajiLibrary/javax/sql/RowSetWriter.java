package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetWriter -- returns a {@link RowSet}'s changes to the database.
 *
 * <p>It returns `boolean` and not `void`, which is what sets it apart from {@link RowSetReader}:
 * `false` means **conflict** --somebody else changed those rows since they were read-- and not an
 * error. It is the only honest answer when two writes overwrite each other.
 */
public interface RowSetWriter {

    /** Writes the changes; `false` if there was a conflict. */
    boolean writeData(RowSetInternal caller) throws java.sql.SQLException;
}
