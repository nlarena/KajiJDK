package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AppForegroundEvent -- el programa paso a primer plano o se fue al fondo.
 *
 * <p>Lo entrega {@link AppForegroundListener}, que tiene un metodo para cada direccion; el evento en
 * si no dice cual de las dos fue.
 *
 * <p>Sirve para bajar el ritmo de animaciones o de sondeos cuando nadie esta mirando.
 */
public final class AppForegroundEvent extends AppEvent {

    private static final long serialVersionUID = -5513582555740533911L;

    /** Sin datos: el evento es el aviso. */
    public AppForegroundEvent() {
    }
}
