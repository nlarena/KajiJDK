package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.LayoutManager;
import java.awt.Window;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A window with neither frame nor title bar.
 *
 * <h2>What a window that cannot be moved or closed is for</h2>
 *
 * <p>For what is drawn <em>on top</em> of everything and disappears by itself: a drop-down menu
 * that goes outside the application's edge, a tool tip, a splash screen. All that needs a
 * system window -- otherwise, it cannot go outside its application -- but it needs none of the
 * trimmings.
 *
 * <p>As it has no title bar, it has no close button either, and therefore it <strong>cannot
 * receive the keyboard focus on its own</strong>. Whoever opens it has to remember to close
 * it.
 *
 * <h2>The children go to the content</h2>
 *
 * <p>As in {@link JDialog}, inside there is a {@link JRootPane} and {@code add} redirects to
 * its content pane; see that class's note, which also explains why the redirection is switched
 * off while the root pane is built.
 */
public class JWindow extends Window implements Accessible, RootPaneContainer,
        TransferHandler.HasGetTransferHandler {

    /** The root pane; see the class note. */
    protected JRootPane rootPane;

    /** Whether adding redirects to the content. */
    protected boolean rootPaneCheckingEnabled = false;

    protected AccessibleContext accessibleContext;

    private TransferHandler transferHandler;

    /**
     * A window with no owner.
     *
     * <p>It hangs from a shared and hidden window: a system window needs some window to depend
     * on.
     *
     * @throws java.awt.HeadlessException if there is no screen
     */
    public JWindow() {
        this((Frame) null);
    }

    /**
     * In that screen configuration.
     *
     * @throws java.awt.HeadlessException if there is no screen
     */
    public JWindow(GraphicsConfiguration gc) {
        this(null, gc);
        super.setFocusableWindowState(false);
    }

    /**
     * With that window as owner.
     *
     * @throws java.awt.HeadlessException if there is no screen
     */
    public JWindow(Frame owner) {
        super(owner == null ? SwingUtilities.getSharedOwnerFrame() : owner);
        if (owner == null) {
            super.setFocusableWindowState(false);
        }
        windowInit();
    }

    /**
     * With that window as owner.
     *
     * @throws java.awt.HeadlessException if there is no screen
     */
    public JWindow(Window owner) {
        super(owner == null ? (Window) SwingUtilities.getSharedOwnerFrame() : owner);
        if (owner == null) {
            super.setFocusableWindowState(false);
        }
        windowInit();
    }

    /**
     * With owner and screen configuration.
     *
     * @throws java.awt.HeadlessException if there is no screen
     */
    public JWindow(Window owner, GraphicsConfiguration gc) {
        super(owner == null ? (Window) SwingUtilities.getSharedOwnerFrame() : owner, gc);
        if (owner == null) {
            super.setFocusableWindowState(false);
        }
        windowInit();
    }

    /** It builds the root pane; the redirection is switched on only at the end. */
    protected void windowInit() {
        setLocale(JComponent.getDefaultLocale());
        setRootPane(createRootPane());
        setRootPaneCheckingEnabled(true);
    }

    protected JRootPane createRootPane() {
        JRootPane rp = new JRootPane();
        rp.setOpaque(true);
        return rp;
    }

    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    public void setTransferHandler(TransferHandler newHandler) {
        TransferHandler oldHandler = transferHandler;
        transferHandler = newHandler;
        firePropertyChange("transferHandler", oldHandler, newHandler);
    }

    public TransferHandler getTransferHandler() {
        return transferHandler;
    }

    /**
     * It draws without clearing the background first.
     *
     * <p>Swing draws every pixel that falls to it, so clearing beforehand only produces a
     * flicker.
     */
    public void update(Graphics g) {
        paint(g);
    }

    protected void setRootPaneCheckingEnabled(boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }

    /**
     * It adds to the content, not to the window.
     *
     * @throws IllegalArgumentException if the root pane is added with the redirection switched
     *     on
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().add(comp, constraints, index);
        } else {
            super.addImpl(comp, constraints, index);
        }
    }

    /** It removes from the content, unless it is the root pane. */
    public void remove(Component comp) {
        if (comp == rootPane) {
            super.remove(comp);
        } else {
            getContentPane().remove(comp);
        }
    }

    /** It gives the layout to the content, not to the window. */
    public void setLayout(LayoutManager manager) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().setLayout(manager);
        } else {
            super.setLayout(manager);
        }
    }

    public JRootPane getRootPane() {
        return rootPane;
    }

    protected void setRootPane(JRootPane root) {
        if (rootPane != null) {
            remove(rootPane);
        }
        rootPane = root;
        if (rootPane != null) {
            boolean checkingEnabled = isRootPaneCheckingEnabled();
            try {
                // Switched off while the root pane is added: otherwise, it would redirect to
                // itself.
                setRootPaneCheckingEnabled(false);
                add(rootPane, java.awt.BorderLayout.CENTER);
            } finally {
                setRootPaneCheckingEnabled(checkingEnabled);
            }
        }
    }

    public Container getContentPane() {
        return getRootPane().getContentPane();
    }

    public void setContentPane(Container contentPane) {
        getRootPane().setContentPane(contentPane);
    }

    public JLayeredPane getLayeredPane() {
        return getRootPane().getLayeredPane();
    }

    public void setLayeredPane(JLayeredPane layeredPane) {
        getRootPane().setLayeredPane(layeredPane);
    }

    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }

    public void setGlassPane(Component glassPane) {
        getRootPane().setGlassPane(glassPane);
    }

    public Graphics getGraphics() {
        return super.getGraphics();
    }

    public void repaint(long time, int x, int y, int width, int height) {
        super.repaint(time, x, y, width, height);
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
