package java.awt.dnd;

import java.util.EventListener;

/**
 * Whoever hears that the user **wanted to start** dragging.
 *
 * <p>It is the trigger of the whole mechanism. Recognising the gesture —how many pixels have to be
 * moved with the button held down for it to be a drag and not a clumsy click— is done by a
 * {@link DragGestureRecognizer}, which is what keeps each application from inventing its own
 * threshold.
 */
public interface DragGestureListener extends EventListener {

    /** The drag gesture was recognised; here it is decided whether to start and with what data. */
    void dragGestureRecognized(DragGestureEvent dge);
}
