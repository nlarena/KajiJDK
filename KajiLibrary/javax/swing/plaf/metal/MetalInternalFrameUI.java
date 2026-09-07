package javax.swing.plaf.metal;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * La ventana interna de Metal.
 *
 * <p>Lo unico propio es el modo paleta. {@link #setPalette} le pasa el aviso a la barra de titulo
 * -- ver {@link MetalInternalFrameTitlePane} -- y le cambia el borde a la ventana, porque una
 * paleta tampoco lleva el marco grueso de cuatro pixeles.
 *
 * <p>La propiedad de cliente {@value #IS_PALETTE} hace lo mismo desde afuera: un programa la pone y
 * el escucha de la barra de titulo se entera. Es la manera de convertir una ventana en paleta sin
 * tener el UI a mano.
 *
 * <p>{@link #IS_PALETTE} es {@code protected static} y <strong>no</strong> {@code final}, que es
 * raro y esta asi en el JDK.
 */
public class MetalInternalFrameUI extends BasicInternalFrameUI {

    /** La propiedad de cliente que convierte la ventana en paleta. */
    protected static String IS_PALETTE = "JInternalFrame.isPalette";

    public MetalInternalFrameUI(JInternalFrame b) {
        super(b);
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalInternalFrameUI((JInternalFrame) c);
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        Object o = c instanceof JInternalFrame
                ? ((JInternalFrame) c).getClientProperty(IS_PALETTE) : null;
        setPalette(Boolean.TRUE.equals(o));
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    protected void installListeners() {
        super.installListeners();
    }

    protected void uninstallListeners() {
        super.uninstallListeners();
    }

    protected void installKeyboardActions() {
        super.installKeyboardActions();
    }

    protected void uninstallKeyboardActions() {
        super.uninstallKeyboardActions();
    }

    protected void uninstallComponents() {
        super.uninstallComponents();
    }

    protected JComponent createNorthPane(JInternalFrame w) {
        titlePane = new MetalInternalFrameTitlePane(w);
        return titlePane;
    }

    protected MouseInputAdapter createBorderListener(JInternalFrame w) {
        return super.createBorderListener(w);
    }

    /** Cambia la ventana entre normal y paleta; ver la nota de la clase. */
    public void setPalette(boolean isPalette) {
        if (titlePane instanceof MetalInternalFrameTitlePane) {
            ((MetalInternalFrameTitlePane) titlePane).setPalette(isPalette);
        }
        if (frame != null) {
            frame.setBorder(isPalette
                    ? MetalBorders.getPaletteBorder()
                    : MetalBorders.getInternalFrameBorder());
        }
    }
}
