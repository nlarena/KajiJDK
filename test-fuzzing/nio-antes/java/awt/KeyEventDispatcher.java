package java.awt;

import java.awt.event.KeyEvent;

/**
 * Something that steps in to dispatch key events **before** the focus manager.
 *
 * <p>It is registered with {@link KeyboardFocusManager#addKeyEventDispatcher}. Returning `true`
 * means "I take it": the event travels no further and nobody else sees it, not even the component
 * with the focus. It is the way to implement a global shortcut.
 */
public interface KeyEventDispatcher {

    /**
     * Dispatches that key event.
     *
     * @return `true` if it consumed it and nobody else has to see it
     */
    boolean dispatchKeyEvent(KeyEvent e);
}
