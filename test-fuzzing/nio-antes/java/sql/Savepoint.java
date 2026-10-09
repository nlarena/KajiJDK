package java.sql;

/**
 * KajiLibrary's java.sql.Savepoint -- a mark inside a transaction.
 *
 * <p>It serves to undo **a part**: mark, carry on, and if something goes wrong go back to the mark
 * without losing what came before. Without it the only granularity would be the whole transaction,
 * which forces redoing work that was fine.
 *
 * <p>It has two accessors and **only one is valid** for each savepoint: unnamed ones have a number,
 * named ones have a name, and asking for the other throws. The alternative would have been two
 * interfaces, and one with two halves was chosen.
 */
public interface Savepoint {

    /**
     * The number of this savepoint.
     *
     * @throws SQLException if this savepoint has a name
     */
    int getSavepointId() throws SQLException;

    /**
     * The name of this savepoint.
     *
     * @throws SQLException if this savepoint has no name
     */
    String getSavepointName() throws SQLException;
}
