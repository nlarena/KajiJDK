package java.awt.dnd;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * Decides **when** a movement of the mouse stops being a clumsy click and becomes a drag.
 *
 * <p>The question looks silly until it has to be answered: pressing the button and moving two
 * pixels is not dragging, moving twenty is, and the threshold depends on the platform and even on
 * the device. Putting that criterion here, and not in each application, is what makes dragging feel
 * the same everywhere.
 *
 * <p>The class gathers the events of the gesture while it recognises it, and when it is convinced
 * it fires a {@link DragGestureEvent} with **all** of them. That is why it keeps the list instead
 * of only the last one: whoever decides what is dragged usually needs where the gesture started and
 * not where it is now.
 *
 * <p>Like {@link DropTarget}, it admits **a single** listener: two could start two drags with the
 * same gesture.
 */
public abstract class DragGestureRecognizer implements Serializable {

    private static final long serialVersionUID = 8996673345831063337L;

    /** Who is going to carry the drag through. */
    protected DragSource dragSource;

    /** Over which component the gesture is watched for. */
    protected Component component;

    /** Who to tell when the gesture is recognised. */
    protected transient DragGestureListener dragGestureListener;

    /** Which actions the source accepts. */
    protected int sourceActions;

    /**
     * The events gathered from the gesture under way.
     *
     * <p>The declared type is `ArrayList` and not `List`, against what one would write today: it is
     * a **protected** field since 1.2, so its type is part of the API and a subclass can depend on
     * it.
     */
    protected ArrayList<InputEvent> events = new ArrayList<InputEvent>(1);

    /**
     * With everything given.
     *
     * @throws IllegalArgumentException if the drag source is `null`
     */
    protected DragGestureRecognizer(DragSource ds, Component c, int sa, DragGestureListener dgl) {
        if (ds == null) {
            throw new IllegalArgumentException("null DragSource");
        }
        this.dragSource = ds;
        this.component = c;
        this.sourceActions = sa & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
        if (dgl != null) {
            try {
                this.addDragGestureListener(dgl);
            } catch (TooManyListenersException e) {
                // It cannot happen: we have just built it and it has no listeners.
                throw new InternalError(e.toString());
            }
        }
        if (c != null) {
            this.registerListeners();
        }
    }

    /**
     * With no listener.
     *
     * @throws IllegalArgumentException if the drag source is `null`
     */
    protected DragGestureRecognizer(DragSource ds, Component c, int sa) {
        this(ds, c, sa, null);
    }

    /**
     * Accepting any action.
     *
     * @throws IllegalArgumentException if the drag source is `null`
     */
    protected DragGestureRecognizer(DragSource ds, Component c) {
        this(ds, c, DnDConstants.ACTION_NONE);
    }

    /**
     * With no component yet.
     *
     * @throws IllegalArgumentException if the drag source is `null`
     */
    protected DragGestureRecognizer(DragSource ds) {
        this(ds, null);
    }

    /** Starts listening to the component; each concrete recogniser writes it. */
    protected abstract void registerListeners();

    /** Stops listening to it. */
    protected abstract void unregisterListeners();

    /** Who is going to carry the drag through. */
    public DragSource getDragSource() {
        return this.dragSource;
    }

    /** Which component it watches over. */
    public synchronized Component getComponent() {
        return this.component;
    }

    /**
     * Changes the component it watches.
     *
     * <p>It stops listening to the previous one and starts with the new one: it is what keeps the
     * recogniser from staying hooked to a component that is no longer used.
     */
    public synchronized void setComponent(Component c) {
        if (this.component != null && this.dragGestureListener != null) {
            this.unregisterListeners();
        }
        this.component = c;
        if (this.component != null && this.dragGestureListener != null) {
            this.registerListeners();
        }
    }

    /** Which actions the source accepts. */
    public synchronized int getSourceActions() {
        return this.sourceActions;
    }

    /** Changes which actions it accepts. */
    public synchronized void setSourceActions(int actions) {
        this.sourceActions = actions
                & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
    }

    /**
     * The event that started the gesture.
     *
     * @return the first one, or `null` if there is no gesture yet
     */
    public InputEvent getTriggerEvent() {
        if (this.events.isEmpty()) {
            return null;
        }
        return this.events.get(0);
    }

    /** Discards the half-recognised gesture and starts again. */
    public void resetRecognizer() {
        this.events.clear();
    }

    /**
     * Registers the listener.
     *
     * @throws TooManyListenersException if there is one already
     */
    public synchronized void addDragGestureListener(DragGestureListener dgl)
            throws TooManyListenersException {
        if (this.dragGestureListener != null) {
            throw new TooManyListenersException();
        }
        this.dragGestureListener = dgl;
        if (this.component != null) {
            this.registerListeners();
        }
    }

    /**
     * Removes the listener.
     *
     * @throws IllegalArgumentException if it is not the one that was registered
     */
    public synchronized void removeDragGestureListener(DragGestureListener dgl) {
        if (this.dragGestureListener != dgl) {
            throw new IllegalArgumentException();
        }
        this.dragGestureListener = null;
        if (this.component != null) {
            this.unregisterListeners();
        }
    }

    /**
     * Fires the notice that the gesture was recognised.
     *
     * <p>It empties the list of events after giving notice: the gesture has been consumed, and
     * leaving them would make the next drag start with the rubbish of the previous one.
     */
    protected synchronized void fireDragGestureRecognized(int dragAction, Point p) {
        try {
            if (this.dragGestureListener != null) {
                this.dragGestureListener.dragGestureRecognized(
                        new DragGestureEvent(this, dragAction, p, this.events));
            }
        } finally {
            this.events.clear();
        }
    }

    /** Adds an event to the gesture under way. */
    protected synchronized void appendEvent(InputEvent awtie) {
        this.events.add(awtie);
    }
}
