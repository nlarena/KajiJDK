package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dialog$ModalityType;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A secondary Swing window.
 *
 * <h2>The children go to the content, not to the dialog</h2>
 *
 * <p>A {@code JDialog} has a {@link JRootPane} inside, and everything that is added goes to its
 * content pane. {@link #addImpl} redirects it by itself: {@code dialog.add(button)} really adds
 * to the content.
 *
 * <p>That redirection can be switched off with {@link #setRootPaneCheckingEnabled}, and it is
 * what the constructor does while it builds the root pane: if it were switched on, adding the
 * root pane would redirect to itself.
 *
 * <h2>What to do on closing</h2>
 *
 * <p>{@link #setDefaultCloseOperation} decides between hiding, destroying, doing nothing, or
 * ending the program. By default it hides, which is what is right for a dialog that is going to
 * be opened again; destroying releases the system window and forces it to be rebuilt.
 *
 * <h2>With no screen</h2>
 *
 * <p>A dialog needs a system window. With no screen it can be built and configured -- and that
 * is what is needed for whatever uses it to compile -- but not shown.
 */
public class JDialog extends Dialog implements WindowConstants, Accessible,
        RootPaneContainer, TransferHandler$HasGetTransferHandler {

    private static boolean defaultLookAndFeelDecorated = false;

    /** The root pane; see the class note. */
    protected JRootPane rootPane;

    /** Whether adding redirects to the content. */
    protected boolean rootPaneCheckingEnabled = false;

    protected AccessibleContext accessibleContext;

    private int defaultCloseOperation = HIDE_ON_CLOSE;
    private TransferHandler transferHandler;

    /** A dialog with no owner and not modal. */
    public JDialog() {
        this((Frame) null, false);
    }

    /** A dialog of that window, not modal. */
    public JDialog(Frame owner) {
        this(owner, false);
    }

    /** A dialog of that window. */
    public JDialog(Frame owner, boolean modal) {
        this(owner, null, modal);
    }

    /** A dialog with that title, not modal. */
    public JDialog(Frame owner, String title) {
        this(owner, title, false);
    }

    /** A dialog with that title. */
    public JDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        dialogInit();
    }

    /** A dialog in that screen configuration. */
    public JDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        dialogInit();
    }

    /** A dialog of another dialog, not modal. */
    public JDialog(Dialog owner) {
        this(owner, false);
    }

    public JDialog(Dialog owner, boolean modal) {
        this(owner, null, modal);
    }

    public JDialog(Dialog owner, String title) {
        this(owner, title, false);
    }

    public JDialog(Dialog owner, String title, boolean modal) {
        super(owner, title, modal);
        dialogInit();
    }

    public JDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        dialogInit();
    }

    /** A dialog of that window, not modal. */
    public JDialog(Window owner) {
        this(owner, Dialog$ModalityType.MODELESS);
    }

    /** A dialog with that modality type. */
    public JDialog(Window owner, Dialog$ModalityType modalityType) {
        this(owner, null, modalityType);
    }

    public JDialog(Window owner, String title) {
        this(owner, title, Dialog$ModalityType.MODELESS);
    }

    public JDialog(Window owner, String title, Dialog$ModalityType modalityType) {
        super(owner, title, modalityType);
        dialogInit();
    }

    public JDialog(Window owner, String title, Dialog$ModalityType modalityType,
            GraphicsConfiguration gc) {
        super(owner, title, modalityType, gc);
        dialogInit();
    }

    /**
     * It builds the root pane.
     *
     * <p>The redirection is switched on only at the end; see the class note.
     */
    protected void dialogInit() {
        enableEvents(java.awt.AWTEvent.KEY_EVENT_MASK | java.awt.AWTEvent.WINDOW_EVENT_MASK);
        setRootPane(createRootPane());
        setRootPaneCheckingEnabled(true);
        if (isDefaultLookAndFeelDecorated()) {
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
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
            if (defaultCloseOperation == HIDE_ON_CLOSE) {
                setVisible(false);
            } else if (defaultCloseOperation == DISPOSE_ON_CLOSE) {
                dispose();
            } else if (defaultCloseOperation == EXIT_ON_CLOSE) {
                System.exit(0);
            }
        }
    }

    /**
     * What to do when the user closes the dialog.
     *
     * @throws IllegalArgumentException if it is not one of the four.
     */
    public void setDefaultCloseOperation(int operation) {
        if (operation != DO_NOTHING_ON_CLOSE && operation != HIDE_ON_CLOSE
                && operation != DISPOSE_ON_CLOSE && operation != EXIT_ON_CLOSE) {
            throw new IllegalArgumentException("defaultCloseOperation must be"
                    + " one of: DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE, DISPOSE_ON_CLOSE,"
                    + " or EXIT_ON_CLOSE");
        }
        int oldValue = this.defaultCloseOperation;
        this.defaultCloseOperation = operation;
        firePropertyChange("defaultCloseOperation", oldValue, operation);
    }

    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
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

    public void setJMenuBar(JMenuBar menu) {
        getRootPane().setJMenuBar(menu);
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
     * It adds to the content, not to the dialog.
     *
     * @throws IllegalArgumentException if the root pane is added with the redirection switched
     *     on.
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

    /** It gives the layout to the content, not to the dialog. */
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

    /**
     * Whether new dialogs draw their own frame.
     *
     * <p>It is a decision of the whole program, not of one dialog: mixing system and Swing frames
     * in the same application looks wrong.
     */
    public static void setDefaultLookAndFeelDecorated(boolean defaultLookAndFeelDecorated) {
        JDialog.defaultLookAndFeelDecorated = defaultLookAndFeelDecorated;
    }

    public static boolean isDefaultLookAndFeelDecorated() {
        return defaultLookAndFeelDecorated;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
