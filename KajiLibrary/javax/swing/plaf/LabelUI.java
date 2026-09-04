package javax.swing.plaf;

/**
 * El aspecto de una etiqueta.
 *
 * <p>No agrega nada a {@link ComponentUI}, y aun asi existe: es el <em>tipo</em> que
 * {@code JLabel.setUI} pide, para que a una etiqueta no se le pueda instalar por error el aspecto
 * de un boton. Cada componente tiene una de estas, vacia, por la misma razon.
 */
public abstract class LabelUI extends ComponentUI {

    /** Para las subclases. */
    protected LabelUI() {
    }
}
