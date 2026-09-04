package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.ScreenSleepEvent -- la pantalla se va a apagar o se encendio.
 *
 * <p>Lo entrega {@link ScreenSleepListener}. Es la pantalla sola: la maquina sigue corriendo, asi que
 * conviene pausar lo que se dibuja y no lo que se calcula. Para eso esta {@link SystemSleepEvent}.
 */
public final class ScreenSleepEvent extends AppEvent {

    private static final long serialVersionUID = 7521606180376544150L;

    /** Sin datos: el evento es el aviso. */
    public ScreenSleepEvent() {
    }
}
