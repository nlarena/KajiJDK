package java.awt.dnd;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

/**
 * The user made the gesture of starting to drag.
 *
 * <p>It brings **all** the events of the gesture, not only the last one: the press of the button
 * and the movements that were needed to recognise it. It serves for deciding what is being dragged
 * from where the gesture started, which is not where the pointer is now.
 *
 * <p>The {@code startDrag}s here are the comfortable shortcut: they pass everything on to the
 * {@link DragSource} with this event as the trigger, so as not to have to repeat the data the event
 * already has.
 */
public class DragGestureEvent extends EventObject {

    private static final long serialVersionUID = 9080172649166731306L;

    private final transient List<InputEvent> events;
    private final DragSource dragSource;
    private final Component component;
    private final Point origin;
    private final int action;

    /**
     * With the recogniser, the action, the source and the events of the gesture.
     *
     * @throws IllegalArgumentException if the recogniser, its component or its drag source is
     *     missing, if the point or the list is `null`, or if the action is not one of those of
     *     {@link DnDConstants}
     */
    public DragGestureEvent(DragGestureRecognizer dgr, int act, Point ori,
            List<? extends InputEvent> evs) {
        super(dgr);
        if (evs == null || evs.isEmpty()) {
            throw new IllegalArgumentException("null or empty list of events");
        }
        if (act != DnDConstants.ACTION_COPY && act != DnDConstants.ACTION_MOVE
                && act != DnDConstants.ACTION_LINK) {
            throw new IllegalArgumentException("bad action");
        }
        if (ori == null) {
            throw new IllegalArgumentException("null origin");
        }
        this.component = dgr.getComponent();
        this.dragSource = dgr.getDragSource();
        if (this.component == null) {
            throw new IllegalArgumentException("null component");
        }
        if (this.dragSource == null) {
            throw new IllegalArgumentException("null DragSource");
        }
        this.events = new ArrayList<InputEvent>(evs);
        this.action = act;
        this.origin = ori;
    }

    /** The recogniser that fired it. */
    public DragGestureRecognizer getSourceAsDragGestureRecognizer() {
        return (DragGestureRecognizer) this.getSource();
    }

    /** Over which component the gesture was made. */
    public Component getComponent() {
        return this.component;
    }

    /** Who is going to carry the drag through. */
    public DragSource getDragSource() {
        return this.dragSource;
    }

    /** Where the gesture started, relative to the component. */
    public Point getDragOrigin() {
        return this.origin;
    }

    /** All the events that made up the gesture, in order. */
    public Iterator<InputEvent> iterator() {
        return this.events.iterator();
    }

    /** The events of the gesture, as an array. */
    public Object[] toArray() {
        return this.events.toArray();
    }

    /**
     * The events of the gesture, in the given array.
     *
     * @throws ArrayStoreException if the type of the array does not accept input events
     */
    public Object[] toArray(Object[] array) {
        return this.events.toArray(array);
    }

    /** Which action the user asks for. */
    public int getDragAction() {
        return this.action;
    }

    /** The first event of the gesture: the one that started it. */
    public InputEvent getTriggerEvent() {
        return this.getSourceAsDragGestureRecognizer().getTriggerEvent();
    }

    /**
     * Starts the drag.
     *
     * @throws InvalidDnDOperationException if the drag cannot be started
     */
    public void startDrag(Cursor dragCursor, Transferable transferable)
            throws InvalidDnDOperationException {
        this.dragSource.startDrag(this, dragCursor, transferable, null);
    }

    /**
     * Starts the drag with a listener that follows how it goes.
     *
     * @throws InvalidDnDOperationException if the drag cannot be started
     */
    public void startDrag(Cursor dragCursor, Transferable transferable, DragSourceListener dsl)
            throws InvalidDnDOperationException {
        this.dragSource.startDrag(this, dragCursor, transferable, dsl);
    }

    /**
     * Starts the drag with an image that follows the pointer.
     *
     * @throws InvalidDnDOperationException if the drag cannot be started
     */
    public void startDrag(Cursor dragCursor, Image dragImage, Point imageOffset,
            Transferable transferable, DragSourceListener dsl)
            throws InvalidDnDOperationException {
        this.dragSource.startDrag(this, dragCursor, dragImage, imageOffset, transferable, dsl);
    }
}
