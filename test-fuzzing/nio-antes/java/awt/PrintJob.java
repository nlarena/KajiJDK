package java.awt;

/**
 * A print job in progress.
 *
 * <p>The model is from 1.1 and it shows: a page is asked for with {@link #getGraphics}, drawn on
 * like any other surface, and released with {@code dispose}. The next page is another {@code
 * getGraphics}. At the end, {@link #end}.
 *
 * <p>{@link #lastPageFirst} exists because of a quirk of the printers of the time: many stacked the
 * sheets face up, so printing in order left the document backwards. Asking allowed drawing the
 * pages in whatever order was needed.
 *
 * <p>The modern API is {@code java.awt.print}; this one is kept because {@link Toolkit} returns it.
 */
public abstract class PrintJob {

    /** For subclasses. */
    protected PrintJob() {
    }

    /**
     * A new page to draw on.
     *
     * @return the page's context, or `null` if there are no more pages
     */
    public abstract Graphics getGraphics();

    /** How big a page is, in print pixels. */
    public abstract Dimension getPageDimension();

    /** How many dots per inch the page has. */
    public abstract int getPageResolution();

    /** Whether it is better to draw the last page first. */
    public abstract boolean lastPageFirst();

    /** Ends the job and sends it to print. */
    public abstract void end();

    /**
     * Ends the job if nobody ended it.
     *
     * @deprecated it depends on garbage collection, which guarantees neither when it runs nor that
     *     it runs. {@link #end} has to be called by hand.
     */
    @Deprecated
    public void finalize() {
        this.end();
    }
}
