package java.awt.dnd;

/**
 * A {@link DropTargetListener} with the four optional methods empty.
 *
 * <p>The asymmetry with the other adapters is deliberate and well thought out: {@link #drop} goes
 * on being **abstract**. A drop target that does nothing on dropping makes no sense, so the class
 * forces it to be written instead of letting it be forgotten.
 */
public abstract class DropTargetAdapter implements DropTargetListener {

    /** For the subclasses. */
    protected DropTargetAdapter() {
    }

    /** It does nothing. */
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    /** It does nothing. */
    public void dragOver(DropTargetDragEvent dtde) {
    }

    /** It does nothing. */
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    /** It does nothing. */
    public void dragExit(DropTargetEvent dte) {
    }
}
