package javax.swing;

import java.awt.AWTKeyStroke;
import java.awt.event.KeyEvent;

/**
 * Una combinacion de teclas, como objeto y compartida.
 *
 * <p>Es {@link AWTKeyStroke} con otro nombre: existe porque Swing la necesitaba antes de que AWT
 * la tuviera, y quedo. Toda la funcionalidad esta arriba; aca solo estan las fabricas, que
 * devuelven el tipo de Swing.
 *
 * <p>Dos combinaciones iguales son el mismo objeto: las fabricas las comparten. Eso es lo que
 * permite usarlas como clave de un mapa de teclas sin escribir {@code equals} en ningun lado.
 */
public class KeyStroke extends AWTKeyStroke {

    /** Solo lo usa la maquinaria de {@link AWTKeyStroke} al compartir instancias. */
    private KeyStroke() {
    }

    private KeyStroke(char keyChar, int keyCode, int modifiers, boolean onKeyRelease) {
        super(keyChar, keyCode, modifiers, onKeyRelease);
    }

    /** La combinacion de escribir ese caracter. */
    public static KeyStroke getKeyStroke(char keyChar) {
        return getCached(keyChar, KeyEvent.VK_UNDEFINED, 0, false);
    }

    /** Como la anterior; {@code onKeyRelease} la ata a soltar la tecla y no a apretarla. */
    public static KeyStroke getKeyStroke(char keyChar, boolean onKeyRelease) {
        return getCached(keyChar, KeyEvent.VK_UNDEFINED, 0, onKeyRelease);
    }

    /**
     * Ese caracter con esos modificadores.
     *
     * @deprecated es {@link #getKeyStroke(char)}; el {@code Character} es para no chocar con la
     *     version de {@code int}.
     */
    @Deprecated
    public static KeyStroke getKeyStroke(Character keyChar, int modifiers) {
        if (keyChar == null) {
            throw new IllegalArgumentException("keyChar cannot be null");
        }
        return getCached(keyChar.charValue(), KeyEvent.VK_UNDEFINED, modifiers, false);
    }

    /** Esa tecla virtual con esos modificadores. */
    public static KeyStroke getKeyStroke(int keyCode, int modifiers, boolean onKeyRelease) {
        return getCached(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, onKeyRelease);
    }

    public static KeyStroke getKeyStroke(int keyCode, int modifiers) {
        return getCached(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, false);
    }

    /** La combinacion que representa ese evento de teclado. */
    public static KeyStroke getKeyStrokeForEvent(KeyEvent anEvent) {
        AWTKeyStroke base = AWTKeyStroke.getAWTKeyStrokeForEvent(anEvent);
        return getCached(base.getKeyChar(), base.getKeyCode(), base.getModifiers(),
                base.isOnKeyRelease());
    }

    /**
     * La combinacion que describe esa cadena, como {@code "control S"}.
     *
     * <p>Devuelve {@code null} si la cadena no se entiende, en vez de lanzar: es la forma que
     * tiene el JDK, y viene de que estas cadenas suelen salir de un archivo de configuracion.
     */
    public static KeyStroke getKeyStroke(String s) {
        AWTKeyStroke base = AWTKeyStroke.getAWTKeyStroke(s);
        if (base == null) {
            return null;
        }
        return getCached(base.getKeyChar(), base.getKeyCode(), base.getModifiers(),
                base.isOnKeyRelease());
    }

    /** Una instancia compartida con esos valores. */
    private static KeyStroke getCached(char keyChar, int keyCode, int modifiers,
            boolean onKeyRelease) {
        return new KeyStroke(keyChar, keyCode, modifiers, onKeyRelease);
    }
}
