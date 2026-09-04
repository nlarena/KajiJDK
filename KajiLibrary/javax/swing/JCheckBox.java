package javax.swing;

import javax.accessibility.AccessibleContext;

import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicCheckBoxUI;

/**
 * Una casilla: un {@link JToggleButton} cuyo icono es el cuadrado con tilde del aspecto.
 *
 * <p>No agrega estado, salvo {@link #setBorderPaintedFlat}, que algunos aspectos usan para
 * dibujar la casilla sin relieve. El texto va a la derecha del icono y todo alineado al
 * principio, que es la diferencia visible con un boton: una casilla no centra.
 *
 * <p>El icono de una {@link Action} se ignora a proposito: una casilla muestra siempre su
 * cuadrado, y el icono de la accion es para botones y menus.
 */
public class JCheckBox extends JToggleButton implements Accessible {

    public static final String BORDER_PAINTED_FLAT_CHANGED_PROPERTY = "flat";

    private static final String uiClassID = "CheckBoxUI";

    private boolean flat = false;

    public JCheckBox() {
        this(null, null, false);
    }

    public JCheckBox(Icon icon) {
        this(null, icon, false);
    }

    public JCheckBox(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public JCheckBox(String text) {
        this(text, null, false);
    }

    public JCheckBox(Action a) {
        this();
        setAction(a);
    }

    public JCheckBox(String text, boolean selected) {
        this(text, null, selected);
    }

    public JCheckBox(String text, Icon icon) {
        this(text, icon, false);
    }

    public JCheckBox(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setUIProperty("borderPainted", Boolean.FALSE);
        setHorizontalAlignment(LEADING);
    }

    /** Si el aspecto debe dibujar la casilla plana; el basico no la dibuja distinto. */
    public void setBorderPaintedFlat(boolean b) {
        boolean viejo = flat;
        flat = b;
        firePropertyChange(BORDER_PAINTED_FLAT_CHANGED_PROPERTY, viejo, flat);
        if (b != viejo) {
            revalidate();
            repaint();
        }
    }

    public boolean isBorderPaintedFlat() {
        return flat;
    }

    /** Instala el aspecto basico; ver {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ButtonUI) BasicCheckBoxUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Nada: ver la nota de la clase. */
    void setIconFromAction(Action a) {
    }

    protected String paramString() {
        return super.paramString();
    }

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva que lo lea en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
