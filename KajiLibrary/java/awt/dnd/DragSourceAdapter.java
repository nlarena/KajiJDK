package java.awt.dnd;

/**
 * Un oyente del lado del origen que no hace nada, para redefinir sólo lo que interese.
 *
 * <p>Implementa las **dos** interfaces del origen —la de estado y la de movimiento— así que un solo
 * objeto puede atender todo el arrastre.
 *
 * <p>A diferencia de {@link DropTargetAdapter}, acá **todos** los métodos son vacíos: del lado del
 * origen no hay ninguno que sea obligatorio, porque el arrastre funciona igual si nadie da
 * realimentación.
 */
public abstract class DragSourceAdapter implements DragSourceListener, DragSourceMotionListener {

    /** Para las subclases. */
    protected DragSourceAdapter() {
    }

    /** No hace nada. */
    public void dragEnter(DragSourceDragEvent dsde) {
    }

    /** No hace nada. */
    public void dragOver(DragSourceDragEvent dsde) {
    }

    /** No hace nada. */
    public void dropActionChanged(DragSourceDragEvent dsde) {
    }

    /** No hace nada. */
    public void dragExit(DragSourceEvent dse) {
    }

    /** No hace nada. */
    public void dragDropEnd(DragSourceDropEvent dsde) {
    }

    /** No hace nada. */
    public void dragMouseMoved(DragSourceDragEvent dsde) {
    }
}
