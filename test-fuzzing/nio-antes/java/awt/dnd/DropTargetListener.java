package java.awt.dnd;

import java.util.EventListener;

/**
 * Whoever attends a drag that passes over a component.
 *
 * <p>The five methods are the five things that can happen, and only the last one is the drop. The
 * other four exist so that the destination can **answer while the drag is under way**: change the
 * cursor, highlight where it is going to fall, scroll.
 *
 * <p>The rule that gets forgotten: in {@code dragEnter} and {@code dragOver} {@code acceptDrag} or
 * {@code rejectDrag} has to be called. Without that the user does not see whether they can drop
 * there, and the cursor tells them they cannot even though the component does accept.
 */
public interface DropTargetListener extends EventListener {

    /** The drag entered the component. */
    void dragEnter(DropTargetDragEvent dtde);

    /** The drag is moving over it. */
    void dragOver(DropTargetDragEvent dtde);

    /** The user changed the action, usually by pressing a key. */
    void dropActionChanged(DropTargetDragEvent dtde);

    /** The drag left the component or was cancelled. */
    void dragExit(DropTargetEvent dte);

    /** It was dropped here. */
    void drop(DropTargetDropEvent dtde);
}
