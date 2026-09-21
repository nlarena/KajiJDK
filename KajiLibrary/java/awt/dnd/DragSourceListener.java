package java.awt.dnd;

import java.util.EventListener;

/**
 * Whoever attends the drag **from the source's side**.
 *
 * <p>It is the mirror of {@link DropTargetListener}: the same moments, seen from whoever let go of
 * the datum instead of whoever receives it. It serves above all for giving feedback —changing the
 * cursor according to whether the destination accepts or not— and for hearing, in
 * {@link #dragDropEnd}, whether the original had to be deleted.
 *
 * <p>{@link #dragDropEnd} is the only one that **always** arrives, whether it was dropped or
 * cancelled, and it is the place where the cleaning up goes.
 */
public interface DragSourceListener extends EventListener {

    /** The drag entered a destination that accepts it. */
    void dragEnter(DragSourceDragEvent dsde);

    /** The drag is moving over a destination. */
    void dragOver(DragSourceDragEvent dsde);

    /** The chosen action changed. */
    void dropActionChanged(DragSourceDragEvent dsde);

    /** The drag left the destination. */
    void dragExit(DragSourceEvent dse);

    /** It finished, with or without a drop. */
    void dragDropEnd(DragSourceDropEvent dsde);
}
