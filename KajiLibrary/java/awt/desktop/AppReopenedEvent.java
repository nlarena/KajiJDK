package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppReopenedEvent -- alguien volvio a lanzar el programa mientras ya corria.
 *
 * <p>Lo entrega {@link AppReopenedListener}. En lugar de arrancar una segunda copia, el escritorio
 * avisa a la que ya esta; lo habitual es responder mostrando la ventana principal.
 */
public final class AppReopenedEvent extends AppEvent {

    private static final long serialVersionUID = 1503238361530407990L;

    /** Sin datos: el evento es el aviso. */
    public AppReopenedEvent() {
    }
}
