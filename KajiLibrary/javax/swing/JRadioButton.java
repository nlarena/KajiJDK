package javax.swing;

import javax.accessibility.AccessibleContext;

import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;

/**
 * Un boton de radio: un {@link JToggleButton} con el circulo del aspecto, pensado para vivir en
 * un {@link ButtonGroup}.
 *
 * <p>La exclusion no esta aca sino en el grupo y en {@code ToggleButtonModel}: un radio suelto
 * se marca y desmarca como una casilla. Como la casilla, ignora el icono de su {@link Action}.
 */
public class JRadioButton extends JToggleButton implements Accessible {

    private static final String uiClassID = "RadioButtonUI";

    public JRadioButton() {
        this(null, null, false);
    }

    public JRadioButton(Icon icon) {
        this(null, icon, false);
    }

    public JRadioButton(Action a) {
        this();
        setAction(a);
    }

    public JRadioButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public JRadioButton(String text) {
        this(text, null, false);
    }

    public JRadioButton(String text, boolean selected) {
        this(text, null, selected);
    }

    public JRadioButton(String text, Icon icon) {
        this(text, icon, false);
    }

    public JRadioButton(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setBorderPainted(false);
        setHorizontalAlignment(LEADING);
    }

    /** Instala el aspecto basico; ver {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ButtonUI) BasicRadioButtonUI.createUI(this));
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
