package javax.swing.plaf;

/**
 * El aspecto de una barra de desplazamiento.
 *
 * <p>No agrega nada a {@link ComponentUI}: la barra es un modelo mas dos botones, y todo lo que
 * hay que saber de ella se pregunta por su API. Existe por lo mismo que {@link ButtonUI}.
 */
public abstract class ScrollBarUI extends ComponentUI {

    protected ScrollBarUI() {
    }
}
