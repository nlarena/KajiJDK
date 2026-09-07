package javax.swing.plaf.metal;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;

/**
 * La barra de titulo de una ventana interna, en Metal.
 *
 * <h2>Dos barras en una clase</h2>
 *
 * <p>{@link #isPalette} decide cual de las dos se dibuja. Una barra normal mide veintitres pixeles
 * de alto y lleva tres botones; una de <strong>paleta</strong> mide once y lleva uno solo, el de
 * cerrar, de siete por siete. Medido.
 *
 * <p>Una paleta es una ventanita de herramientas -- la de colores de un editor de dibujo -- que
 * tiene que estar siempre a mano y ocupar lo menos posible. Achicarle la barra a la mitad es lo
 * que hace que quepa; darle un solo boton es porque una paleta no se maximiza ni se minimiza, se
 * cierra.
 *
 * <p>El cambio es en caliente: {@link #setPalette} rehace los botones y vuelve a armar la barra.
 * Por eso {@link #createButtons} y {@link #addSubComponents} son metodos y no codigo del
 * constructor.
 */
public class MetalInternalFrameTitlePane extends BasicInternalFrameTitlePane {

    /** Once; ver la nota de la clase. */
    protected int paletteTitleHeight = 11;

    protected Icon paletteCloseIcon;
    protected boolean isPalette;

    public MetalInternalFrameTitlePane(JInternalFrame f) {
        super(f);
        paletteCloseIcon = MetalIconFactory.getInternalFrameCloseIcon(7);
    }

    protected void installDefaults() {
        super.installDefaults();
        if (paletteCloseIcon == null) {
            paletteCloseIcon = MetalIconFactory.getInternalFrameCloseIcon(7);
        }
    }

    protected void uninstallDefaults() {
        super.uninstallDefaults();
    }

    public void addNotify() {
        super.addNotify();
    }

    protected void createButtons() {
        super.createButtons();
    }

    /** Como paleta, solo el boton de cerrar; ver la nota de la clase. */
    protected void addSubComponents() {
        if (!isPalette) {
            super.addSubComponents();
            return;
        }
        removeAll();
        if (closeButton != null) {
            closeButton.setIcon(paletteCloseIcon);
            add(closeButton);
        }
    }

    protected LayoutManager createLayout() {
        return new MetalTitlePaneLayout();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new MetalPropertyChangeHandler();
    }

    protected void assembleSystemMenu() {
        if (!isPalette) {
            super.assembleSystemMenu();
        }
    }

    protected void addSystemMenuItems(JMenu systemMenu) {
        super.addSystemMenuItems(systemMenu);
    }

    protected void showSystemMenu() {
        if (!isPalette) {
            super.showSystemMenu();
        }
    }

    /** Cambia entre las dos barras y rehace los hijos. */
    public void setPalette(boolean b) {
        isPalette = b;
        addSubComponents();
        revalidate();
        repaint();
    }

    public void paintComponent(Graphics g) {
        if (isPalette) {
            paintPalette(g);
            return;
        }
        super.paintComponent(g);
    }

    /** La barra de once pixeles: un relleno y una linea, sin titulo. */
    public void paintPalette(Graphics g) {
        Dimension s = getSize();
        g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
        g.fillRect(0, 0, s.width, s.height);
        g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
        g.drawLine(0, s.height - 1, s.width, s.height - 1);
    }

    /** La distribucion de la barra; como paleta, el alto es otro. */
    private class MetalTitlePaneLayout implements LayoutManager {

        public void addLayoutComponent(String name, java.awt.Component c) {
        }

        public void removeLayoutComponent(java.awt.Component c) {
        }

        public Dimension preferredLayoutSize(Container c) {
            Dimension d = laDelBasico().preferredLayoutSize(c);
            if (isPalette) {
                return new Dimension(d.width, paletteTitleHeight);
            }
            return d;
        }

        public Dimension minimumLayoutSize(Container c) {
            return preferredLayoutSize(c);
        }

        public void layoutContainer(Container c) {
            laDelBasico().layoutContainer(c);
        }
    }

    private LayoutManager distribucionDelBasico;

    /**
     * La del basico, guardada aca y no adentro de la clase interna.
     *
     * <p>Por dos razones. Una: el compilador de esta casa todavia no acepta
     * {@code MetalInternalFrameTitlePane.super.createLayout()}; ver el hallazgo #400. La otra, mas
     * de fondo: {@code createLayout} lo llama el constructor de la superclase, asi que un campo de
     * la clase interna inicializado ahi se arma antes de que esta clase termine de construirse.
     * Pedirla cuando hace falta lo evita.
     */
    private LayoutManager laDelBasico() {
        if (distribucionDelBasico == null) {
            distribucionDelBasico = super.createLayout();
        }
        return distribucionDelBasico;
    }

    /** El que escucha los cambios de la ventana. */
    private class MetalPropertyChangeHandler implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            elDelBasico().propertyChange(e);
            if (MetalInternalFrameUI.IS_PALETTE.equals(e.getPropertyName())) {
                setPalette(Boolean.TRUE.equals(e.getNewValue()));
            }
        }
    }

    private PropertyChangeListener escuchaDelBasico;

    /** Igual que {@link #laDelBasico}. */
    private PropertyChangeListener elDelBasico() {
        if (escuchaDelBasico == null) {
            escuchaDelBasico = super.createPropertyChangeListener();
        }
        return escuchaDelBasico;
    }
}
