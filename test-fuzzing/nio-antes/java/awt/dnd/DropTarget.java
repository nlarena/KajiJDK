package java.awt.dnd;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.io.Serializable;
import java.util.TooManyListenersException;

/**
 * Declares that a component **can receive** dragged things.
 *
 * <p>It is hooked to a component with {@code setDropTarget} and with that the component becomes a
 * valid destination. Everything else —which formats it accepts, what it does on dropping— is
 * decided by the {@link DropTargetListener} registered with it.
 *
 * <p>It is a {@link DropTargetListener} itself, and that allows two ways of using it: registering a
 * listener with it, or inheriting from it and redefining the five methods. The first is the usual
 * one; the second exists because sometimes the destination and its logic are the same thing.
 *
 * <p><strong>It admits a single listener</strong>, and that is why {@link #addDropTargetListener}
 * throws {@code TooManyListenersException}. It is not an arbitrary limitation: two listeners could
 * answer different things to the same drag —one accept and the other reject— and there is no way of
 * resolving that tie.
 *
 * <p>The automatic scrolling turns itself on only if the component implements {@link Autoscroll}.
 * The sum of whether the pointer entered the sensitive zone is here and not in the component, so
 * that each one does not have to redo it.
 */
public class DropTarget implements DropTargetListener, Serializable {

    private static final long serialVersionUID = -6283860791671019047L;

    private Component component;

    /** Which actions this destination accepts. */
    int actions = DnDConstants.ACTION_COPY_OR_MOVE;

    /** Whether the destination is listening. */
    boolean active = true;

    private transient DropTargetContext dropTargetContext;
    private transient DropTargetListener dtListener;
    private transient FlavorMap flavorMap;
    private transient DropTargetAutoScroller autoScroller;

    /**
     * With everything given.
     *
     * @param dt the component, or `null` to hook it later
     * @param ops the actions it accepts
     * @param dtl the listener, or `null`
     * @param act whether it starts active
     * @param fm the map of formats, or `null` for the system's
     * @throws HeadlessException if there is no screen
     */
    public DropTarget(Component dt, int ops, DropTargetListener dtl, boolean act, FlavorMap fm)
            throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        this.component = dt;
        this.setDefaultActions(ops);
        this.dtListener = dtl;
        this.active = act;
        if (fm != null) {
            this.flavorMap = fm;
        } else {
            this.flavorMap = SystemFlavorMap.getDefaultFlavorMap();
        }
        if (dt != null) {
            dt.setDropTarget(this);
        }
    }

    /**
     * With the system's map of formats.
     *
     * @throws HeadlessException if there is no screen
     */
    public DropTarget(Component dt, int ops, DropTargetListener dtl, boolean act)
            throws HeadlessException {
        this(dt, ops, dtl, act, null);
    }

    /**
     * With neither component nor listener; they have to be hooked later.
     *
     * @throws HeadlessException if there is no screen
     */
    public DropTarget() throws HeadlessException {
        this(null, DnDConstants.ACTION_COPY_OR_MOVE, null, true, null);
    }

    /**
     * With the component and the listener, accepting copy or move.
     *
     * @throws HeadlessException if there is no screen
     */
    public DropTarget(Component dt, DropTargetListener dtl) throws HeadlessException {
        this(dt, DnDConstants.ACTION_COPY_OR_MOVE, dtl, true, null);
    }

    /**
     * With the component, the actions and the listener.
     *
     * @throws HeadlessException if there is no screen
     */
    public DropTarget(Component dt, int ops, DropTargetListener dtl) throws HeadlessException {
        this(dt, ops, dtl, true, null);
    }

    /**
     * Hooks this destination to another component.
     *
     * <p>It unhooks the previous one: a destination belongs to one component at a time.
     */
    public synchronized void setComponent(Component c) {
        if (this.component == c) {
            return;
        }
        Component previous = this.component;
        this.component = c;
        if (previous != null) {
            this.clearAutoscroll();
            previous.setDropTarget(null);
        }
        if (c != null && c.getDropTarget() != this) {
            c.setDropTarget(this);
        }
    }

    /** The component it is hooked to, or `null`. */
    public synchronized Component getComponent() {
        return this.component;
    }

    /**
     * Changes which actions it accepts.
     *
     * <p>Whatever is not copy, move or link is discarded silently, just as the JDK does.
     */
    public void setDefaultActions(int ops) {
        this.doSetDefaultActions(ops);
    }

    /** The part that does the work, so that the context can call it. */
    void doSetDefaultActions(int ops) {
        this.actions = ops & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
    }

    /** Which actions it accepts. */
    public int getDefaultActions() {
        return this.actions;
    }

    /**
     * Turns the destination on or off.
     *
     * <p>Turning it off while there is a drag over it cuts it short: the automatic scrolling stops
     * and the drag stops receiving an answer.
     */
    public synchronized void setActive(boolean isActive) {
        if (isActive != this.active) {
            this.active = isActive;
            if (!isActive) {
                this.clearAutoscroll();
            }
        }
    }

    /** Whether it is listening. */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Registers the listener.
     *
     * @throws TooManyListenersException if there is one already: two listeners could answer
     *     contradictory things to the same drag
     */
    public synchronized void addDropTargetListener(DropTargetListener dtl)
            throws TooManyListenersException {
        if (dtl == null) {
            return;
        }
        if (this == dtl) {
            throw new IllegalArgumentException("DropTarget may not be its own Listener");
        }
        if (this.dtListener == null) {
            this.dtListener = dtl;
        } else {
            throw new TooManyListenersException();
        }
    }

    /** Removes the listener. */
    public synchronized void removeDropTargetListener(DropTargetListener dtl) {
        if (dtl != null && this.dtListener != null) {
            if (this.dtListener == dtl) {
                this.dtListener = null;
            } else {
                throw new IllegalArgumentException("listener mismatch");
            }
        }
    }

    /** Passes the notice on to the listener and starts the automatic scrolling where it fits. */
    public synchronized void dragEnter(DropTargetDragEvent dtde) {
        if (!this.active) {
            return;
        }
        if (this.dtListener != null) {
            this.dtListener.dragEnter(dtde);
        } else {
            dtde.getDropTargetContext().setTargetActions(DnDConstants.ACTION_NONE);
        }
        this.initializeAutoscrolling(dtde.getLocation());
    }

    /** Passes the notice on to the listener and updates the automatic scrolling. */
    public synchronized void dragOver(DropTargetDragEvent dtde) {
        if (!this.active) {
            return;
        }
        if (this.dtListener != null) {
            this.dtListener.dragOver(dtde);
        }
        this.updateAutoscroll(dtde.getLocation());
    }

    /** Passes the notice on to the listener. */
    public synchronized void dropActionChanged(DropTargetDragEvent dtde) {
        if (!this.active) {
            return;
        }
        if (this.dtListener != null) {
            this.dtListener.dropActionChanged(dtde);
        }
        this.updateAutoscroll(dtde.getLocation());
    }

    /** Passes the notice on to the listener and stops the automatic scrolling. */
    public synchronized void dragExit(DropTargetEvent dte) {
        if (!this.active) {
            return;
        }
        if (this.dtListener != null) {
            this.dtListener.dragExit(dte);
        }
        this.clearAutoscroll();
    }

    /**
     * Passes the drop on to the listener.
     *
     * <p>With no listener, it is rejected: accepting with nobody to read the data would leave the
     * source waiting for a {@code dropComplete} that is not going to come.
     */
    public synchronized void drop(DropTargetDropEvent dtde) {
        this.clearAutoscroll();
        if (this.dtListener != null && this.active) {
            this.dtListener.drop(dtde);
        } else {
            dtde.rejectDrop();
        }
    }

    /** The dictionary of formats this destination uses. */
    public FlavorMap getFlavorMap() {
        return this.flavorMap;
    }

    /** Changes the dictionary; `null` goes back to the system's. */
    public void setFlavorMap(FlavorMap fm) {
        if (fm == null) {
            this.flavorMap = SystemFlavorMap.getDefaultFlavorMap();
        } else {
            this.flavorMap = fm;
        }
    }

    /** Tells that the component became displayable. */
    public void addNotify() {
    }

    /** Tells that the component stopped being displayable; it cuts the scrolling short. */
    public void removeNotify() {
        this.clearAutoscroll();
    }

    /** The channel this destination answers through. */
    public DropTargetContext getDropTargetContext() {
        if (this.dropTargetContext == null) {
            this.dropTargetContext = this.createDropTargetContext();
        }
        return this.dropTargetContext;
    }

    /** Builds the context; a subclass can give its own. */
    protected DropTargetContext createDropTargetContext() {
        return new DropTargetContext(this);
    }

    /** Builds the scrolling timer; a subclass can give its own. */
    protected DropTargetAutoScroller createDropTargetAutoScroller(Component c, Point p) {
        return new DropTargetAutoScroller(c, p);
    }

    /**
     * Starts the automatic scrolling if the component admits it.
     *
     * <p>`instanceof` is checked instead of a flag: implementing {@link Autoscroll} **is** the way
     * of asking for it.
     */
    protected void initializeAutoscrolling(Point p) {
        if (this.component == null || !(this.component instanceof Autoscroll)) {
            return;
        }
        this.autoScroller = this.createDropTargetAutoScroller(this.component, p);
    }

    /** Tells the scrolling where the pointer is now. */
    protected void updateAutoscroll(Point dragCursorLocn) {
        if (this.autoScroller != null) {
            this.autoScroller.updateLocation(dragCursorLocn);
        }
    }

    /** Stops the automatic scrolling. */
    protected void clearAutoscroll() {
        if (this.autoScroller != null) {
            this.autoScroller.stop();
            this.autoScroller = null;
        }
    }

    /**
     * Whoever scrolls the component while the pointer is near an edge.
     *
     * <p>The sum it does is a single one: if the point falls inside the insets the component
     * declared, a scrolling step is asked of it. It lives here and not in the component so that
     * each one does not have to repeat it.
     */
    protected static class DropTargetAutoScroller {

        private final Component component;
        private final Autoscroll autoScroll;
        private Point locn;

        /** With the component to scroll and the initial point. */
        protected DropTargetAutoScroller(Component c, Point p) {
            this.component = c;
            this.autoScroll = (Autoscroll) c;
            this.locn = new Point(p);
        }

        /** Tells it where the pointer is and scrolls if it falls in the sensitive zone. */
        protected void updateLocation(Point newLocn) {
            this.locn = new Point(newLocn);
            Insets margins = this.autoScroll.getAutoscrollInsets();
            int w = this.component.getWidth();
            int h = this.component.getHeight();
            boolean insideZone = this.locn.x < margins.left
                    || this.locn.x > w - margins.right
                    || this.locn.y < margins.top
                    || this.locn.y > h - margins.bottom;
            if (insideZone) {
                this.autoScroll.autoscroll(this.locn);
            }
        }

        /** Stops scrolling. */
        protected void stop() {
            this.locn = null;
        }
    }
}
