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
 * Una ventana secundaria de Swing.
 *
 * <h2>Los hijos van al contenido, no al dialogo</h2>
 *
 * <p>Un {@code JDialog} tiene adentro un {@link JRootPane}, y todo lo que se agregue va a su panel
 * de contenido. {@link #addImpl} lo redirige solo: {@code dialogo.add(boton)} en realidad agrega al
 * contenido.
 *
 * <p>Esa redireccion se puede apagar con {@link #setRootPaneCheckingEnabled}, y es lo que hace el
 * constructor mientras arma el panel raiz: si estuviera prendida, agregar el panel raiz se
 * redirigiria a si mismo.
 *
 * <h2>Que hacer al cerrar</h2>
 *
 * <p>{@link #setDefaultCloseOperation} decide entre esconder, destruir, no hacer nada, o terminar
 * el programa. Por omision esconde, que es lo correcto para un dialogo que se va a volver a abrir;
 * destruir libera la ventana del sistema y obliga a rearmarla.
 *
 * <h2>Sin pantalla</h2>
 *
 * <p>Un dialogo necesita una ventana del sistema. Sin pantalla se puede construir y configurar --
 * y eso es lo que hace falta para que compile lo que lo usa -- pero no mostrarse.
 */
public class JDialog extends Dialog implements WindowConstants, Accessible,
        RootPaneContainer, TransferHandler$HasGetTransferHandler {

    private static boolean defaultLookAndFeelDecorated = false;

    /** El panel raiz; ver la nota de la clase. */
    protected JRootPane rootPane;

    /** Si agregar redirige al contenido. */
    protected boolean rootPaneCheckingEnabled = false;

    protected AccessibleContext accessibleContext;

    private int defaultCloseOperation = HIDE_ON_CLOSE;
    private TransferHandler transferHandler;

    /** Un dialogo sin dueno y no modal. */
    public JDialog() {
        this((Frame) null, false);
    }

    /** Un dialogo de esa ventana, no modal. */
    public JDialog(Frame owner) {
        this(owner, false);
    }

    /** Un dialogo de esa ventana. */
    public JDialog(Frame owner, boolean modal) {
        this(owner, null, modal);
    }

    /** Un dialogo con ese titulo, no modal. */
    public JDialog(Frame owner, String title) {
        this(owner, title, false);
    }

    /** Un dialogo con ese titulo. */
    public JDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        dialogInit();
    }

    /** Un dialogo en esa configuracion de pantalla. */
    public JDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        dialogInit();
    }

    /** Un dialogo de otro dialogo, no modal. */
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

    /** Un dialogo de esa ventana, no modal. */
    public JDialog(Window owner) {
        this(owner, Dialog$ModalityType.MODELESS);
    }

    /** Un dialogo con ese tipo de modalidad. */
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
     * Arma el panel raiz.
     *
     * <p>La redireccion se prende recien al final; ver la nota de la clase.
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

    /** Atiende el cierre segun {@link #setDefaultCloseOperation}. */
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
     * Que hacer cuando el usuario cierra el dialogo.
     *
     * @throws IllegalArgumentException si no es una de las cuatro.
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
     * Dibuja sin borrar el fondo primero.
     *
     * <p>Swing dibuja cada pixel que le toca, asi que borrar antes solo produce un parpadeo.
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
     * Agrega al contenido, no al dialogo.
     *
     * @throws IllegalArgumentException si se intenta agregar el panel raiz con la redireccion
     *     prendida.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().add(comp, constraints, index);
        } else {
            super.addImpl(comp, constraints, index);
        }
    }

    /** Saca del contenido, salvo que sea el panel raiz. */
    public void remove(Component comp) {
        if (comp == rootPane) {
            super.remove(comp);
        } else {
            getContentPane().remove(comp);
        }
    }

    /** Le pone acomodador al contenido, no al dialogo. */
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
                // Apagado mientras se agrega el panel raiz; ver la nota de la clase.
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
     * Si los dialogos nuevos dibujan su propio marco.
     *
     * <p>Es una decision del programa entero, no de un dialogo: mezclar marcos del sistema y de
     * Swing en la misma aplicacion se ve mal.
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
