package java.awt;

import java.awt.event.KeyEvent;

/**
 * Alguien que mira los eventos de teclado **después** de que nadie los haya consumido.
 *
 * <p>Es el otro extremo de {@link KeyEventDispatcher}: el repartidor va antes que todos y el
 * posprocesador va después. Sirve para el atajo que sólo tiene que actuar si el componente con el
 * foco no hizo nada con la tecla —una tecla de menú, por ejemplo—.
 */
public interface KeyEventPostProcessor {

    /**
     * Mira ese evento ya repartido.
     *
     * @return `true` si lo consumió y ningún otro posprocesador tiene que verlo
     */
    boolean postProcessKeyEvent(KeyEvent e);
}
