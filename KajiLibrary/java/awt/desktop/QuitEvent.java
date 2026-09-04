package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.QuitEvent -- el sistema pide cerrar el programa.
 *
 * <p>Lo entrega {@link QuitHandler}, junto con un {@link QuitResponse}. Ver ahi por que la respuesta
 * va aparte.
 */
public final class QuitEvent extends AppEvent {

    private static final long serialVersionUID = -256100795532403146L;

    /** Sin datos: el evento es el aviso. */
    public QuitEvent() {
    }
}
