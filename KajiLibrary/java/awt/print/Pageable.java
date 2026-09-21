package java.awt.print;

/**
 * KajiLibrary's java.awt.print.Pageable -- a document whose number of pages is known.
 *
 * <p>The alternative to passing a lone {@link Printable}, and it serves two things that one cannot:
 *
 * <ul>
 *   <li>saying how many pages there are in advance, so the dialog can offer a range;
 *   <li>giving a <b>different</b> {@link PageFormat} <b>per page</b>. A document with a landscape
 *       table in the middle cannot be described any other way.
 * </ul>
 *
 * <p>It can also give a different {@code Printable} per page, which is what allows building a
 * document by joining pieces from different sources. {@link Book} is the ready-made implementation.
 *
 * <p>{@link #getNumberOfPages} can return {@link #UNKNOWN_NUMBER_OF_PAGES}, and then it falls back
 * to {@code Printable}'s behaviour: pages are requested until one returns {@code NO_SUCH_PAGE}.
 */
public interface Pageable {

    /** It is not known how many there are. See the class note. */
    int UNKNOWN_NUMBER_OF_PAGES = -1;

    /** How many pages, or {@link #UNKNOWN_NUMBER_OF_PAGES}. */
    int getNumberOfPages();

    /**
     * The format of that page.
     *
     * @throws IndexOutOfBoundsException if it does not exist
     */
    PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException;

    /**
     * Who draws that page.
     *
     * @throws IndexOutOfBoundsException if it does not exist
     */
    Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException;
}
