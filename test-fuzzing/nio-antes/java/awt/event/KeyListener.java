package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear about the keyboard.
 *
 * <p>The three methods are not the same thing. {@code keyPressed} and {@code keyReleased} talk about
 * **keys** and bring a key code; {@code keyTyped} talks about **characters** and brings the character
 * that resulted. A dead key followed by a vowel is three key events and a single character typed.
 */
public interface KeyListener extends EventListener {

    /** A character was produced. */
    void keyTyped(KeyEvent e);

    /** A key was pressed. */
    void keyPressed(KeyEvent e);

    /** A key was released. */
    void keyReleased(KeyEvent e);
}
