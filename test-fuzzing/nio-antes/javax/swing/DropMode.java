package javax.swing;

/**
 * How where what is being dragged will fall is shown.
 *
 * <p>The difference between {@link #ON} and {@link #INSERT} is the one seen when dragging over
 * a list: {@code ON} marks an element -- it is going to be replaced -- and {@code INSERT} marks
 * the line between two -- it is going to be put in there --. {@link #USE_SELECTION} is the old
 * way, which moved the selection while dragging and therefore lost it if one changed one's
 * mind.
 *
 * <p>The row and column variants are for a table, which has two directions to insert in.
 */
public enum DropMode {

    /**
     * The selection itself marks the destination; it leaves the component changed if it is
     * cancelled.
     */
    USE_SELECTION,

    /** The element one is over is marked. */
    ON,

    /** The gap between two elements is marked. */
    INSERT,

    INSERT_ROWS,

    INSERT_COLS,

    /** The element or the gap is marked, according to where the cursor falls. */
    ON_OR_INSERT,

    ON_OR_INSERT_ROWS,

    ON_OR_INSERT_COLS
}
