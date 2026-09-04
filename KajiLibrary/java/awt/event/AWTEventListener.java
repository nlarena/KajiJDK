package java.awt.event;

import java.awt.AWTEvent;
import java.util.EventListener;

/**
 * Quien quiere ver **todos** los eventos de ciertas familias, antes de que lleguen a su destino.

 <p>Es la puerta de atrás del despacho de eventos: se registra en el {@code Toolkit} y no en un
 componente, y recibe copia de todo lo que pase. Sirve para depurar y para accesibilidad; usarlo
 para lógica de la aplicación es una forma segura de acoplar todo con todo.
 */
public interface AWTEventListener extends EventListener {

    /** Pasó un evento de una familia observada. */
    void eventDispatched(AWTEvent e);
}
