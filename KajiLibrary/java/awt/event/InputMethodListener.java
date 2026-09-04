package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de lo que está componiendo el método de entrada.

 <p>Al escribir en japonés o en chino, el texto pasa por un estado intermedio antes de confirmarse.
 Estos eventos son ese estado.
 */
public interface InputMethodListener extends EventListener {

    /** Cambió el texto en composición. */
    void inputMethodTextChanged(InputMethodEvent e);

    /** Se movió el cursor dentro del texto en composición. */
    void caretPositionChanged(InputMethodEvent e);
}
