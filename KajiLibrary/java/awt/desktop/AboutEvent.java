package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AboutEvent -- el usuario pidio ver el cuadro de {@code Acerca de}.
 *
 * <p>Lo entrega {@link AboutHandler}. Registrar uno hace que el sistema use el cuadro propio del
 * programa en lugar del que arma el escritorio.
 */
public final class AboutEvent extends AppEvent {

    private static final long serialVersionUID = -5987180734802756477L;

    /** Sin datos: el evento es el aviso. */
    public AboutEvent() {
    }
}
