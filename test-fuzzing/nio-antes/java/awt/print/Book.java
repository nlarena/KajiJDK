package java.awt.print;

import java.util.Vector;

/**
 * KajiLibrary's java.awt.print.Book -- a {@link Pageable} put together by hand.
 *
 * <p>A list of pages, each with its {@link Printable} and its {@link PageFormat}. It is the way to
 * put together in one document things drawn differently: a portrait cover, a landscape table, an
 * appendix from another source.
 *
 * <h2>{@code append} with a count does not add different pages</h2>
 *
 * <p>{@link #append(Printable, PageFormat, int)} adds {@code numPages} entries that <b>share</b>
 * the same painter and the same format. It is not a shortcut for repeating the same page: the
 * painter receives consecutive indices and decides what to put in each. That is how a {@code
 * Printable} that knows how to draw N pages fits whole into a {@code Book}.
 *
 * <h2>Out-of-range indices</h2>
 *
 * <p>{@link Pageable} declares {@link IndexOutOfBoundsException}, and what comes out is an
 * {@link ArrayIndexOutOfBoundsException}, which is a subclass. It comes from the {@code Vector}
 * inside; we keep it because a program that depends on the exact type keeps working.
 */
public class Book implements Pageable {

    /** The pages. */
    private final Vector<BookPage> mPages;

    /** An empty book. */
    public Book() {
        this.mPages = new Vector<BookPage>();
    }

    /** How many pages. Never {@link Pageable#UNKNOWN_NUMBER_OF_PAGES}. */
    public int getNumberOfPages() {
        return this.mPages.size();
    }

    /**
     * The format of that page.
     *
     * @throws IndexOutOfBoundsException if it does not exist
     */
    public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
        return getPage(pageIndex).getPageFormat();
    }

    /**
     * Who draws that page.
     *
     * @throws IndexOutOfBoundsException if it does not exist
     */
    public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
        return getPage(pageIndex).getPrintable();
    }

    /**
     * Replaces a page that already exists.
     *
     * @throws IndexOutOfBoundsException if it does not exist
     * @throws NullPointerException if either of the two is null
     */
    public void setPage(int pageIndex, Printable painter, PageFormat page)
        throws IndexOutOfBoundsException {
        if (painter == null) {
            throw new NullPointerException("painter is null");
        }
        if (page == null) {
            throw new NullPointerException("page is null");
        }
        this.mPages.setElementAt(new BookPage(painter, page), pageIndex);
    }

    /**
     * Adds a page at the end.
     *
     * @throws NullPointerException if either of the two is null
     */
    public void append(Printable painter, PageFormat page) {
        this.mPages.addElement(new BookPage(painter, page));
    }

    /**
     * Adds {@code numPages} pages that share painter and format.
     *
     * <p>See the class note: they are not copies of the same page.
     *
     * @throws NullPointerException if either of the two is null
     */
    public void append(Printable painter, PageFormat page, int numPages) {
        BookPage bookPage = new BookPage(painter, page);
        int i = 0;
        while (i < numPages) {
            this.mPages.addElement(bookPage);
            i = i + 1;
        }
    }

    /** The range-checked access both accessors share. */
    private BookPage getPage(int pageNumber) throws ArrayIndexOutOfBoundsException {
        return this.mPages.elementAt(pageNumber);
    }

    /** A page: who draws it and with which format. Immutable. */
    private static final class BookPage {

        /** The format. */
        private final PageFormat mFormat;

        /** The painter. */
        private final Printable mPainter;

        BookPage(Printable painter, PageFormat format) {
            if (painter == null || format == null) {
                throw new NullPointerException();
            }
            this.mFormat = format;
            this.mPainter = painter;
        }

        PageFormat getPageFormat() {
            return this.mFormat;
        }

        Printable getPrintable() {
            return this.mPainter;
        }
    }
}
