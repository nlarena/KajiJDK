package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.LayoutManager;
import java.awt.event.WindowEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A window with decoration, on Swing's side.
 *
 * <p>It is an AWT {@link Frame} with a root pane inside; from there comes almost all its API
 * -- the content pane, the glass pane, the layers -- and also its best-known oddity: adding a
 * component to it directly does not add it to the `JFrame` but to its content pane, because the
 * `JFrame` itself admits no child other than the root pane.
 *
 * <h2>The children go to the content</h2>
 *
 * <p>{@link #addImpl} redirects by itself, and the redirection is switched off while the
 * constructor builds the root pane -- if it were switched on, adding the root pane would
 * redirect to itself. It is exactly the same as {@link JDialog} and {@link JWindow} do.
 *
 * <h2>The closing policy</h2>
 *
 * <p>{@link #setDefaultCloseOperation} decides between hiding, destroying, doing nothing, or
 * ending the program; {@link #processWindowEvent} applies it when the closing event arrives.
 * With no handing out of window events that event does not arrive by itself, but the method is
 * there and does what it says if somebody delivers it to it.
 *
 * @see WindowConstants
 */
public class JFrame extends Frame implements WindowConstants, Accessible, RootPaneContainer,
        TransferHandler.HasGetTransferHandler {

    /** The root pane; see the class note. */
    protected JRootPane rootPane;

    /** Whether adding redirects to the content. */
    protected boolean rootPaneCheckingEnabled = false;

    protected AccessibleContext accessibleContext;

    private TransferHandler transferHandler;


    /** Whether new windows are decorated with the `LookAndFeel` instead of with the system. */
    private static boolean defaultLookAndFeelDecorated = false;

    /** What to do on closing it; one of {@link WindowConstants}' constants. */
    private int defaultCloseOperation = HIDE_ON_CLOSE;

    /**
     * A window with no title, still invisible.
     *
     * @throws HeadlessException if the environment has no screen
     */
    public JFrame() throws HeadlessException {
        super();
        frameInit();
    }

    /**
     * A window with no title in that graphics configuration.
     *
     * @param gc the screen and the mode, or `null` for the usual ones
     */
    public JFrame(GraphicsConfiguration gc) {
        super(gc);
        frameInit();
    }

    /**
     * A window with that title, still invisible.
     *
     * @param title the title, or `null` for none
     * @throws HeadlessException if the environment has no screen
     */
    public JFrame(String title) throws HeadlessException {
        super(title);
        frameInit();
    }

    /**
     * A window with that title in that graphics configuration.
     *
     * @param title the title, or `null` for none
     * @param gc the screen and the mode, or `null` for the usual ones
     */
    public JFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
        frameInit();
    }

    /**
     * It fixes what to do when the user closes it.
     *
     * <p>{@link #processWindowEvent} applies it; see the class note.
     *
     * @param operation one of {@link WindowConstants}' constants
     * @throws IllegalArgumentException if it is not one of the four
     */
    public void setDefaultCloseOperation(int operation) {
        if (operation != DO_NOTHING_ON_CLOSE && operation != HIDE_ON_CLOSE
                && operation != DISPOSE_ON_CLOSE && operation != EXIT_ON_CLOSE) {
            throw new IllegalArgumentException("defaultCloseOperation must be one of: "
                    + "DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE, DISPOSE_ON_CLOSE, or EXIT_ON_CLOSE");
        }
        this.defaultCloseOperation = operation;
    }

    /** What would be done on closing it. By default, {@link WindowConstants#HIDE_ON_CLOSE}. */
    public int getDefaultCloseOperation() {
        return this.defaultCloseOperation;
    }

    /**
     * Whether the windows created from now on are decorated with the `LookAndFeel`.
     *
     * <p>It only affects those created afterwards: the decoration is chosen when they are
     * built.
     */
    public static void setDefaultLookAndFeelDecorated(boolean defaultLookAndFeelDecorated) {
        JFrame.defaultLookAndFeelDecorated = defaultLookAndFeelDecorated;
    }

    /** Whether new windows are decorated with the `LookAndFeel`. By default, no. */
    public static boolean isDefaultLookAndFeelDecorated() {
        return defaultLookAndFeelDecorated;
    }

    // -- the root pane -------------------------------------------------------------------------

    /**
     * It builds the root pane.
     *
     * <p>The redirection is switched on only at the end; see the class note.
     */
    protected void frameInit() {
        enableEvents(java.awt.AWTEvent.KEY_EVENT_MASK | java.awt.AWTEvent.WINDOW_EVENT_MASK);
        setLocale(JComponent.getDefaultLocale());
        setRootPane(createRootPane());
        setBackground(java.awt.Color.white);
        setRootPaneCheckingEnabled(true);
        if (JFrame.isDefaultLookAndFeelDecorated()) {
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        }
    }

    protected JRootPane createRootPane() {
        JRootPane rp = new JRootPane();
        rp.setOpaque(true);
        return rp;
    }

    /** It attends the closing according to {@link #setDefaultCloseOperation}. */
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            int op = getDefaultCloseOperation();
            if (op == HIDE_ON_CLOSE) {
                setVisible(false);
            } else if (op == DISPOSE_ON_CLOSE) {
                dispose();
            } else if (op == EXIT_ON_CLOSE) {
                System.exit(0);
            }
        }
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

    public void setJMenuBar(JMenuBar menubar) {
        getRootPane().setJMenuBar(menubar);
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
                // Switched off while the root pane is added; see the class note.
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

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
