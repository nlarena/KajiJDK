package java.awt.print;

import java.awt.Graphics;

/**
 * KajiLibrary's java.awt.print.Printable -- dibuja una pagina.
 *
 * <p>Es toda la interfaz del sistema de impresion viejo: un metodo que recibe un lienzo, el formato de
 * la pagina y el numero de pagina, y dibuja.
 *
 * <h2>Se llama mas de una vez por pagina</h2>
 *
 * <p>Es lo que hay que saber. El sistema puede pedir la misma pagina varias veces --para medir, para
 * rasterizar en bandas, para reintentar-- y tiene que salir lo mismo cada vez. Un {@code Printable}
 * que lleve un contador propio o consuma un iterador imprime mal y el error es dificil de ver.
 *
 * <h2>Como se sabe donde termina</h2>
 *
 * <p>Devolviendo {@link #NO_SUCH_PAGE}. El sistema pide paginas desde la cero hasta que una diga que
 * no existe. Por eso hay que devolver {@code NO_SUCH_PAGE} para <b>todo</b> indice pasado del final, no
 * solo para el primero: puede preguntar salteado.
 *
 * <p>{@link Pageable} es la alternativa cuando se sabe la cantidad de antemano.
 *
 * <h2>El origen no es el de la hoja</h2>
 *
 * <p>El {@code Graphics} llega con el origen en la esquina de la <b>hoja</b>, no del area imprimible.
 * Dibujar en (0,0) casi siempre significa dibujar en el margen mecanico, donde no sale nada. Lo
 * correcto es trasladar a {@code getImageableX()}, {@code getImageableY()} antes de empezar.
 */
public interface Printable {

    /** La pagina existe y se dibujo. */
    int PAGE_EXISTS = 0;

    /** No hay pagina con ese indice. Ver la nota de la clase. */
    int NO_SUCH_PAGE = 1;

    /**
     * Dibuja esa pagina.
     *
     * @param graphics donde dibujar; ver la nota sobre el origen
     * @param pageFormat el formato de esta pagina
     * @param pageIndex cual, empezando en cero
     * @return {@link #PAGE_EXISTS} o {@link #NO_SUCH_PAGE}
     * @throws PrinterException para abortar el trabajo
     */
    int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException;
}
