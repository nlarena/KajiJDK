package java.awt.print;

import java.awt.Graphics;

/**
 * KajiLibrary's java.awt.print.Printable -- draws a page.
 *
 * <p>The whole interface of the old printing system is one method: it receives a canvas, the page
 * format and the page number, and draws.
 *
 * <h2>It is called more than once per page</h2>
 *
 * <p>That is what has to be known. The system can ask for the same page several times --to measure,
 * to rasterize in bands, to retry-- and the same thing has to come out each time. A {@code
 * Printable} that keeps its own counter or consumes an iterator prints wrongly, and the error is
 * hard to see.
 *
 * <h2>How the end is known</h2>
 *
 * <p>By returning {@link #NO_SUCH_PAGE}. The system asks for pages from zero until one says it does
 * not exist. That is why {@code NO_SUCH_PAGE} has to be returned for <b>every</b> index past the
 * end, not only for the first one: it may ask out of order.
 *
 * <p>{@link Pageable} is the alternative when the count is known in advance.
 *
 * <h2>The origin is the sheet's, not the imageable area's</h2>
 *
 * <p>The {@code Graphics} arrives with its origin at the corner of the <b>sheet</b>, not of the
 * imageable area. Drawing at (0,0) almost always means drawing in the mechanical margin, where
 * nothing comes out. The right thing is to translate to {@code getImageableX()},
 * {@code getImageableY()} before starting.
 */
public interface Printable {

    /** The page exists and was drawn. */
    int PAGE_EXISTS = 0;

    /** There is no page with that index. See the class note. */
    int NO_SUCH_PAGE = 1;

    /**
     * Draws that page.
     *
     * @param graphics where to draw; see the note on the origin
     * @param pageFormat the format of this page
     * @param pageIndex which one, starting at zero
     * @return {@link #PAGE_EXISTS} or {@link #NO_SUCH_PAGE}
     * @throws PrinterException to abort the job
     */
    int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException;
}
