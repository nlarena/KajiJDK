package java.awt.dnd;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * The side that **delivers** in a drag.
 *
 * <p>It is the mirror of {@link DropTarget}: that one declares that a component can receive, this
 * one carries the sending through. A single `DragSource` is enough for the whole application —hence
 * {@link #getDefaultDragSource}— because it keeps no state of the drag; that lives in the {@link
 * DragSourceContext} built for each one.
 *
 * <p>The six stock cursors are the ones the user sees while dragging, and they come in pairs: one
 * for when it can be dropped and another for when it cannot. It is the only feedback they have
 * about whether the destination is going to accept it.
 *
 * <p><strong>This implementation cannot start a drag.</strong> A real drag is handled by the
 * operating system: it captures the mouse, draws the cursor over every window and negotiates with
 * other programs. With no window system there is none of that, so the four {@code startDrag}s throw
 * {@link InvalidDnDOperationException} — which is exactly the exception they declare for "the drag
 * system is in no condition to do this", and it is true. Everything else of the class —the cursors,
 * the listeners, the dictionary of formats, the threshold— works.
 */
public class DragSource implements Serializable {

    private static final long serialVersionUID = 6236096958971414066L;

    /** The copy cursor over a destination that accepts. */
    public static final Cursor DefaultCopyDrop = cursor("DnD.Cursor.CopyDrop", Cursor.HAND_CURSOR);

    /** The move cursor over a destination that accepts. */
    public static final Cursor DefaultMoveDrop = cursor("DnD.Cursor.MoveDrop", Cursor.HAND_CURSOR);

    /** The link cursor over a destination that accepts. */
    public static final Cursor DefaultLinkDrop = cursor("DnD.Cursor.LinkDrop", Cursor.HAND_CURSOR);

    /** The copy cursor where nothing can be dropped. */
    public static final Cursor DefaultCopyNoDrop =
            cursor("DnD.Cursor.CopyNoDrop", Cursor.DEFAULT_CURSOR);

    /** The move cursor where nothing can be dropped. */
    public static final Cursor DefaultMoveNoDrop =
            cursor("DnD.Cursor.MoveNoDrop", Cursor.DEFAULT_CURSOR);

    /** The link cursor where nothing can be dropped. */
    public static final Cursor DefaultLinkNoDrop =
            cursor("DnD.Cursor.LinkNoDrop", Cursor.DEFAULT_CURSOR);

    private static DragSource defaultDragSource;

    private transient FlavorMap flavorMap = SystemFlavorMap.getDefaultFlavorMap();
    private transient final List<DragSourceListener> listeners =
            new ArrayList<DragSourceListener>();
    private transient final List<DragSourceMotionListener> motionListeners =
            new ArrayList<DragSourceMotionListener>();

    /**
     * The desktop cursor with that name, or the predefined one if it is not there.
     *
     * <p>It is what the JDK does when the desktop does not define the cursor: it falls back on a
     * predefined one instead of being left with no cursor. Here it is **never** there, because this
     * library brings no cursor descriptors, so the predefined one is always used — and that is
     * honest: it is the cursor that would really be seen.
     */
    private static Cursor cursor(String name, int predefined) {
        try {
            return Cursor.getSystemCustomCursor(name);
        } catch (AWTException e) {
            return Cursor.getPredefinedCursor(predefined);
        }
    }

    /**
     * A new drag source.
     *
     * @throws HeadlessException if there is no screen
     */
    public DragSource() throws HeadlessException {
    }

    /**
     * The source the whole application shares.
     *
     * @throws HeadlessException if there is no screen
     */
    public static DragSource getDefaultDragSource() {
        synchronized (DragSource.class) {
            if (defaultDragSource == null) {
                defaultDragSource = new DragSource();
            }
            return defaultDragSource;
        }
    }

    /**
     * Whether an image that follows the pointer can be shown during the drag.
     *
     * <p>It answers `false`: drawing over every window is done by the system, and there is none.
     */
    public static boolean isDragImageSupported() {
        return false;
    }

    /** The single message of everything the drag system needs. */
    private static InvalidDnDOperationException noDragSystem() {
        return new InvalidDnDOperationException("there is no native drag system: starting a "
                + "drag requires capturing the mouse and drawing over every window, and this "
                + "library brings no window system");
    }

    /**
     * Starts the drag with an image and a dictionary of formats.
     *
     * @throws InvalidDnDOperationException always: there is no drag system to carry it through
     */
    public void startDrag(DragGestureEvent trigger, Cursor dragCursor, Image dragImage,
            Point dragOffset, Transferable transferable, DragSourceListener dsl, FlavorMap fm)
            throws InvalidDnDOperationException {
        throw noDragSystem();
    }

    /**
     * Starts the drag with a dictionary of formats.
     *
     * @throws InvalidDnDOperationException always, for the same reason
     */
    public void startDrag(DragGestureEvent trigger, Cursor dragCursor, Transferable transferable,
            DragSourceListener dsl, FlavorMap fm) throws InvalidDnDOperationException {
        throw noDragSystem();
    }

    /**
     * Starts the drag with an image.
     *
     * @throws InvalidDnDOperationException always, for the same reason
     */
    public void startDrag(DragGestureEvent trigger, Cursor dragCursor, Image dragImage,
            Point imageOffset, Transferable transferable, DragSourceListener dsl)
            throws InvalidDnDOperationException {
        throw noDragSystem();
    }

    /**
     * Starts the drag.
     *
     * @throws InvalidDnDOperationException always, for the same reason
     */
    public void startDrag(DragGestureEvent trigger, Cursor dragCursor, Transferable transferable,
            DragSourceListener dsl) throws InvalidDnDOperationException {
        throw noDragSystem();
    }

    /**
     * Builds the context of a drag; a subclass can give its own.
     *
     * @throws IllegalArgumentException if the trigger or the transferable is `null`
     */
    protected DragSourceContext createDragSourceContext(DragGestureEvent dgl, Cursor dragCursor,
            Image dragImage, Point imageOffset, Transferable t, DragSourceListener dsl) {
        return new DragSourceContext(dgl, dragCursor, dragImage, imageOffset, t, dsl);
    }

    /** The dictionary between Java formats and native names. */
    public FlavorMap getFlavorMap() {
        return this.flavorMap;
    }

    /**
     * Builds a gesture recogniser of the class asked for.
     *
     * @return the recogniser, or `null` if the platform has none of that class
     */
    public <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> recognizerAbstractClass, Component c, int actions, DragGestureListener dgl) {
        return null;
    }

    /**
     * Builds the gesture recogniser that corresponds to this platform.
     *
     * @return `null` always: the concrete recogniser is provided by the window system, and there is
     *     none
     */
    public DragGestureRecognizer createDefaultDragGestureRecognizer(Component c, int actions,
            DragGestureListener dgl) {
        return null;
    }

    /** Adds a listener of the state of the drag; a `null` is ignored. */
    public void addDragSourceListener(DragSourceListener dsl) {
        if (dsl == null) {
            return;
        }
        synchronized (this) {
            this.listeners.add(dsl);
        }
    }

    /** Removes that listener. */
    public void removeDragSourceListener(DragSourceListener dsl) {
        if (dsl == null) {
            return;
        }
        synchronized (this) {
            this.listeners.remove(dsl);
        }
    }

    /** The listeners of the state of the drag. */
    public DragSourceListener[] getDragSourceListeners() {
        synchronized (this) {
            return this.listeners.toArray(new DragSourceListener[this.listeners.size()]);
        }
    }

    /** Adds a listener of the movement; a `null` is ignored. */
    public void addDragSourceMotionListener(DragSourceMotionListener dsml) {
        if (dsml == null) {
            return;
        }
        synchronized (this) {
            this.motionListeners.add(dsml);
        }
    }

    /** Removes that listener. */
    public void removeDragSourceMotionListener(DragSourceMotionListener dsml) {
        if (dsml == null) {
            return;
        }
        synchronized (this) {
            this.motionListeners.remove(dsml);
        }
    }

    /** The listeners of the movement. */
    public DragSourceMotionListener[] getDragSourceMotionListeners() {
        synchronized (this) {
            return this.motionListeners.toArray(
                    new DragSourceMotionListener[this.motionListeners.size()]);
        }
    }

    /**
     * The listeners of that class.
     *
     * @throws ClassCastException if the class is not one of the two drag listener ones
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener[] out;
        if (listenerType == DragSourceListener.class) {
            out = this.getDragSourceListeners();
        } else if (listenerType == DragSourceMotionListener.class) {
            out = this.getDragSourceMotionListeners();
        } else {
            out = new EventListener[0];
        }
        @SuppressWarnings("unchecked")
        T[] typed = (T[]) out;
        return typed;
    }

    /**
     * How many pixels one has to move for it to be a drag and not a click.
     *
     * <p>It comes from the `awt.dnd.drag.threshold` property if it is set, and if not it is worth
     * 5, which is the JDK's default value. A value that is not numeric or not positive is ignored.
     */
    public static int getDragThreshold() {
        String prop = System.getProperty("awt.dnd.drag.threshold");
        if (prop != null) {
            try {
                int v = Integer.parseInt(prop);
                if (v > 0) {
                    return v;
                }
            } catch (NumberFormatException e) {
                // Badly written property: the default value is used.
            }
        }
        return 5;
    }
}
