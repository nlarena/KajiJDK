package javax.swing.plaf;

/**
 * El aspecto de un panel con barras de desplazamiento.
 *
 * <p>No agrega nada a {@link ComponentUI}. Lo unico que un aspecto de panel pinta es el borde de
 * la ventana, si hay; todo lo demas son componentes de verdad que se pintan solos.
 */
public abstract class ScrollPaneUI extends ComponentUI {

    protected ScrollPaneUI() {
    }
}
