package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear what the input method is composing.
 *
 * <p>When typing in Japanese or Chinese, the text goes through an intermediate state before being
 * committed. These events are that state.
 */
public interface InputMethodListener extends EventListener {

    /** The text being composed changed. */
    void inputMethodTextChanged(InputMethodEvent e);

    /** The caret moved inside the text being composed. */
    void caretPositionChanged(InputMethodEvent e);
}
