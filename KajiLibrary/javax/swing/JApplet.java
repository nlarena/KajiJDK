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
 * Un applet de Swing.
 *
 * <h2>Historia, no tecnologia</h2>
 *
 * <p>Los applets ya no corren en ningun navegador: el complemento de Java se retiro y
 * {@link Applet} esta marcado obsoleto en el JDK. Esta clase existe porque su superclase existe, y
 * porque codigo viejo la nombra.
 *
 * <p>Lo unico que agrega sobre {@code Applet} es lo mismo que {@link JWindow} agrega sobre
 * {@code Window}: un {@link JRootPane} adentro, con panel de contenido, panel por capas, panel de
 * vidrio y barra de menu. Todo eso sirve igual afuera de un navegador -- un {@code JApplet} se puede
 * meter adentro de un panel como cualquier componente --, que es la unica razon por la que hoy
 * alguien la tocaria.
 *
 * <p>Los hijos van al contenido; ver la nota de {@link JDialog}.
 */
public class JApplet extends Applet implements Accessible, RootPaneContainer,
        TransferHandler.HasGetTransferHandler {

    /** El panel raiz; ver la nota de la clase. */
    protected JRootPane rootPane;

    /** Si agregar redirige al contenido. */
    protected boolean rootPaneCheckingEnabled = false;

    protected AccessibleContext accessibleContext;

    private TransferHandler transferHandler;

    /**
     * Un applet con su panel raiz.
     *
     * @throws HeadlessException si no hay pantalla
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
     * Dibuja sin borrar el fondo primero.
     *
     * <p>Swing dibuja cada pixel que le toca, asi que borrar antes solo produce un parpadeo.
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
     * Agrega al contenido, no al applet.
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

    /** Le pone acomodador al contenido, no al applet. */
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
