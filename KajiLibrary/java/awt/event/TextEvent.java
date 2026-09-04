package java.awt.event;

import java.awt.AWTEvent;

/**
 * Cambió el texto de un componente.
 *
 * <p>Es el evento más escueto de AWT: no dice qué cambió ni cómo, sólo que cambió. Quien lo reciba
 * tiene que ir a leer el texto. Es una decisión de 1.0 que hoy se ve pobre, pero tiene una virtud:
 * no se puede quedar desactualizado respecto del componente.
 */
public class TextEvent extends AWTEvent {

    private static final long serialVersionUID = 6269902291250941179L;

    /** El primer identificador de la familia. */
    public static final int TEXT_FIRST = 900;

    /** El último identificador de la familia. */
    public static final int TEXT_LAST = 900;

    /** Cambió el texto. */
    public static final int TEXT_VALUE_CHANGED = 900;

    /**
     * Con la fuente y el identificador.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public TextEvent(Object source, int id) {
        super(source, id);
    }

    public String paramString() {
        if (this.id == TEXT_VALUE_CHANGED) {
            return "TEXT_VALUE_CHANGED";
        }
        return "unknown type";
    }
}
