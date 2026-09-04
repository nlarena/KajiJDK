package java.awt.dnd;

import java.util.EventListener;

/**
 * Quien quiere seguir el movimiento del ratón durante todo el arrastre.
 *
 * <p>Está separado de {@link DragSourceListener} por la misma razón que el movimiento del ratón está
 * separado de sus botones: son muchísimos más eventos, y quien sólo quiere saber dónde se soltó no
 * debería pagarlos.
 */
public interface DragSourceMotionListener extends EventListener {

    /** El ratón se movió mientras se arrastra. */
    void dragMouseMoved(DragSourceDragEvent dsde);
}
