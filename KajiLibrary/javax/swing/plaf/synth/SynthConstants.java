package javax.swing.plaf.synth;

/**
 * Los estados en que puede estar un componente.
 *
 * <h2>Son banderas, no valores</h2>
 *
 * <p>Se combinan con {@code |}: un boton puede estar a la vez {@link #ENABLED},
 * {@link #MOUSE_OVER} y {@link #FOCUSED}. Por eso son potencias de dos y por eso hay un
 * {@code SELECTED} separado de {@code PRESSED}, que son cosas distintas aunque se vean parecido.
 *
 * <p>El estado completo es lo que recibe {@link SynthContext#getComponentState} y lo que decide que
 * color y que borde le tocan al componente en ese momento.
 *
 * @since 1.5
 */
public interface SynthConstants {

    /** Se puede usar. */
    int ENABLED = 1;

    /** El puntero esta encima. */
    int MOUSE_OVER = 2;

    /** Esta apretado ahora mismo. */
    int PRESSED = 4;

    /** No se puede usar. */
    int DISABLED = 8;

    /** Tiene el foco del teclado. */
    int FOCUSED = 256;

    /** Esta elegido; no es lo mismo que estar apretado. */
    int SELECTED = 512;

    /** Es la opcion por omision, la que responde a la tecla de entrada. */
    int DEFAULT = 1024;
}
