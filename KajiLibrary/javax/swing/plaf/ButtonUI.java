package javax.swing.plaf;

/**
 * El aspecto de un boton: el tipo por el que {@code AbstractButton} habla con su UI.
 *
 * <p>No agrega nada a {@link ComponentUI}; existe para que cada familia de componentes tenga su
 * tipo de UI y un aspecto no pueda, por error, instalarle a un boton el UI de una etiqueta.
 */
public abstract class ButtonUI extends ComponentUI {

    protected ButtonUI() {
    }
}
