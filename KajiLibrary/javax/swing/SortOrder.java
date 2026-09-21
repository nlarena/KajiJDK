package javax.swing;

/**
 * How a column is sorted.
 *
 * <p>There are three and not two: {@link #UNSORTED} is not "ascending by default" but
 * <strong>the model's order</strong>, that is, the one the data had before anybody sorted it.
 * Without that third constant there would be no way of going back.
 *
 * @since 1.6
 */
public enum SortOrder {

    /** From smallest to largest. */
    ASCENDING,
    /** From largest to smallest. */
    DESCENDING,
    /** Unsorted: the model's order. */
    UNSORTED
}
