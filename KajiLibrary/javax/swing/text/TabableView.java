package javax.swing.text;

/**
 * Una vista que sabe medirse teniendo en cuenta las tabulaciones.
 *
 * <p>Sin esto, una vista mide su texto y listo. Con tabulaciones no alcanza: cuanto ocupa un
 * tramo depende de <em>donde empieza</em>, porque una tabulacion salta hasta la proxima parada.
 * De ahi que {@link #getTabbedSpan} reciba la posicion de partida y quien sabe donde caen las
 * paradas.
 */
public interface TabableView {

    /**
     * Cuanto ocupa empezando en {@code x}, expandiendo las tabulaciones con ese expansor.
     */
    float getTabbedSpan(float x, TabExpander e);

    /**
     * Cuanto ocupa ese tramo suyo, sin tabulaciones de por medio.
     *
     * <p>Lo usa quien tiene que partir la vista: para saber donde cortar hay que poder medir
     * pedazos.
     */
    float getPartialSpan(int p0, int p1);
}
