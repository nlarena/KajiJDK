package java.awt.event;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;

/**
 * The input method is composing text.
 *
 * <p>Writing in Japanese, Chinese or Korean is not typing characters: a pronunciation is typed, the
 * input method offers candidates, and only on choosing one does the text become **committed**. These
 * events are that process.
 *
 * <p>Hence the number that splits the text in two: the first characters are committed and the rest
 * is still being composed. An editor has to show both, and tell them apart, because what is being
 * composed may still change entirely.
 */
public class InputMethodEvent extends AWTEvent {

    private static final long serialVersionUID = 4727190874778922661L;

    /** The caret moved inside the text being composed. */
    public static final int CARET_POSITION_CHANGED = 1101;

    /** The family's first identifier. */
    public static final int INPUT_METHOD_FIRST = 1100;

    /** The family's last identifier. */
    public static final int INPUT_METHOD_LAST = 1101;

    /** The text being composed changed. */
    public static final int INPUT_METHOD_TEXT_CHANGED = 1100;

    private final AttributedCharacterIterator text;
    private final int committedCharacterCount;
    private final TextHitInfo caret;
    private final TextHitInfo visiblePosition;
    private final long when;

    /**
     * With everything given.
     *
     * @throws IllegalArgumentException if the source is `null` or if the identifier is neither of
     *     the two
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
     * Without the moment.
     *
     * @throws IllegalArgumentException if the source is `null` or the identifier is not valid
     */
    public InputMethodEvent(Component source, int id, AttributedCharacterIterator text,
            int committedCharacterCount, TextHitInfo caret, TextHitInfo visiblePosition) {
        this(source, id, System.currentTimeMillis(), text, committedCharacterCount, caret,
                visiblePosition);
    }

    /**
     * With the positions alone, for the caret changes.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public InputMethodEvent(Component source, int id, TextHitInfo caret,
            TextHitInfo visiblePosition) {
        this(source, id, System.currentTimeMillis(), null, 0, caret, visiblePosition);
    }

    /** The whole text, committed and being composed. */
    public AttributedCharacterIterator getText() {
        return this.text;
    }

    /** How many characters from the start are already committed. */
    public int getCommittedCharacterCount() {
        return this.committedCharacterCount;
    }

    /** Where the caret is inside the text being composed. */
    public TextHitInfo getCaret() {
        return this.caret;
    }

    /** Which part is worth keeping in view if the text does not fit. */
    public TextHitInfo getVisiblePosition() {
        return this.visiblePosition;
    }

    /** Marks that someone took charge. */
    public void consume() {
        this.consumed = true;
    }

    /** Whether someone already took charge. */
    public boolean isConsumed() {
        return this.consumed;
    }

    /** When it happened. */
    public long getWhen() {
        return this.when;
    }

    public String paramString() {
        String type;
        if (this.id == INPUT_METHOD_TEXT_CHANGED) {
            type = "INPUT_METHOD_TEXT_CHANGED";
        } else if (this.id == CARET_POSITION_CHANGED) {
            type = "CARET_POSITION_CHANGED";
        } else {
            type = "unknown type";
        }
        return type + ", committedCharacterCount=" + this.committedCharacterCount + ", caret="
                + this.caret + ", visiblePosition=" + this.visiblePosition;
    }
}
