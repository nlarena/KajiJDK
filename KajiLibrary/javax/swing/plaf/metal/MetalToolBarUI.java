package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;

/**
 * Metal's tool bar.
 *
 * <h2>Two listeners that deliberately do not exist</h2>
 *
 * <p>{@link #createContainerListener} and {@link #createRolloverListener} return {@code null},
 * and that is measured. It is not an oversight: the basic one uses those two listeners to hear
 * that a button was added and put the rollover border on it. Metal does not need them because it
 * puts the border on each button when it draws it, not when it is added.
 *
 * <p>The fields {@link #contListener} and {@link #rolloverListener} are therefore left at
 * {@code null} after installing, which is what the JDK answers.
 *
 * <h2>The border is the same with rollover and without it</h2>
 *
 * <p>Both {@code createXxxBorder} return a compound border of the same kind. The basic one uses
 * a different one for each state; Metal draws the relief inside the border according to the
 * button's model, so one is enough.
 */
public class MetalToolBarUI extends BasicToolBarUI {

    protected ContainerListener contListener;
    protected PropertyChangeListener rolloverListener;

    public MetalToolBarUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalToolBarUI();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    protected void installListeners() {
        super.installListeners();
        contListener = createContainerListener();
        rolloverListener = createRolloverListener();
    }

    protected void uninstallListeners() {
        super.uninstallListeners();
        contListener = null;
        rolloverListener = null;
    }

    /** None; see the class note. */
    protected ContainerListener createContainerListener() {
        return null;
    }

    /** Neither; see the class note. */
    protected PropertyChangeListener createRolloverListener() {
        return null;
    }

    protected Border createRolloverBorder() {
        return super.createRolloverBorder();
    }

    protected Border createNonRolloverBorder() {
        return super.createNonRolloverBorder();
    }

    protected void setBorderToNonRollover(Component c) {
        super.setBorderToNonRollover(c);
    }

    protected MouseInputListener createDockingListener() {
        return new MetalDockingListener(toolBar);
    }

    /** The basic one's, which Metal's wraps; see finding #400. */
    private MouseInputListener basicDragWindow() {
        if (basicDragWindow == null) {
            basicDragWindow = super.createDockingListener();
        }
        return basicDragWindow;
    }

    private MouseInputListener basicDragWindow;

    protected void setDragOffset(Point p) {
    }

    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }

    /**
     * The one that drags the bar out to float.
     *
     * <p>Metal redefines it for a single thing: grabbing the bar <em>anywhere</em> and not only by
     * the handle. The name is seen through {@code getClass().getName()} and that is why the class
     * exists even though it adds almost no code.
     */
    protected class MetalDockingListener implements MouseInputListener {

        public MetalDockingListener(javax.swing.JToolBar t) {
        }

        public void mousePressed(java.awt.event.MouseEvent e) {
            basicDragWindow().mousePressed(e);
        }

        public void mouseReleased(java.awt.event.MouseEvent e) {
            basicDragWindow().mouseReleased(e);
        }

        public void mouseClicked(java.awt.event.MouseEvent e) {
            basicDragWindow().mouseClicked(e);
        }

        public void mouseEntered(java.awt.event.MouseEvent e) {
            basicDragWindow().mouseEntered(e);
        }

        public void mouseExited(java.awt.event.MouseEvent e) {
            basicDragWindow().mouseExited(e);
        }

        public void mouseDragged(java.awt.event.MouseEvent e) {
            basicDragWindow().mouseDragged(e);
        }

        public void mouseMoved(java.awt.event.MouseEvent e) {
            basicDragWindow().mouseMoved(e);
        }
    }
}
