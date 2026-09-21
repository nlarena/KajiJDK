package javax.swing;

import java.awt.AWTKeyStroke;
import java.awt.event.KeyEvent;

/**
 * A key combination, as an object and shared.
 *
 * <p>It is {@link AWTKeyStroke} with another name: it exists because Swing needed it before AWT
 * had it, and it stayed. All the functionality is above; here there are only the factories,
 * which return Swing's type.
 *
 * <p>Two equal combinations are the same object: the factories share them. That is what allows
 * them to be used as a key map's key without writing {@code equals} anywhere.
 */
public class KeyStroke extends AWTKeyStroke {

    /** Only {@link AWTKeyStroke}'s machinery uses it when sharing instances. */
    private KeyStroke() {
    }

    private KeyStroke(char keyChar, int keyCode, int modifiers, boolean onKeyRelease) {
        super(keyChar, keyCode, modifiers, onKeyRelease);
    }

    /** The combination of typing that character. */
    public static KeyStroke getKeyStroke(char keyChar) {
        return getCached(keyChar, KeyEvent.VK_UNDEFINED, 0, false);
    }

    /**
     * Like the previous one; {@code onKeyRelease} ties it to releasing the key and not to pressing
     * it.
     */
    public static KeyStroke getKeyStroke(char keyChar, boolean onKeyRelease) {
        return getCached(keyChar, KeyEvent.VK_UNDEFINED, 0, onKeyRelease);
    }

    /**
     * That character with those modifiers.
     *
     * @deprecated it is {@link #getKeyStroke(char)}; the {@code Character} is so as not to clash
     *     with the {@code int} version.
     */
    @Deprecated
    public static KeyStroke getKeyStroke(Character keyChar, int modifiers) {
        if (keyChar == null) {
            throw new IllegalArgumentException("keyChar cannot be null");
        }
        return getCached(keyChar.charValue(), KeyEvent.VK_UNDEFINED, modifiers, false);
    }

    /** That virtual key with those modifiers. */
    public static KeyStroke getKeyStroke(int keyCode, int modifiers, boolean onKeyRelease) {
        return getCached(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, onKeyRelease);
    }

    public static KeyStroke getKeyStroke(int keyCode, int modifiers) {
        return getCached(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, false);
    }

    /** The combination that represents that keyboard event. */
    public static KeyStroke getKeyStrokeForEvent(KeyEvent anEvent) {
        AWTKeyStroke base = AWTKeyStroke.getAWTKeyStrokeForEvent(anEvent);
        return getCached(base.getKeyChar(), base.getKeyCode(), base.getModifiers(),
                base.isOnKeyRelease());
    }

    /**
     * The combination that string describes, such as {@code "control S"}.
     *
     * <p>It returns {@code null} if the string is not understood, instead of throwing: it is the
     * JDK's way, and it comes from these strings usually coming out of a configuration file.
     */
    public static KeyStroke getKeyStroke(String s) {
        AWTKeyStroke base = AWTKeyStroke.getAWTKeyStroke(s);
        if (base == null) {
            return null;
        }
        return getCached(base.getKeyChar(), base.getKeyCode(), base.getModifiers(),
                base.isOnKeyRelease());
    }

    /** A shared instance with those values. */
    private static KeyStroke getCached(char keyChar, int keyCode, int modifiers,
            boolean onKeyRelease) {
        return new KeyStroke(keyChar, keyCode, modifiers, onKeyRelease);
    }
}
