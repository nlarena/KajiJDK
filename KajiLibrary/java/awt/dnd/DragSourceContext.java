package java.awt.dnd;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;
import java.util.TooManyListenersException;

/**
 * The state of **one** drag under way, on the source's side.
 *
 * <p>While {@link DragSource} is unique and remembers nothing, this one is built for each drag and
 * carries everything that lasts: what is being dragged, with which cursor, who wants to hear.
 *
 * <p>It is a {@link DragSourceListener} and a {@link DragSourceMotionListener} itself, and there is
 * its real work: it receives the notices from the system, updates the cursor according to whether
 * the destination accepts or not, and then hands them out to the listener of the drag and to those
 * of the source. That repetition —cursor first, notices afterwards— is what makes the visual
 * feedback coherent even when nobody is listening.
 *
 * <p>{@link #setCursor} with `null` does not turn the cursor off: it **gives control back** to the
 * automatic behaviour. It is the difference between "I want no cursor" and "you choose it", and
 * only the second makes sense during a drag.
 */
public class DragSourceContext
        implements DragSourceListener, DragSourceMotionListener, Serializable {

    private static final long serialVersionUID = -115407898692194719L;

    /** The cursor has not been decided yet. */
    protected static final int DEFAULT = 0;

    /** The drag entered a destination. */
    protected static final int ENTER = 1;

    /** The drag is moving over a destination. */
    protected static final int OVER = 2;

    /** The chosen action changed. */
    protected static final int CHANGED = 3;

    private final DragGestureEvent trigger;
    private final Component component;
    private final Transferable transferable;
    private final DragSourceListener listener;
    private final Image dragImage;
    private final Point offset;
    private final int sourceActions;
    private Cursor cursor;
    private boolean useCustomCursor;

    /**
     * With the gesture that fired it and what is being dragged.
     *
     * @throws IllegalArgumentException if the trigger is `null`, if its component or its drag
     *     source is `null`, if its action is {@code ACTION_NONE}, or if an image is given without
     *     its offset
     * @throws NullPointerException if the transferable is `null`
     */
    public DragSourceContext(DragGestureEvent trigger, Cursor dragCursor, Image dragImage,
            Point offset, Transferable t, DragSourceListener dsl) {
        if (trigger == null) {
            throw new NullPointerException("Trigger");
        }
        if (t == null) {
            throw new NullPointerException("Transferable");
        }
        if (trigger.getComponent() == null) {
            throw new IllegalArgumentException("Source Component");
        }
        if (trigger.getDragSource() == null) {
            throw new IllegalArgumentException("DragSource");
        }
        if (trigger.getDragAction() == DnDConstants.ACTION_NONE) {
            throw new IllegalArgumentException("Drag Action");
        }
        // An image with no offset cannot be placed: there is no knowing which point of it follows
        // the pointer.
        if (dragImage != null && offset == null) {
            throw new IllegalArgumentException("Image Offset");
        }
        this.trigger = trigger;
        this.component = trigger.getComponent();
        this.sourceActions = trigger.getSourceAsDragGestureRecognizer().getSourceActions();
        this.transferable = t;
        this.listener = dsl;
        this.dragImage = dragImage;
        this.offset = offset;
        this.cursor = dragCursor;
        this.useCustomCursor = dragCursor != null;
    }

    /** Who carries the drag through. */
    public DragSource getDragSource() {
        return this.trigger.getDragSource();
    }

    /** Which component it came from. */
    public Component getComponent() {
        return this.component;
    }

    /** The gesture that started it. */
    public DragGestureEvent getTrigger() {
        return this.trigger;
    }

    /** Which actions the source accepts. */
    public int getSourceActions() {
        return this.sourceActions;
    }

    /**
     * Changes the cursor of the drag.
     *
     * <p>With `null` it goes back to the automatic cursor, the one that comes out of whether the
     * destination accepts or not. It is not the same as having no cursor.
     */
    public synchronized void setCursor(Cursor c) {
        this.useCustomCursor = c != null;
        this.cursor = c;
    }

    /** The current cursor. */
    public Cursor getCursor() {
        return this.cursor;
    }

    /**
     * Registers a listener besides the one given when constructing.
     *
     * @throws TooManyListenersException if there is one already
     */
    public synchronized void addDragSourceListener(DragSourceListener dsl)
            throws TooManyListenersException {
        if (dsl == null) {
            return;
        }
        if (this == dsl) {
            throw new IllegalArgumentException("DragSourceContext may not be its own listener");
        }
        if (this.listener != null) {
            throw new TooManyListenersException();
        }
    }

    /** Removes the listener. */
    public synchronized void removeDragSourceListener(DragSourceListener dsl) {
    }

    /** Tells that the formats that can be delivered changed. */
    public void transferablesFlavorsChanged() {
    }

    /**
     * The drag entered a destination: it updates the cursor and hands the notice out.
     *
     * <p>The order matters: the cursor first, the listeners afterwards. If a listener changes the
     * cursor by hand, its change has to be the last one applied.
     */
    public void dragEnter(DragSourceDragEvent dsde) {
        if (this.listener != null) {
            this.listener.dragEnter(dsde);
        }
        DragSourceListener[] others = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < others.length; i++) {
            others[i].dragEnter(dsde);
        }
        this.updateCurrentCursor(dsde.getDropAction(), dsde.getTargetActions(), ENTER);
    }

    /** The drag is moving over a destination. */
    public void dragOver(DragSourceDragEvent dsde) {
        if (this.listener != null) {
            this.listener.dragOver(dsde);
        }
        DragSourceListener[] others = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < others.length; i++) {
            others[i].dragOver(dsde);
        }
        this.updateCurrentCursor(dsde.getDropAction(), dsde.getTargetActions(), OVER);
    }

    /** The drag left the destination: it goes back to the "not here" cursor. */
    public void dragExit(DragSourceEvent dse) {
        if (this.listener != null) {
            this.listener.dragExit(dse);
        }
        DragSourceListener[] others = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < others.length; i++) {
            others[i].dragExit(dse);
        }
        this.updateCurrentCursor(DnDConstants.ACTION_NONE, DnDConstants.ACTION_NONE, DEFAULT);
    }

    /** The chosen action changed. */
    public void dropActionChanged(DragSourceDragEvent dsde) {
        if (this.listener != null) {
            this.listener.dropActionChanged(dsde);
        }
        DragSourceListener[] others = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < others.length; i++) {
            others[i].dropActionChanged(dsde);
        }
        this.updateCurrentCursor(dsde.getDropAction(), dsde.getTargetActions(), CHANGED);
    }

    /** The drag finished. */
    public void dragDropEnd(DragSourceDropEvent dsde) {
        if (this.listener != null) {
            this.listener.dragDropEnd(dsde);
        }
        DragSourceListener[] others = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < others.length; i++) {
            others[i].dragDropEnd(dsde);
        }
    }

    /** The mouse moved during the drag. */
    public void dragMouseMoved(DragSourceDragEvent dsde) {
        DragSourceMotionListener[] others = this.getDragSource().getDragSourceMotionListeners();
        for (int i = 0; i < others.length; i++) {
            others[i].dragMouseMoved(dsde);
        }
    }

    /** What is being dragged. */
    public Transferable getTransferable() {
        return this.transferable;
    }

    /**
     * Chooses the cursor that corresponds to the state of the drag.
     *
     * <p>It does nothing if somebody put a cursor by hand: a cursor of one's own wins over the
     * automatic one, because whoever put it knows something this object does not.
     */
    protected synchronized void updateCurrentCursor(int dropOp, int targetAct, int status) {
        if (this.useCustomCursor) {
            return;
        }
        int action = dropOp & targetAct;
        if (status == DEFAULT || action == DnDConstants.ACTION_NONE) {
            if ((dropOp & DnDConstants.ACTION_LINK) != 0) {
                this.cursor = DragSource.DefaultLinkNoDrop;
            } else if ((dropOp & DnDConstants.ACTION_MOVE) != 0) {
                this.cursor = DragSource.DefaultMoveNoDrop;
            } else {
                this.cursor = DragSource.DefaultCopyNoDrop;
            }
            return;
        }
        if ((action & DnDConstants.ACTION_LINK) != 0) {
            this.cursor = DragSource.DefaultLinkDrop;
        } else if ((action & DnDConstants.ACTION_MOVE) != 0) {
            this.cursor = DragSource.DefaultMoveDrop;
        } else {
            this.cursor = DragSource.DefaultCopyDrop;
        }
    }
}
