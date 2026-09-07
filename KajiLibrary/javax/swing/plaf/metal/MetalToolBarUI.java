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
 * La barra de herramientas de Metal.
 *
 * <h2>Dos escuchas que a proposito no existen</h2>
 *
 * <p>{@link #createContainerListener} y {@link #createRolloverListener} devuelven {@code null}, y
 * eso esta medido. No es un olvido: el basico usa esos dos escuchas para enterarse de que se agrego
 * un boton y ponerle el borde de relieve. Metal no los necesita porque le pone el borde a cada
 * boton cuando lo dibuja, no cuando lo agregan.
 *
 * <p>Los campos {@link #contListener} y {@link #rolloverListener} quedan por eso en {@code null}
 * despues de instalar, que es lo que contesta el JDK.
 *
 * <h2>El borde es el mismo con relieve y sin el</h2>
 *
 * <p>Los dos {@code createXxxBorder} devuelven un borde compuesto del mismo tipo. El basico usa uno
 * distinto para cada estado; Metal dibuja el relieve adentro del borde segun el modelo del boton,
 * asi que le alcanza con uno.
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

    /** Ninguno; ver la nota de la clase. */
    protected ContainerListener createContainerListener() {
        return null;
    }

    /** Tampoco. */
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

    /** El del basico, que el de Metal envuelve; ver el hallazgo #400. */
    private MouseInputListener arrastreDelBasico() {
        if (arrastreDelBasico == null) {
            arrastreDelBasico = super.createDockingListener();
        }
        return arrastreDelBasico;
    }

    private MouseInputListener arrastreDelBasico;

    protected void setDragOffset(Point p) {
    }

    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }

    /**
     * El que arrastra la barra para sacarla a flotar.
     *
     * <p>Metal lo redefine para una sola cosa: agarrar la barra <em>en cualquier parte</em> y no
     * solo por la manija. El nombre se ve por {@code getClass().getName()} y por eso la clase
     * existe aunque casi no agregue codigo.
     */
    protected class MetalDockingListener implements MouseInputListener {

        public MetalDockingListener(javax.swing.JToolBar t) {
        }

        public void mousePressed(java.awt.event.MouseEvent e) {
            arrastreDelBasico().mousePressed(e);
        }

        public void mouseReleased(java.awt.event.MouseEvent e) {
            arrastreDelBasico().mouseReleased(e);
        }

        public void mouseClicked(java.awt.event.MouseEvent e) {
            arrastreDelBasico().mouseClicked(e);
        }

        public void mouseEntered(java.awt.event.MouseEvent e) {
            arrastreDelBasico().mouseEntered(e);
        }

        public void mouseExited(java.awt.event.MouseEvent e) {
            arrastreDelBasico().mouseExited(e);
        }

        public void mouseDragged(java.awt.event.MouseEvent e) {
            arrastreDelBasico().mouseDragged(e);
        }

        public void mouseMoved(java.awt.event.MouseEvent e) {
            arrastreDelBasico().mouseMoved(e);
        }
    }
}
