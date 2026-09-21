package javax.swing;

import java.awt.Component;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopPaneUI;

/**
 * The container of internal frames.
 *
 * <h2>It is a layered pane</h2>
 *
 * <p>It inherits from {@link JLayeredPane} because the frames overlap and one has to know which
 * is on top. What it adds is the notion of the <em>active frame</em> and a
 * {@link DesktopManager} that decides how they behave.
 *
 * <h2>The icons are children too</h2>
 *
 * <p>A minimized frame is taken off the desktop and its {@code JDesktopIcon} is put in its
 * place. That is why {@link #getAllFrames} looks at both things: the children that are frames
 * and those that are icons, asking each icon for its frame. Walking the children looking only
 * for {@code JInternalFrame} would lose the minimized ones.
 *
 * <h2>Live dragging or with an outline</h2>
 *
 * <p>{@link #LIVE_DRAG_MODE} redraws the whole frame while it is moved;
 * {@link #OUTLINE_DRAG_MODE} draws only a rectangle and moves on releasing. The second exists
 * for desktops with many frames, where redrawing live drags.
 */
public class JDesktopPane extends JLayeredPane implements Accessible {

    private static final String uiClassID = "DesktopPaneUI";

    /** The whole frame is redrawn while it is moved. */
    public static final int LIVE_DRAG_MODE = 0;

    /** Only the outline is drawn and it moves on releasing. */
    public static final int OUTLINE_DRAG_MODE = 1;

    transient DesktopManager desktopManager;

    private transient JInternalFrame selectedFrame = null;
    private int dragMode = LIVE_DRAG_MODE;
    private boolean dragModeSet = false;

    /** An empty desktop. */
    public JDesktopPane() {
        setFocusCycleRoot(true);
        setOpaque(true);
        updateUI();
    }

    public DesktopPaneUI getUI() {
        return (DesktopPaneUI) ui;
    }

    public void setUI(DesktopPaneUI ui) {
        super.setUI(ui);
    }

    /**
     * How a frame's dragging is seen.
     *
     * <p>It validates nothing, and this is measured against the JDK: its documentation promises an
     * {@link IllegalArgumentException} for an unknown mode and the code does not throw it. The code
     * is copied and not the promise -- an odd mode is kept and the look and feel ignores it.
     */
    public void setDragMode(int dragMode) {
        int oldDragMode = this.dragMode;
        this.dragMode = dragMode;
        firePropertyChange("dragMode", oldDragMode, this.dragMode);
        dragModeSet = true;
    }

    public int getDragMode() {
        return dragMode;
    }

    /** Who decides how the frames behave; see {@link DesktopManager}. */
    public DesktopManager getDesktopManager() {
        return desktopManager;
    }

    public void setDesktopManager(DesktopManager d) {
        DesktopManager oldValue = desktopManager;
        desktopManager = d;
        firePropertyChange("desktopManager", oldValue, desktopManager);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Every frame, minimized ones included; see the class note. */
    public JInternalFrame[] getAllFrames() {
        Vector<JInternalFrame> vResults = new Vector<JInternalFrame>(10);
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            Component next = getComponent(i);
            if (next instanceof JInternalFrame) {
                vResults.addElement((JInternalFrame) next);
            } else if (next instanceof JInternalFrame.JDesktopIcon) {
                JInternalFrame.JDesktopIcon icon = (JInternalFrame.JDesktopIcon) next;
                JInternalFrame tmp = icon.getInternalFrame();
                if (tmp != null) {
                    vResults.addElement(tmp);
                }
            }
        }
        JInternalFrame[] results = new JInternalFrame[vResults.size()];
        vResults.copyInto(results);
        return results;
    }

    /** The active frame, or null if none is. */
    public JInternalFrame getSelectedFrame() {
        return selectedFrame;
    }

    /**
     * It notes which the active frame is.
     *
     * <p>It only notes: it does not activate it. Who activates is
     * {@link JInternalFrame#setSelected}, and this method is there for the desktop to learn about
     * it.
     */
    public void setSelectedFrame(JInternalFrame f) {
        selectedFrame = f;
    }

    /** That layer's frames, minimized ones included. */
    public JInternalFrame[] getAllFramesInLayer(int layer) {
        Vector<JInternalFrame> vResults = new Vector<JInternalFrame>(10);
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            Component next = getComponent(i);
            if (next instanceof JInternalFrame) {
                JInternalFrame v = (JInternalFrame) next;
                if (v.getLayer() == layer) {
                    vResults.addElement(v);
                }
            } else if (next instanceof JInternalFrame.JDesktopIcon) {
                JInternalFrame.JDesktopIcon icon = (JInternalFrame.JDesktopIcon) next;
                JInternalFrame tmp = icon.getInternalFrame();
                if (tmp != null && tmp.getLayer() == layer) {
                    vResults.addElement(tmp);
                }
            }
        }
        JInternalFrame[] results = new JInternalFrame[vResults.size()];
        vResults.copyInto(results);
        return results;
    }

    /**
     * It activates the next frame or the previous one.
     *
     * <p>It is what Ctrl+F6 does. It wraps round on reaching the end: on a desktop there is no
     * final frame to get stuck on.
     *
     * @return the one that was left active, or null if there is none.
     */
    public JInternalFrame selectFrame(boolean forward) {
        JInternalFrame[] frames = getAllFrames();
        if (frames.length == 0) {
            return null;
        }
        int current = -1;
        JInternalFrame sel = getSelectedFrame();
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] == sel) {
                current = i;
            }
        }
        int next;
        if (current < 0) {
            next = forward ? 0 : frames.length - 1;
        } else {
            next = forward ? current + 1 : current - 1;
            if (next >= frames.length) {
                next = 0;
            } else if (next < 0) {
                next = frames.length - 1;
            }
        }
        JInternalFrame chosen = frames[next];
        try {
            chosen.setSelected(true);
            chosen.moveToFront();
        } catch (java.beans.PropertyVetoException e) {
            // If it is vetoed, the selection does not change; the one that was attempted is
            // returned all the same.
        }
        return chosen;
    }

    /**
     * It removes a child.
     *
     * <p>Removing the active frame does <em>not</em> leave the desktop with no active one:
     * {@link #getSelectedFrame} goes on returning the one that left. It looks like an oversight
     * and it is what the JDK does -- it is measured --, so it is copied. Whoever removes a frame
     * by hand and wants the desktop clean has to call {@link #setSelectedFrame} with null
     * themselves; the desktop manager already does it on closing.
     */
    public void remove(Component comp) {
        super.remove(comp);
    }

    /** It removes the child at that position; see {@link #remove(Component)}. */
    public void remove(int index) {
        super.remove(index);
    }

    /**
     * It removes them all; it does not forget the active one either. See {@link
     * #remove(Component)}.
     */
    public void removeAll() {
        super.removeAll();
    }

    public void setComponentZOrder(Component comp, int index) {
        super.setComponentZOrder(comp, index);
    }

    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
