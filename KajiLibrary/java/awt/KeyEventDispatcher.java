package java.awt;

import java.awt.event.KeyEvent;

/**
 * Alguien que se mete a repartir los eventos de teclado **antes** que el gestor del foco.
 *
 * <p>Se registra con {@link KeyboardFocusManager#addKeyEventDispatcher}. Devolver `true` quiere decir
 * "yo me lo llevo": el evento no sigue viajando y ningún otro lo ve, ni siquiera el componente que
 * tiene el foco. Es la forma de implementar un atajo global.
 */
public interface KeyEventDispatcher {

    /**
     * Reparte ese evento de teclado.
     *
     * @return `true` si lo consumió y nadie más tiene que verlo
     */
    boolean dispatchKeyEvent(KeyEvent e);
}
