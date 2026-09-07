package javax.swing;

import java.awt.Component;
import java.awt.Container;

/**
 * Una ventanita que aparece encima de todo.
 *
 * <h2>Dos formas de aparecer</h2>
 *
 * <p>Un desplegable puede dibujarse adentro de la ventana que lo abrio -- rapido, pero no puede
 * salirse de ella -- o en una ventana propia del sistema, que si puede pero cuesta mas y parpadea.
 * Quien decide es {@link PopupFactory} mirando si lo que hay que mostrar entra.
 *
 * <p>Esta clase esconde esa decision: quien la use llama a {@link #show} y {@link #hide} sin saber
 * cual de las dos le toco.
 *
 * <h2>No se construye a mano</h2>
 *
 * <p>El constructor es protegido. Las hace {@link PopupFactory}, que es la que sabe elegir. Un
 * {@code Popup} armado a mano no tendria como decidir y quedaria siempre en la forma equivocada.
 */
public class Popup {

    private Component owner;
    private Component contents;
    private int x;
    private int y;
    private java.awt.Window ventana;

    /** Una ventanita con ese contenido, en ese punto de la pantalla. */
    protected Popup(Component owner, Component contents, int x, int y) {
        if (contents == null) {
            throw new IllegalArgumentException("Contents must be non-null");
        }
        this.owner = owner;
        this.contents = contents;
        this.x = x;
        this.y = y;
    }

    /** Una ventanita sin nada; la usan las subclases que arman el contenido despues. */
    protected Popup() {
    }

    /**
     * La muestra.
     *
     * <p>Sin pantalla no hay donde mostrarla, asi que aca solo se anota el estado. Lo que falta es
     * el destinatario, no la logica.
     */
    public void show() {
        if (contents != null) {
            contents.setVisible(true);
        }
    }

    public void hide() {
        if (contents != null) {
            contents.setVisible(false);
        }
    }
}
