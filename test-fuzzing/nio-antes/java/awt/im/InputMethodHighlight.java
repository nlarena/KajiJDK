package java.awt.im;

import java.awt.font.TextAttribute;
import java.util.Map;

/**
 * How a stretch of text that is still being composed is highlighted.
 *
 * <p>While the input method works, the text goes through states and the user has to be able to
 * tell them apart at a glance. There are two axes:
 *
 * <ul>
 *   <li>the <strong>state</strong>: raw, as it was typed, or already converted to the final script;
 *   <li>the <strong>selection</strong>: whether it is the stretch the user is working on now or one
 *       of the others.
 * </ul>
 *
 * <p>Four combinations, four constants. The **variation** lets a concrete input method add more
 * nuances within a state, and the style lets it say exactly which attributes to draw it with
 * instead of leaving it to the component.
 */
public class InputMethodHighlight {

    /** Text as it was typed, not converted. */
    public static final int RAW_TEXT = 0;

    /** Text already converted to the final script. */
    public static final int CONVERTED_TEXT = 1;

    /** Raw and outside the stretch being worked on. */
    public static final InputMethodHighlight UNSELECTED_RAW_TEXT_HIGHLIGHT =
            new InputMethodHighlight(false, RAW_TEXT);

    /** Raw and inside the stretch being worked on. */
    public static final InputMethodHighlight SELECTED_RAW_TEXT_HIGHLIGHT =
            new InputMethodHighlight(true, RAW_TEXT);

    /** Converted and outside the stretch being worked on. */
    public static final InputMethodHighlight UNSELECTED_CONVERTED_TEXT_HIGHLIGHT =
            new InputMethodHighlight(false, CONVERTED_TEXT);

    /** Converted and inside the stretch being worked on. */
    public static final InputMethodHighlight SELECTED_CONVERTED_TEXT_HIGHLIGHT =
            new InputMethodHighlight(true, CONVERTED_TEXT);

    private final boolean selected;
    private final int state;
    private final int variation;
    private final Map<TextAttribute, ?> style;

    /**
     * With the selection and the state, without variation.
     *
     * @throws IllegalArgumentException if the state is not one of the two
     */
    public InputMethodHighlight(boolean selected, int state) {
        this(selected, state, 0, null);
    }

    /**
     * With a variation of the state.
     *
     * @throws IllegalArgumentException if the state is not one of the two
     */
    public InputMethodHighlight(boolean selected, int state, int variation) {
        this(selected, state, variation, null);
    }

    /**
     * With the drawing style already resolved.
     *
     * @throws IllegalArgumentException if the state is not one of the two
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

    /** Whether it is the stretch being worked on. */
    public boolean isSelected() {
        return this.selected;
    }

    /** Raw or converted. */
    public int getState() {
        return this.state;
    }

    /** Which nuance within the state. */
    public int getVariation() {
        return this.variation;
    }

    /**
     * Which attributes to draw it with.
     *
     * @return the style, or `null` to let the component decide
     */
    public Map<TextAttribute, ?> getStyle() {
        return this.style;
    }
}
