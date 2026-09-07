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
 * Una ventana con decoracion, del lado de Swing.
 *
 * <p>Es un {@link Frame} de AWT con un panel raiz adentro; de ahi sale casi toda su API --el panel
 * de contenido, el panel de vidrio, las capas-- y tambien su rareza mas conocida: agregarle un
 * componente directamente no lo agrega al `JFrame` sino a su panel de contenido, porque el `JFrame`
 * mismo no admite mas hijo que el panel raiz.
 *
 * <h2>Los hijos van al contenido</h2>
 *
 * <p>{@link #addImpl} redirige solo, y la redireccion se apaga mientras el constructor arma el panel
 * raiz -- si estuviera prendida, agregar el panel raiz se redirigiria a si mismo. Es exactamente lo
 * mismo que hacen {@link JDialog} y {@link JWindow}.
 *
 * <h2>La politica de cierre</h2>
 *
 * <p>{@link #setDefaultCloseOperation} decide entre esconder, destruir, no hacer nada, o terminar el
 * programa; {@link #processWindowEvent} la aplica al llegar el evento de cierre. Sin reparto de
 * eventos de ventana ese evento no llega solo, pero el metodo esta y hace lo que dice si alguien se
 * lo entrega.
 *
 * @see WindowConstants
 */
public class JFrame extends Frame implements WindowConstants, Accessible, RootPaneContainer,
        TransferHandler.HasGetTransferHandler {

    /** El panel raiz; ver la nota de la clase. */
    protected JRootPane rootPane;

    /** Si agregar redirige al contenido. */
    protected boolean rootPaneCheckingEnabled = false;

    protected AccessibleContext accessibleContext;

    private TransferHandler transferHandler;


    /** Si las ventanas nuevas se decoran con el `LookAndFeel` en vez de con el sistema. */
    private static boolean defaultLookAndFeelDecorated = false;

    /** Que hacer al cerrarla; una de las constantes de {@link WindowConstants}. */
    private int defaultCloseOperation = HIDE_ON_CLOSE;

    /**
     * Una ventana sin titulo, todavia invisible.
     *
     * @throws HeadlessException si el entorno no tiene pantalla
     */
    public JFrame() throws HeadlessException {
        super();
        frameInit();
    }

    /**
     * Una ventana sin titulo en esa configuracion grafica.
     *
     * @param gc la pantalla y el modo, o `null` para los de siempre
     */
    public JFrame(GraphicsConfiguration gc) {
        super(gc);
        frameInit();
    }

    /**
     * Una ventana con ese titulo, todavia invisible.
     *
     * @param title el titulo, o `null` para ninguno
     * @throws HeadlessException si el entorno no tiene pantalla
     */
    public JFrame(String title) throws HeadlessException {
        super(title);
        frameInit();
    }

    /**
     * Una ventana con ese titulo en esa configuracion grafica.
     *
     * @param title el titulo, o `null` para ninguno
     * @param gc la pantalla y el modo, o `null` para los de siempre
     */
    public JFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
        frameInit();
    }

    /**
     * Fija que hacer cuando el usuario la cierre.
     *
     * <p>La aplica {@link #processWindowEvent}; ver la nota de la clase.
     *
     * @param operation una de las constantes de {@link WindowConstants}
     * @throws IllegalArgumentException si no es una de las cuatro
     */
    public void setDefaultCloseOperation(int operation) {
        if (operation != DO_NOTHING_ON_CLOSE && operation != HIDE_ON_CLOSE
                && operation != DISPOSE_ON_CLOSE && operation != EXIT_ON_CLOSE) {
            throw new IllegalArgumentException("defaultCloseOperation must be one of: "
                    + "DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE, DISPOSE_ON_CLOSE, or EXIT_ON_CLOSE");
        }
        this.defaultCloseOperation = operation;
    }

    /** Que se haria al cerrarla. Por omision, {@link WindowConstants#HIDE_ON_CLOSE}. */
    public int getDefaultCloseOperation() {
        return this.defaultCloseOperation;
    }

    /**
     * Si las ventanas creadas de aca en mas se decoran con el `LookAndFeel`.
     *
     * <p>Solo afecta a las que se creen despues: la decoracion se elige al construirlas.
     */
    public static void setDefaultLookAndFeelDecorated(boolean defaultLookAndFeelDecorated) {
        JFrame.defaultLookAndFeelDecorated = defaultLookAndFeelDecorated;
    }

    /** Si las ventanas nuevas se decoran con el `LookAndFeel`. Por omision, no. */
    public static boolean isDefaultLookAndFeelDecorated() {
        return defaultLookAndFeelDecorated;
    }

    // -- el panel raiz --------------------------------------------------------------------------

    /**
     * Arma el panel raiz.
     *
     * <p>La redireccion se prende recien al final; ver la nota de la clase.
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

    /** Atiende el cierre segun {@link #setDefaultCloseOperation}. */
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
     * Dibuja sin borrar el fondo primero.
     *
     * <p>Swing dibuja cada pixel que le toca, asi que borrar antes solo produce un parpadeo.
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
     * Agrega al contenido, no a la ventana.
     *
     * @throws IllegalArgumentException si se intenta agregar el panel raiz con la redireccion
     *     prendida
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

    /** Le pone acomodador al contenido, no a la ventana. */
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

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
