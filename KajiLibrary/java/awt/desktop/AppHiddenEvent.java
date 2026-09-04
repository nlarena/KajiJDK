package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppHiddenEvent -- el programa se oculto o se volvio a mostrar.
 *
 * <p>Lo entrega {@link AppHiddenListener}. Ocultarse no es lo mismo que irse al fondo: un programa
 * oculto no tiene ninguna ventana visible, uno en el fondo si.
 */
public final class AppHiddenEvent extends AppEvent {

    private static final long serialVersionUID = 2637465279476429224L;

    /** Sin datos: el evento es el aviso. */
    public AppHiddenEvent() {
    }
}
