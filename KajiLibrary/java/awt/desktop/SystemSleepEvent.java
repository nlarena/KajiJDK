package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.SystemSleepEvent -- la maquina se va a suspender o desperto.
 *
 * <p>Lo entrega {@link SystemSleepListener}. A diferencia de {@link ScreenSleepEvent}, aca se detiene
 * todo: hay que cerrar conexiones y guardar lo pendiente, porque al despertar el reloj salto y las
 * conexiones abiertas casi seguro estan muertas.
 *
 * <p>El aviso previo llega con poco margen y el sistema no espera: lo que no se alcance a hacer, no se
 * hace.
 */
public final class SystemSleepEvent extends AppEvent {

    private static final long serialVersionUID = 11372269824930549L;

    /** Sin datos: el evento es el aviso. */
    public SystemSleepEvent() {
    }
}
