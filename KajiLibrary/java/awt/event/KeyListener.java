package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse del teclado.

 <p>Los tres métodos no son lo mismo. {@code keyPressed} y {@code keyReleased} hablan de **teclas**
 y traen un código de tecla; {@code keyTyped} habla de **caracteres** y trae el carácter que resultó.
 Una tecla muerta seguida de una vocal son tres pulsaciones y un solo carácter tecleado.
 */
public interface KeyListener extends EventListener {

    /** Se produjo un carácter. */
    void keyTyped(KeyEvent e);

    /** Se apretó una tecla. */
    void keyPressed(KeyEvent e);

    /** Se soltó una tecla. */
    void keyReleased(KeyEvent e);
}
