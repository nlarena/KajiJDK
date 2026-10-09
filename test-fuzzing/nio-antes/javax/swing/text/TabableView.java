package javax.swing.text;

/**
 * A view that knows how to measure itself taking the tabs into account.
 *
 * <p>Without this, a view measures its text and that is that. With tabs it is not enough: how
 * much a stretch takes up depends on <em>where it starts</em>, because a tab jumps to the next
 * stop. Hence {@link #getTabbedSpan} takes the starting position and whoever knows where the
 * stops fall.
 */
public interface TabableView {

    /**
     * How much it takes up starting at {@code x}, expanding the tabs with that expander.
     */
    float getTabbedSpan(float x, TabExpander e);

    /**
     * How much that stretch of its own takes up, with no tabs in between.
     *
     * <p>Whoever has to split the view uses it: to know where to break one has to be able to
     * measure pieces.
     */
    float getPartialSpan(int p0, int p1);
}
