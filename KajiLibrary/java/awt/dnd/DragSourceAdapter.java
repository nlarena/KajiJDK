package java.awt.dnd;

/**
 * A listener on the source's side that does nothing, for redefining only what is of interest.
 *
 * <p>It implements **both** interfaces of the source —the one of state and the one of movement— so
 * a single object can attend the whole drag.
 *
 * <p>Unlike {@link DropTargetAdapter}, here **all** the methods are empty: on the source's side
 * there is none that is compulsory, because the drag works just the same if nobody gives feedback.
 */
public abstract class DragSourceAdapter implements DragSourceListener, DragSourceMotionListener {

    /** For the subclasses. */
    protected DragSourceAdapter() {
    }

    /** It does nothing. */
    public void dragEnter(DragSourceDragEvent dsde) {
    }

    /** It does nothing. */
    public void dragOver(DragSourceDragEvent dsde) {
    }

    /** It does nothing. */
    public void dropActionChanged(DragSourceDragEvent dsde) {
    }

    /** It does nothing. */
    public void dragExit(DragSourceEvent dse) {
    }

    /** It does nothing. */
    public void dragDropEnd(DragSourceDropEvent dsde) {
    }

    /** It does nothing. */
    public void dragMouseMoved(DragSourceDragEvent dsde) {
    }
}
