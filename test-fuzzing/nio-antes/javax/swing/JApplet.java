package javax.swing;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.LayoutManager;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A Swing applet.
 *
 * <h2>History, not technology</h2>
 *
 * <p>Applets no longer run in any browser: the Java plug-in was withdrawn and {@link Applet} is
 * marked obsolete in the JDK. This class exists because its superclass exists, and because old
 * code names it.
 *
 * <p>The only thing it adds over {@code Applet} is the same as what {@link JWindow} adds over
 * {@code Window}: a {@link JRootPane} inside, with content pane, layered pane, glass pane and
 * menu bar. All that serves just as well outside a browser -- a {@code JApplet} can be put
 * inside a panel like any component --, which is the only reason anybody would touch it
 * today.
 *
 * <p>The children go to the content; see {@link JDialog}'s note.
 */
public class JApplet extends Applet implements Accessible, RootPaneContainer,
        TransferHandler.HasGetTransferHandler {

    /** The root pane; see the class note. */
    protected JRootPane rootPane;

    /** Whether adding redirects to the content. */
    protected boolean rootPaneCheckingEnabled = false;

    protected AccessibleContext accessibleContext;

    private TransferHandler transferHandler;

    /**
     * An applet with its root pane.
     *
     * @throws HeadlessException if there is no screen
     */
    public JApplet() throws HeadlessException {
        super();
        setForeground(java.awt.Color.black);
        setBackground(java.awt.Color.white);
        setLocale(JComponent.getDefaultLocale());
        setLayout(new java.awt.BorderLayout());
        setRootPane(createRootPane());
        setRootPaneCheckingEnabled(true);
    }

    protected JRootPane createRootPane() {
        JRootPane rp = new JRootPane();
        rp.setOpaque(true);
        return rp;
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

    public void setJMenuBar(JMenuBar menuBar) {
        getRootPane().setJMenuBar(menuBar);
    }

    public JMenuBar getJMenuBar() {
        return getRootPane().getJMenuBar();
    }

    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    protected void setRootPaneCheckingEnabled(boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }

    /**
     * It adds to the content, not to the applet.
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

    /** It gives the layout to the content, not to the applet. */
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
