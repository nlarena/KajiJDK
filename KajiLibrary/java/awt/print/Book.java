package java.awt.print;

import java.util.Vector;

/**
 * KajiLibrary's java.awt.print.Book -- un {@link Pageable} armado a mano.
 *
 * <p>Una lista de paginas, cada una con su {@link Printable} y su {@link PageFormat}. Es la forma de
 * juntar en un documento cosas que se dibujan distinto: una portada vertical, una tabla apaisada, un
 * anexo de otro origen.
 *
 * <h2>{@code append} con cantidad no agrega paginas distintas</h2>
 *
 * <p>{@link #append(Printable, PageFormat, int)} agrega {@code numPages} entradas que <b>comparten</b>
 * el mismo dibujante y el mismo formato. No es un atajo para repetir la misma pagina: el dibujante
 * recibe indices consecutivos y decide que poner en cada uno. Es asi como un {@code Printable} que
 * sabe dibujar N paginas se mete entero en un {@code Book}.
 *
 * <h2>Los indices fuera de rango</h2>
 *
 * <p>{@link Pageable} declara {@link IndexOutOfBoundsException}, y lo que sale es un
 * {@link ArrayIndexOutOfBoundsException}, que es subclase. Viene de que adentro hay un {@code Vector};
 * lo mantenemos porque un programa que dependa del tipo exacto seguiria andando.
 */
public class Book implements Pageable {

    /** Las paginas. */
    private final Vector<BookPage> mPages;

    /** Un libro vacio. */
    public Book() {
        this.mPages = new Vector<BookPage>();
    }

    /** Cuantas paginas. Nunca {@link Pageable#UNKNOWN_NUMBER_OF_PAGES}. */
    public int getNumberOfPages() {
        return this.mPages.size();
    }

    /**
     * El formato de esa pagina.
     *
     * @throws IndexOutOfBoundsException si no existe
     */
    public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
        return getPage(pageIndex).getPageFormat();
    }

    /**
     * Quien dibuja esa pagina.
     *
     * @throws IndexOutOfBoundsException si no existe
     */
    public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
        return getPage(pageIndex).getPrintable();
    }

    /**
     * Reemplaza una pagina que ya existe.
     *
     * @throws IndexOutOfBoundsException si no existe
     * @throws NullPointerException si alguno de los dos es null
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
     * Agrega una pagina al final.
     *
     * @throws NullPointerException si alguno de los dos es null
     */
    public void append(Printable painter, PageFormat page) {
        this.mPages.addElement(new BookPage(painter, page));
    }

    /**
     * Agrega {@code numPages} paginas que comparten dibujante y formato.
     *
     * <p>Ver la nota de la clase: no son copias de la misma pagina.
     *
     * @throws NullPointerException si alguno de los dos es null
     */
    public void append(Printable painter, PageFormat page, int numPages) {
        BookPage bookPage = new BookPage(painter, page);
        int i = 0;
        while (i < numPages) {
            this.mPages.addElement(bookPage);
            i = i + 1;
        }
    }

    /** El acceso con control de rango que comparten los dos accesores. */
    private BookPage getPage(int pageNumber) throws ArrayIndexOutOfBoundsException {
        return this.mPages.elementAt(pageNumber);
    }

    /** Una pagina: quien la dibuja y con que formato. Inmutable. */
    private static final class BookPage {

        /** El formato. */
        private final PageFormat mFormat;

        /** El dibujante. */
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
