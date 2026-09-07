package javax.swing.plaf.basic;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.LookAndFeel;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopIconUI;

/**
 * El aspecto basico de una ventana interna hecha icono.
 *
 * <h2>El icono es una barra de titulo sin ventana</h2>
 *
 * <p>{@link #iconPane} <em>es</em> un {@link BasicInternalFrameTitlePane}. No se le parece: es uno.
 * Eso explica por que un icono de escritorio muestra el titulo y los botones de restaurar y cerrar,
 * y por que mide lo que mide -- lo que mida esa barra --.
 *
 * <p>La consecuencia practica es que esta clase no se puede escribir antes que la barra de titulo, y
 * es por eso que quedo para el final del paquete.
 *
 * <h2>Doble click restaura</h2>
 *
 * <p>{@link #deiconize} es la operacion, y el escucha de mouse la dispara con dos clicks. Un click
 * solo elige la ventana sin restaurarla, que es lo que deja arrastrar el icono a otro lugar del
 * escritorio.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Arrastrar el icono por el escritorio necesita el administrador de escritorio y un mouse; el
 * escucha esta y no arrastra.
 */
public class BasicDesktopIconUI extends DesktopIconUI {

    protected JInternalFrame.JDesktopIcon desktopIcon;
    protected JInternalFrame frame;

    /** La barra de titulo que se ve como icono; ver la nota de la clase. */
    protected JComponent iconPane;

    private MouseInputListener mouseInputListener;

    public BasicDesktopIconUI() {
    }

    /** Uno nuevo por icono: guarda la barra que muestra. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicDesktopIconUI();
    }

    public void installUI(JComponent c) {
        desktopIcon = (JInternalFrame.JDesktopIcon) c;
        frame = desktopIcon.getInternalFrame();
        installDefaults();
        installComponents();
        installListeners();
        javax.swing.JLayeredPane.putLayer(desktopIcon, javax.swing.JLayeredPane.PALETTE_LAYER);
    }

    public void uninstallUI(JComponent c) {
        uninstallComponents();
        uninstallListeners();
        uninstallDefaults();
        frame = null;
        desktopIcon = null;
    }

    /** Colores, acomodador y opacidad. */
    protected void installDefaults() {
        desktopIcon.setLayout(new BorderLayout());
        LookAndFeel.installProperty(desktopIcon, "opaque", Boolean.TRUE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
        desktopIcon.setLayout(null);
    }

    /** Arma la barra de titulo y la pone en el centro; ver la nota de la clase. */
    protected void installComponents() {
        iconPane = new BasicInternalFrameTitlePane(frame);
        desktopIcon.setLayout(new BorderLayout());
        desktopIcon.add(iconPane, BorderLayout.CENTER);
    }

    protected void uninstallComponents() {
        if (iconPane != null) {
            desktopIcon.remove(iconPane);
        }
        desktopIcon.setLayout(null);
        iconPane = null;
    }

    protected void installListeners() {
        mouseInputListener = createMouseInputListener();
        desktopIcon.addMouseListener(mouseInputListener);
        desktopIcon.addMouseMotionListener(mouseInputListener);
    }

    protected void uninstallListeners() {
        desktopIcon.removeMouseListener(mouseInputListener);
        desktopIcon.removeMouseMotionListener(mouseInputListener);
        mouseInputListener = null;
    }

    protected MouseInputListener createMouseInputListener() {
        return new Handler(this);
    }

    /** Devuelve la ventana a su tamano; ver la nota de la clase. */
    public void deiconize() {
        try {
            frame.setIcon(false);
        } catch (PropertyVetoException ex) {
            // Alguien dijo que no. Es una respuesta valida, no un error.
        }
    }

    /** El de la barra de titulo mas los margenes. */
    public Dimension getPreferredSize(JComponent c) {
        return desktopIcon.getLayout().preferredLayoutSize(desktopIcon);
    }

    /** Idem con el minimo. */
    public Dimension getMinimumSize(JComponent c) {
        return desktopIcon.getLayout().minimumLayoutSize(desktopIcon);
    }

    /** Sin tope. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /** Cuatro pixeles de aire alrededor. */
    public Insets getInsets(JComponent c) {
        JInternalFrame f = desktopIcon.getInternalFrame();
        javax.swing.border.Border border = f.getBorder();
        if (border != null) {
            return border.getBorderInsets(f);
        }
        return new Insets(0, 0, 0, 0);
    }

    /** Dos clicks restauran la ventana; ver la nota de la clase. */
    private static class Handler extends MouseInputAdapter implements MouseInputListener {

        private final BasicDesktopIconUI ui;

        Handler(BasicDesktopIconUI ui) {
            this.ui = ui;
        }

        public void mousePressed(MouseEvent e) {
            if (e.getClickCount() > 1 && ui.frame.isIconifiable()) {
                ui.deiconize();
                return;
            }
            try {
                ui.frame.setSelected(true);
            } catch (PropertyVetoException ex) {
                // Alguien dijo que no.
            }
        }
    }
}
