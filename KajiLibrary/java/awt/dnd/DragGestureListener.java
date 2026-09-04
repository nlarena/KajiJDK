package java.awt.dnd;

import java.util.EventListener;

/**
 * Quien se entera de que el usuario **quiso empezar** a arrastrar.
 *
 * <p>Es el disparador de todo el mecanismo. Reconocer el gesto —cuántos píxeles hay que mover con el
 * botón apretado para que sea un arrastre y no un clic torpe— lo hace un
 * {@link DragGestureRecognizer}, que es lo que evita que cada aplicación invente su propio umbral.
 */
public interface DragGestureListener extends EventListener {

    /** El gesto de arrastre se reconoció; acá se decide si arrancar y con qué datos. */
    void dragGestureRecognized(DragGestureEvent dge);
}
