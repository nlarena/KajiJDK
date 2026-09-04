package java.awt.dnd;

import java.util.EventListener;

/**
 * Quien atiende un arrastre que pasa por encima de un componente.
 *
 * <p>Los cinco métodos son las cinco cosas que pueden pasar, y sólo el último es el soltado. Los
 * otros cuatro existen para que el destino pueda **responder mientras el arrastre está en curso**:
 * cambiar el cursor, resaltar dónde va a caer, desplazarse.
 *
 * <p>La regla que se olvida: en {@code dragEnter} y {@code dragOver} hay que llamar a
 * {@code acceptDrag} o a {@code rejectDrag}. Sin eso el usuario no ve si puede soltar ahí, y el
 * cursor le dice que no aunque el componente sí acepte.
 */
public interface DropTargetListener extends EventListener {

    /** El arrastre entró al componente. */
    void dragEnter(DropTargetDragEvent dtde);

    /** El arrastre se está moviendo por encima. */
    void dragOver(DropTargetDragEvent dtde);

    /** El usuario cambió la acción, normalmente apretando una tecla. */
    void dropActionChanged(DropTargetDragEvent dtde);

    /** El arrastre salió del componente o se canceló. */
    void dragExit(DropTargetEvent dte);

    /** Se soltó acá. */
    void drop(DropTargetDropEvent dtde);
}
