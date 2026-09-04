package java.awt.print;

/**
 * KajiLibrary's java.awt.print.Pageable -- un documento del que se sabe cuantas paginas tiene.
 *
 * <p>La alternativa a pasar un {@link Printable} suelto, y sirve para dos cosas que aquel no puede:
 *
 * <ul>
 *   <li>decir cuantas paginas hay de antemano, para que el dialogo pueda ofrecer un rango;
 *   <li>dar un {@link PageFormat} <b>distinto por pagina</b>. Un documento con una tabla apaisada en el
 *       medio no se puede describir de otra forma.
 * </ul>
 *
 * <p>Tambien puede dar un {@code Printable} distinto por pagina, que es lo que permite armar un
 * documento juntando pedazos de origenes distintos. {@link Book} es la implementacion que viene hecha.
 *
 * <p>{@link #getNumberOfPages} puede devolver {@link #UNKNOWN_NUMBER_OF_PAGES}, y ahi se vuelve al
 * comportamiento de {@code Printable}: se piden paginas hasta que una devuelva {@code NO_SUCH_PAGE}.
 */
public interface Pageable {

    /** No se sabe cuantas hay. Ver la nota de la clase. */
    int UNKNOWN_NUMBER_OF_PAGES = -1;

    /** Cuantas paginas, o {@link #UNKNOWN_NUMBER_OF_PAGES}. */
    int getNumberOfPages();

    /**
     * El formato de esa pagina.
     *
     * @throws IndexOutOfBoundsException si no existe
     */
    PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException;

    /**
     * Quien dibuja esa pagina.
     *
     * @throws IndexOutOfBoundsException si no existe
     */
    Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException;
}
