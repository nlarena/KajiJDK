package javax.accessibility;

/**
 * An accessible table that also translates between **linear index** and row and column.
 *
 * <p>The children of an accessible object are numbered consecutively, and a table is walked by
 * coordinates. These three methods are the bridge between the two numberings, and without them it
 * has to be rebuilt by hand in each walk -- wrongly, because merged cells break the naive count.
 */
public interface AccessibleExtendedTable extends AccessibleTable {

    /** Which row corresponds to that linear index. */
    int getAccessibleRow(int index);

    /** Which column corresponds to that linear index. */
    int getAccessibleColumn(int index);

    /** Which linear index corresponds to that position. */
    int getAccessibleIndex(int r, int c);
}
