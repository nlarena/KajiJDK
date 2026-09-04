package java.awt.event;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;

/**
 * El método de entrada está componiendo texto.
 *
 * <p>Escribir en japonés, chino o coreano no es teclear caracteres: se teclea una pronunciación, el
 * método de entrada ofrece candidatos, y recién al elegir uno el texto queda **confirmado**. Estos
 * eventos son ese proceso.
 *
 * <p>De ahí el número que parte el texto en dos: los primeros caracteres están confirmados y el
 * resto todavía se está componiendo. Un editor tiene que mostrar los dos, y distinguirlos, porque lo
 * que está en composición todavía puede cambiar entero.
 */
public class InputMethodEvent extends AWTEvent {

    private static final long serialVersionUID = 4727190874778922661L;

    /** Se movió el cursor dentro del texto en composición. */
    public static final int CARET_POSITION_CHANGED = 1101;

    /** El primer identificador de la familia. */
    public static final int INPUT_METHOD_FIRST = 1100;

    /** El último identificador de la familia. */
    public static final int INPUT_METHOD_LAST = 1101;

    /** Cambió el texto en composición. */
    public static final int INPUT_METHOD_TEXT_CHANGED = 1100;

    private final AttributedCharacterIterator text;
    private final int committedCharacterCount;
    private final TextHitInfo caret;
    private final TextHitInfo visiblePosition;
    private final long when;

    /**
     * Con todo dado.
     *
     * @throws IllegalArgumentException si la fuente es `null` o si el identificador no es uno de los
     *     dos
     */
    public InputMethodEvent(Component source, int id, long when, AttributedCharacterIterator text,
            int committedCharacterCount, TextHitInfo caret, TextHitInfo visiblePosition) {
        super(source, id);
        if (id < INPUT_METHOD_FIRST || id > INPUT_METHOD_LAST) {
            throw new IllegalArgumentException("id outside of valid range");
        }
        if (id == CARET_POSITION_CHANGED && text != null) {
            throw new IllegalArgumentException("text must be null for CARET_POSITION_CHANGED");
        }
        this.text = text;
        this.committedCharacterCount = committedCharacterCount;
        this.caret = caret;
        this.visiblePosition = visiblePosition;
        this.when = when;
    }

    /**
     * Sin el momento.
     *
     * @throws IllegalArgumentException si la fuente es `null` o el identificador no es válido
     */
    public InputMethodEvent(Component source, int id, AttributedCharacterIterator text,
            int committedCharacterCount, TextHitInfo caret, TextHitInfo visiblePosition) {
        this(source, id, System.currentTimeMillis(), text, committedCharacterCount, caret,
                visiblePosition);
    }

    /**
     * Sólo con las posiciones, para los cambios de cursor.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public InputMethodEvent(Component source, int id, TextHitInfo caret,
            TextHitInfo visiblePosition) {
        this(source, id, System.currentTimeMillis(), null, 0, caret, visiblePosition);
    }

    /** El texto completo, confirmado y en composición. */
    public AttributedCharacterIterator getText() {
        return this.text;
    }

    /** Cuántos caracteres del principio ya están confirmados. */
    public int getCommittedCharacterCount() {
        return this.committedCharacterCount;
    }

    /** Dónde está el cursor dentro del texto en composición. */
    public TextHitInfo getCaret() {
        return this.caret;
    }

    /** Qué parte conviene mantener a la vista si el texto no entra. */
    public TextHitInfo getVisiblePosition() {
        return this.visiblePosition;
    }

    /** Marca que alguien se hizo cargo. */
    public void consume() {
        this.consumed = true;
    }

    /** Si alguien ya se hizo cargo. */
    public boolean isConsumed() {
        return this.consumed;
    }

    /** Cuándo pasó. */
    public long getWhen() {
        return this.when;
    }

    public String paramString() {
        String tipo;
        if (this.id == INPUT_METHOD_TEXT_CHANGED) {
            tipo = "INPUT_METHOD_TEXT_CHANGED";
        } else if (this.id == CARET_POSITION_CHANGED) {
            tipo = "CARET_POSITION_CHANGED";
        } else {
            tipo = "unknown type";
        }
        return tipo + ", committedCharacterCount=" + this.committedCharacterCount + ", caret="
                + this.caret + ", visiblePosition=" + this.visiblePosition;
    }
}
