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
 * Una ventana sin marco ni barra de titulo.
 *
 * <h2>Para que sirve una ventana que no se puede mover ni cerrar</h2>
 *
 * <p>Para lo que se dibuja <em>encima</em> de todo y desaparece solo: un menu desplegable que se sale
 * del borde de la aplicacion, un cartel de ayuda, una pantalla de bienvenida. Todo eso necesita una
 * ventana del sistema -- si no, no puede salirse de su aplicacion -- pero no necesita ninguno de los
 * adornos.
 *
 * <p>Como no tiene barra de titulo, tampoco tiene boton de cerrar, y por lo tanto <strong>no puede
 * recibir el foco del teclado por si sola</strong>. Quien la abre tiene que acordarse de cerrarla.
 *
 * <h2>Los hijos van al contenido</h2>
 *
 * <p>Como en {@link JDialog}, adentro hay un {@link JRootPane} y {@code add} redirige a su panel de
 * contenido; ver la nota de esa clase, que explica tambien por que la redireccion se apaga mientras
 * se arma el panel raiz.
 */
public class JWindow extends Window implements Accessible, RootPaneContainer,
        TransferHandler.HasGetTransferHandler {

    /** El panel raiz; ver la nota de la clase. */
    protected JRootPane rootPane;

    /** Si agregar redirige al contenido. */
    protected boolean rootPaneCheckingEnabled = false;

    protected AccessibleContext accessibleContext;

    private TransferHandler transferHandler;

    /**
     * Una ventana sin dueno.
     *
     * <p>Cuelga de una ventana compartida y escondida: una ventana del sistema necesita alguna
     * ventana de la que depender.
     *
     * @throws java.awt.HeadlessException si no hay pantalla
     */
    public JWindow() {
        this((Frame) null);
    }

    /**
     * En esa configuracion de pantalla.
     *
     * @throws java.awt.HeadlessException si no hay pantalla
     */
    public JWindow(GraphicsConfiguration gc) {
        this(null, gc);
        super.setFocusableWindowState(false);
    }

    /**
     * Con esa ventana de dueno.
     *
     * @throws java.awt.HeadlessException si no hay pantalla
     */
    public JWindow(Frame owner) {
        super(owner == null ? SwingUtilities.getSharedOwnerFrame() : owner);
        if (owner == null) {
            super.setFocusableWindowState(false);
        }
        windowInit();
    }

    /**
     * Con esa ventana de dueno.
     *
     * @throws java.awt.HeadlessException si no hay pantalla
     */
    public JWindow(Window owner) {
        super(owner == null ? (Window) SwingUtilities.getSharedOwnerFrame() : owner);
        if (owner == null) {
            super.setFocusableWindowState(false);
        }
        windowInit();
    }

    /**
     * Con dueno y configuracion de pantalla.
     *
     * @throws java.awt.HeadlessException si no hay pantalla
     */
    public JWindow(Window owner, GraphicsConfiguration gc) {
        super(owner == null ? (Window) SwingUtilities.getSharedOwnerFrame() : owner, gc);
        if (owner == null) {
            super.setFocusableWindowState(false);
        }
        windowInit();
    }

    /** Arma el panel raiz; la redireccion se prende recien al final. */
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
     * Dibuja sin borrar el fondo primero.
     *
     * <p>Swing dibuja cada pixel que le toca, asi que borrar antes solo produce un parpadeo.
     */
    public void update(Graphics g) {
        paint(g);
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
                // Apagado mientras se agrega el panel raiz: si no, se redirigiria a si mismo.
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
