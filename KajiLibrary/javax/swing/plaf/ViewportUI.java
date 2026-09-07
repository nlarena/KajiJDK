package javax.swing.plaf;

/**
 * El aspecto de una ventana de desplazamiento.
 *
 * <p>No agrega nada a {@link ComponentUI}: una ventana no dibuja mas que su fondo. Existe por lo
 * mismo que {@link ButtonUI}, para que cada familia tenga su tipo.
 */
public abstract class ViewportUI extends ComponentUI {

    protected ViewportUI() {
    }
}
