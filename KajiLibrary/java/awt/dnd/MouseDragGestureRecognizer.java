package java.awt.dnd;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * The base of the gesture recognisers that listen to the **mouse**.
 *
 * <p>It is where the recogniser is connected to the component: it implements both mouse interfaces,
 * and {@link #registerListeners} plugs it in. What it does **not** do is decide when the gesture is
 * recognised — the seven mouse methods are empty on purpose.
 *
 * <p>That decision depends on the platform —how many pixels, which button, with which keys— and
 * that is why the class is abstract although it has no abstract method of its own: it inherits the
 * two of {@link DragGestureRecognizer} and leaves the criterion to each system's concrete
 * recogniser.
 */
public abstract class MouseDragGestureRecognizer extends DragGestureRecognizer
        implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 6220099344182281120L;

    /**
     * With everything given.
     *
     * @throws IllegalArgumentException if the drag source is `null`
     */
    protected MouseDragGestureRecognizer(DragSource ds, Component c, int act,
            DragGestureListener dgl) {
        super(ds, c, act, dgl);
    }

    /**
     * With no listener.
     *
     * @throws IllegalArgumentException if the drag source is `null`
     */
    protected MouseDragGestureRecognizer(DragSource ds, Component c, int act) {
        this(ds, c, act, null);
    }

    /**
     * Accepting any action.
     *
     * @throws IllegalArgumentException if the drag source is `null`
     */
    protected MouseDragGestureRecognizer(DragSource ds, Component c) {
        this(ds, c, DnDConstants.ACTION_NONE);
    }

    /**
     * With no component yet.
     *
     * @throws IllegalArgumentException if the drag source is `null`
     */
    protected MouseDragGestureRecognizer(DragSource ds) {
        this(ds, null);
    }

    /** It hooks itself to the mouse events of the component. */
    protected void registerListeners() {
        this.component.addMouseListener(this);
        this.component.addMouseMotionListener(this);
    }

    /** It unhooks itself. */
    protected void unregisterListeners() {
        this.component.removeMouseListener(this);
        this.component.removeMouseMotionListener(this);
    }

    /** It does nothing: the criterion is put by the concrete recogniser. */
    public void mouseClicked(MouseEvent e) {
    }

    /** It does nothing. */
    public void mousePressed(MouseEvent e) {
    }

    /** It does nothing. */
    public void mouseReleased(MouseEvent e) {
    }

    /** It does nothing. */
    public void mouseEntered(MouseEvent e) {
    }

    /** It does nothing. */
    public void mouseExited(MouseEvent e) {
    }

    /** It does nothing. */
    public void mouseDragged(MouseEvent e) {
    }

    /** It does nothing. */
    public void mouseMoved(MouseEvent e) {
    }
}
