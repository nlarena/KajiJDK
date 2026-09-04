package java.awt.im;

import java.awt.font.TextAttribute;
import java.util.Map;

/**
 * Cómo se resalta un tramo de texto que todavía está en composición.
 *
 * <p>Mientras el método de entrada trabaja, el texto pasa por estados y el usuario tiene que poder
 * distinguirlos de un vistazo. Hay dos ejes:
 *
 * <ul>
 *   <li>el <strong>estado</strong>: crudo, tal como se tecleó, o ya convertido a la escritura
 *       final;
 *   <li>la <strong>selección</strong>: si es el tramo sobre el que el usuario está trabajando ahora
 *       o uno de los otros.
 * </ul>
 *
 * <p>Cuatro combinaciones, cuatro constantes. La **variación** deja que un método de entrada
 * concreto agregue más matices dentro de un estado, y el estilo permite decir exactamente con qué
 * atributos dibujarlo en vez de dejarlo librado al componente.
 */
public class InputMethodHighlight {

    /** Texto tal como se tecleó, sin convertir. */
    public static final int RAW_TEXT = 0;

    /** Texto ya convertido a la escritura final. */
    public static final int CONVERTED_TEXT = 1;

    /** Crudo y fuera del tramo en el que se está trabajando. */
    public static final InputMethodHighlight UNSELECTED_RAW_TEXT_HIGHLIGHT =
            new InputMethodHighlight(false, RAW_TEXT);

    /** Crudo y dentro del tramo en el que se está trabajando. */
    public static final InputMethodHighlight SELECTED_RAW_TEXT_HIGHLIGHT =
            new InputMethodHighlight(true, RAW_TEXT);

    /** Convertido y fuera del tramo en el que se está trabajando. */
    public static final InputMethodHighlight UNSELECTED_CONVERTED_TEXT_HIGHLIGHT =
            new InputMethodHighlight(false, CONVERTED_TEXT);

    /** Convertido y dentro del tramo en el que se está trabajando. */
    public static final InputMethodHighlight SELECTED_CONVERTED_TEXT_HIGHLIGHT =
            new InputMethodHighlight(true, CONVERTED_TEXT);

    private final boolean selected;
    private final int state;
    private final int variation;
    private final Map<TextAttribute, ?> style;

    /**
     * Con la selección y el estado, sin variación.
     *
     * @throws IllegalArgumentException si el estado no es uno de los dos
     */
    public InputMethodHighlight(boolean selected, int state) {
        this(selected, state, 0, null);
    }

    /**
     * Con una variación del estado.
     *
     * @throws IllegalArgumentException si el estado no es uno de los dos
     */
    public InputMethodHighlight(boolean selected, int state, int variation) {
        this(selected, state, variation, null);
    }

    /**
     * Con el estilo de dibujo ya resuelto.
     *
     * @throws IllegalArgumentException si el estado no es uno de los dos
     */
    public InputMethodHighlight(boolean selected, int state, int variation,
            Map<TextAttribute, ?> style) {
        if (state != RAW_TEXT && state != CONVERTED_TEXT) {
            throw new IllegalArgumentException("unknown input method highlight state");
        }
        this.selected = selected;
        this.state = state;
        this.variation = variation;
        this.style = style;
    }

    /** Si es el tramo sobre el que se está trabajando. */
    public boolean isSelected() {
        return this.selected;
    }

    /** Crudo o convertido. */
    public int getState() {
        return this.state;
    }

    /** Qué matiz dentro del estado. */
    public int getVariation() {
        return this.variation;
    }

    /**
     * Con qué atributos dibujarlo.
     *
     * @return el estilo, o `null` para que lo decida el componente
     */
    public Map<TextAttribute, ?> getStyle() {
        return this.style;
    }
}
