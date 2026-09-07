package javax.swing.plaf.metal;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicDesktopIconUI;

/**
 * El icono de una ventana interna minimizada, en Metal.
 *
 * <p>Metal no lo dibuja como un icono: lo dibuja como una <strong>ventana chiquita</strong>, con su
 * barra de titulo y su boton de restaurar. Por eso el ancho preferido es fijo -- 160 pixeles -- y
 * no depende del titulo: si dependiera, un escritorio con varias ventanas minimizadas tendria
 * botones de distinto largo y no se leerian como una fila.
 *
 * <p>Los tres tamanos son el mismo. Un icono de escritorio no se estira.
 */
public class MetalDesktopIconUI extends BasicDesktopIconUI {

    /** El ancho fijo; ver la nota de la clase. */
    private static final int ANCHO = 160;

    public MetalDesktopIconUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalDesktopIconUI();
    }

    /** Los del basico; Metal no cambia ninguno, y el icono queda opaco. Medido. */
    protected void installDefaults() {
        super.installDefaults();
    }

    protected void installComponents() {
        super.installComponents();
    }

    protected void uninstallComponents() {
        super.uninstallComponents();
    }

    protected void installListeners() {
        super.installListeners();
    }

    protected void uninstallListeners() {
        super.uninstallListeners();
    }

    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(ANCHO, alto());
    }

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** El de la barra de titulo que lleva adentro. */
    private int alto() {
        if (iconPane != null) {
            return iconPane.getPreferredSize().height;
        }
        return 0;
    }
}
