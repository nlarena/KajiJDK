package java.awt.event;

import java.awt.AWTEvent;

/**
 * A component's text changed.
 *
 * <p>It is AWT's tersest event: it does not say what changed nor how, only that it changed. Whoever
 * receives it has to go and read the text. It is a decision from 1.0 that looks poor today, but it
 * has one virtue: it cannot fall out of date with respect to the component.
 */
public class TextEvent extends AWTEvent {

    private static final long serialVersionUID = 6269902291250941179L;

    /** The family's first identifier. */
    public static final int TEXT_FIRST = 900;

    /** The family's last identifier. */
    public static final int TEXT_LAST = 900;

    /** The text changed. */
    public static final int TEXT_VALUE_CHANGED = 900;

    /**
     * With the source and the identifier.
     *
     * @throws IllegalArgumentException if the source is `null`
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
