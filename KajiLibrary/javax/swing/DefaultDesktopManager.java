package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.io.Serializable;

/**
 * The desktop manager that comes set.
 *
 * <h2>Where the state is kept</h2>
 *
 * <p>A manager attends every frame of a desktop, so it cannot keep in fields of its own things
 * that belong to each frame. It keeps them in the frame itself: the rectangle from before
 * maximizing goes in {@link JInternalFrame#setNormalBounds}, and the "it has already been
 * minimized" mark in a client property. Hence the four protected methods
 * {@link #setPreviousBounds}, {@link #getPreviousBounds}, {@link #setWasIcon} and
 * {@link #wasIcon}: they are the point where a subclass may change that keeping.
 *
 * <h2>Why "it has already been minimized" matters</h2>
 *
 * <p>The first time a frame is minimized a place has to be chosen for the icon; the following
 * times the place where the user left it has to be respected. Without that mark, each
 * minimizing would return the icon to the corner.
 *
 * <h2>How an icon is placed</h2>
 *
 * <p>{@link #getBoundsForIconOf} tries places in a row at the foot of the desktop and keeps the
 * first that does not overlap another icon; when the row is full it goes up one. It is a simple
 * arrangement and on purpose: anything finer depends on the look and feel.
 */
public class DefaultDesktopManager implements DesktopManager, Serializable {

    static final String HAS_BEEN_ICONIFIED_PROPERTY = "wasIconOnce";

    static final int DEFAULT_DRAG_MODE = 0;
    static final int OUTLINE_DRAG_MODE = 1;
    static final int FASTER_DRAG_MODE = 2;

    int dragMode = DEFAULT_DRAG_MODE;

    private Rectangle currentBounds = null;

    /** The usual manager. */
    public DefaultDesktopManager() {
    }

    /**
     * It shows the frame in place of its icon.
     *
     * <p>It only does something if the icon was set: opening a frame that is already open must not
     * take it from where it is.
     */
    public void openFrame(JInternalFrame f) {
        if (f.getDesktopIcon().getParent() != null) {
            f.getDesktopIcon().getParent().add(f);
            removeIconFor(f);
        }
    }

    /**
     * It takes the frame off the desktop.
     *
     * <p>If it was the active one, it deactivates it first: leaving the desktop pointing at a frame
     * that is no longer there would be a ghost. The kept rectangle and the minimized mark are also
     * forgotten, because a closed frame that is added again starts from scratch.
     */
    public void closeFrame(JInternalFrame f) {
        JDesktopPane d = f.getDesktopPane();
        boolean wasActive = f.isSelected();
        Container c = f.getParent();
        if (wasActive) {
            try {
                f.setSelected(false);
            } catch (PropertyVetoException e2) {
                // It closes all the same: the frame leaves the desktop.
            }
        }
        if (c != null) {
            Rectangle r = f.getBounds();
            c.remove(f);
            c.repaint(r.x, r.y, r.width, r.height);
        }
        removeIconFor(f);
        if (getPreviousBounds(f) != null) {
            setPreviousBounds(f, null);
        }
        if (wasIcon(f)) {
            setWasIcon(f, null);
        }
        if (wasActive && d != null) {
            d.setSelectedFrame(null);
        }
    }

    /**
     * It enlarges the frame to the whole desktop.
     *
     * <p>A minimized one is restored first: an icon cannot be maximized.
     */
    public void maximizeFrame(JInternalFrame f) {
        if (f.isIcon()) {
            try {
                f.setIcon(false);
            } catch (PropertyVetoException e2) {
                // If it cannot be restored it cannot be maximized either.
                return;
            }
        } else {
            setPreviousBounds(f, f.getBounds());
            Container parent = f.getParent();
            if (parent != null) {
                Rectangle bounds = parent.getBounds();
                setBoundsForFrame(f, 0, 0, bounds.width, bounds.height);
            }
        }
        try {
            f.setSelected(true);
        } catch (PropertyVetoException e2) {
            // It is left maximized even though it cannot be activated.
        }
    }

    /** It gives it back the rectangle it had before maximizing. */
    public void minimizeFrame(JInternalFrame f) {
        Rectangle r = getPreviousBounds(f);
        if (r != null) {
            setPreviousBounds(f, null);
            try {
                f.setSelected(true);
            } catch (PropertyVetoException e2) {
                // It goes back to its size even though it cannot be activated.
            }
            setBoundsForFrame(f, r.x, r.y, r.width, r.height);
        }
    }

    /**
     * It replaces the frame with its icon.
     *
     * <p>The icon inherits the frame's layer: otherwise, a frame of the modal layer would minimize
     * to an icon that ends up below the others.
     */
    public void iconifyFrame(JInternalFrame f) {
        JInternalFrame.JDesktopIcon icon = f.getDesktopIcon();
        Container c = f.getParent();
        JDesktopPane d = f.getDesktopPane();
        boolean wasActive = f.isSelected();
        if (c == null) {
            return;
        }
        if (!wasIcon(f)) {
            // The first time: a place has to be chosen for it. See the class note.
            Rectangle r = getBoundsForIconOf(f);
            icon.setBounds(r.x, r.y, r.width, r.height);
            setWasIcon(f, Boolean.TRUE);
        }
        if (c instanceof JLayeredPane) {
            JLayeredPane lp = (JLayeredPane) c;
            int layer = lp.getLayer(f);
            JLayeredPane.putLayer(icon, layer);
        }
        Rectangle r = f.getBounds();
        c.remove(f);
        c.add(icon);
        c.repaint(r.x, r.y, r.width, r.height);
        if (wasActive) {
            try {
                f.setSelected(false);
            } catch (PropertyVetoException e2) {
                // It minimizes all the same.
            }
            if (d != null) {
                d.setSelectedFrame(null);
            }
        }
    }

    /** It gives the frame back in place of its icon. */
    public void deiconifyFrame(JInternalFrame f) {
        JInternalFrame.JDesktopIcon icon = f.getDesktopIcon();
        Container c = icon.getParent();
        if (c == null) {
            return;
        }
        c.add(f);
        removeIconFor(f);
        try {
            f.setSelected(true);
        } catch (PropertyVetoException e2) {
            // It comes back all the same even though it cannot be activated.
        }
    }

    /**
     * The frame became the active one.
     *
     * <p>Deactivating the previous one is part of the job: two frames with the title bar lit at
     * the same time is exactly what this method avoids.
     */
    public void activateFrame(JInternalFrame f) {
        Container p = f.getParent();
        JDesktopPane d = f.getDesktopPane();
        JInternalFrame active = (d == null) ? null : d.getSelectedFrame();
        if (p == null) {
            return;
        }
        if (active == null) {
            if (d != null) {
                d.setSelectedFrame(f);
            }
        } else if (active != f) {
            if (active.isSelected()) {
                try {
                    active.setSelected(false);
                } catch (PropertyVetoException e2) {
                    // If the previous one refuses, the new one is activated all the same.
                }
            }
            if (d != null) {
                d.setSelectedFrame(f);
            }
        }
        f.moveToFront();
    }

    /** The frame stopped being the active one. */
    public void deactivateFrame(JInternalFrame f) {
        JDesktopPane d = f.getDesktopPane();
        JInternalFrame active = (d == null) ? null : d.getSelectedFrame();
        if (active == f) {
            d.setSelectedFrame(null);
        }
    }

    /** It begins a drag; it takes the mode from the desktop. */
    public void beginDraggingFrame(JComponent f) {
        JDesktopPane d = getDesktopPane(f);
        if (d != null && d.getDragMode() == JDesktopPane.OUTLINE_DRAG_MODE) {
            dragMode = OUTLINE_DRAG_MODE;
        } else {
            dragMode = DEFAULT_DRAG_MODE;
        }
        currentBounds = f.getBounds();
    }

    /**
     * The drag is going by that position.
     *
     * <p>In outline mode only where it is going is noted: really moving is done on releasing.
     */
    public void dragFrame(JComponent f, int newX, int newY) {
        if (dragMode == OUTLINE_DRAG_MODE) {
            currentBounds = new Rectangle(newX, newY, f.getWidth(), f.getHeight());
        } else {
            setBoundsForFrame(f, newX, newY, f.getWidth(), f.getHeight());
        }
    }

    /** It ends the drag; in outline mode only here does it move. */
    public void endDraggingFrame(JComponent f) {
        if (dragMode == OUTLINE_DRAG_MODE && currentBounds != null) {
            setBoundsForFrame(f, currentBounds.x, currentBounds.y, currentBounds.width,
                    currentBounds.height);
        }
        currentBounds = null;
    }

    /** It begins resizing; the same scheme as the drag. */
    public void beginResizingFrame(JComponent f, int direction) {
        JDesktopPane d = getDesktopPane(f);
        if (d != null && d.getDragMode() == JDesktopPane.OUTLINE_DRAG_MODE) {
            dragMode = OUTLINE_DRAG_MODE;
        } else {
            dragMode = DEFAULT_DRAG_MODE;
        }
        currentBounds = f.getBounds();
    }

    /** The resizing is going by that rectangle. */
    public void resizeFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
        if (dragMode == OUTLINE_DRAG_MODE) {
            currentBounds = new Rectangle(newX, newY, newWidth, newHeight);
        } else {
            setBoundsForFrame(f, newX, newY, newWidth, newHeight);
        }
    }

    /** It ends the resizing. */
    public void endResizingFrame(JComponent f) {
        if (dragMode == OUTLINE_DRAG_MODE && currentBounds != null) {
            setBoundsForFrame(f, currentBounds.x, currentBounds.y, currentBounds.width,
                    currentBounds.height);
        }
        currentBounds = null;
    }

    /**
     * It moves and resizes the frame.
     *
     * <p>The old rectangle <em>and</em> the new one are repainted: repainting only the new one
     * would leave the trace of the place it came from painted.
     */
    public void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
        boolean resize = (f.getWidth() != newWidth || f.getHeight() != newHeight);
        Rectangle before = f.getBounds();
        f.setBounds(newX, newY, newWidth, newHeight);
        if (resize) {
            f.validate();
        }
        Container parent = f.getParent();
        if (parent != null) {
            parent.repaint(before.x, before.y, before.width, before.height);
            parent.repaint(newX, newY, newWidth, newHeight);
        }
    }

    /** It takes the icon off the desktop. */
    protected void removeIconFor(JInternalFrame f) {
        JInternalFrame.JDesktopIcon di = f.getDesktopIcon();
        Container c = di.getParent();
        if (c != null) {
            Rectangle r = di.getBounds();
            c.remove(di);
            c.repaint(r.x, r.y, r.width, r.height);
        }
    }

    /**
     * It chooses a place for that frame's icon.
     *
     * <p>It tries places in a row at the foot of the desktop and returns the first that does not
     * overlap another icon; when the row is full it goes up one. See the class note.
     */
    protected Rectangle getBoundsForIconOf(JInternalFrame f) {
        JInternalFrame.JDesktopIcon icon = f.getDesktopIcon();
        Dimension measured = icon.getPreferredSize();
        Container c = f.getParent();
        if (c == null) {
            c = f.getDesktopIcon().getParent();
        }
        if (c == null) {
            // It is not anywhere yet: the corner is as good as any other.
            return new Rectangle(0, 0, measured.width, measured.height);
        }
        Rectangle bounds = c.getBounds();
        Component[] children = c.getComponents();
        int w = measured.width;
        int h = measured.height;
        int x = 0;
        int y = bounds.height - h;
        Rectangle free = new Rectangle(x, y, w, h);
        boolean found = false;
        while (!found) {
            free = new Rectangle(x, y, w, h);
            found = true;
            for (int i = 0; i < children.length; i++) {
                JInternalFrame.JDesktopIcon other = null;
                if (children[i] instanceof JInternalFrame) {
                    other = ((JInternalFrame) children[i]).getDesktopIcon();
                } else if (children[i] instanceof JInternalFrame.JDesktopIcon) {
                    other = (JInternalFrame.JDesktopIcon) children[i];
                } else {
                    continue;
                }
                if (icon != other && other.isVisible()) {
                    if (free.intersects(other.getBounds())) {
                        found = false;
                        break;
                    }
                }
            }
            if (!found) {
                x = x + w;
                if (x + w > bounds.width) {
                    x = 0;
                    y = y - h;
                }
            }
        }
        return free;
    }

    /** It keeps the rectangle from before maximizing; see the class note. */
    protected void setPreviousBounds(JInternalFrame f, Rectangle r) {
        f.setNormalBounds(r);
    }

    /** The kept rectangle, or null. */
    protected Rectangle getPreviousBounds(JInternalFrame f) {
        return f.getNormalBounds();
    }

    /** It marks that the frame has already been minimized; see the class note. */
    protected void setWasIcon(JInternalFrame f, Boolean value) {
        if (value != null) {
            f.putClientProperty(HAS_BEEN_ICONIFIED_PROPERTY, value);
        }
    }

    /** Whether it has already been minimized at some point. */
    protected boolean wasIcon(JInternalFrame f) {
        return (f.getClientProperty(HAS_BEEN_ICONIFIED_PROPERTY) == Boolean.TRUE);
    }

    /** That component's desktop, looking upwards. */
    JDesktopPane getDesktopPane(JComponent frame) {
        JDesktopPane pane = null;
        Component c = frame.getParent();
        while (pane == null) {
            if (c instanceof JDesktopPane) {
                pane = (JDesktopPane) c;
            } else if (c == null) {
                break;
            } else {
                c = c.getParent();
            }
        }
        return pane;
    }
}
