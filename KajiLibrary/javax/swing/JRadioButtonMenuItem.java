package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * Un item de menu que se elige entre varios.
 *
 * <h2>El grupo no viene puesto</h2>
 *
 * <p>La clase sola no apaga a nadie: para que elegir uno apague al anterior hay que meterlos a
 * todos en un {@link ButtonGroup}. Sin grupo se comporta igual que un {@link JCheckBoxMenuItem},
 * solo que se dibuja redondo. Es el error mas comun con esta clase y no da ningun aviso.
 */
public class JRadioButtonMenuItem extends JMenuItem implements Accessible {

    private static final String uiClassID = "RadioButtonMenuItemUI";

    /** Sin texto ni icono, sin elegir. */
    public JRadioButtonMenuItem() {
        this(null, null, false);
    }

    /** Con ese icono. */
    public JRadioButtonMenuItem(Icon icon) {
        this(null, icon, false);
    }

    /** Con ese texto. */
    public JRadioButtonMenuItem(String text) {
        this(text, null, false);
    }

    /** Tomando texto, icono y demas de esa accion. */
    public JRadioButtonMenuItem(Action a) {
        this();
        setAction(a);
    }

    /** Con texto e icono. */
    public JRadioButtonMenuItem(String text, Icon icon) {
        this(text, icon, false);
    }

    /** Con ese texto, elegido o no. */
    public JRadioButtonMenuItem(String text, boolean selected) {
        this(text);
        setSelected(selected);
    }

    /** Con ese icono, elegido o no. */
    public JRadioButtonMenuItem(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    /** Con texto, icono y estado. */
    public JRadioButtonMenuItem(String text, Icon icon, boolean selected) {
        super(text, icon);
        setModel(new JToggleButton.ToggleButtonModel());
        setSelected(selected);
        setFocusable(false);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    protected String paramString() {
        return super.paramString();
    }

    /** Toma su estado de la accion; ver {@code AbstractButton}. */
    boolean shouldUpdateSelectedStateFromAction() {
        return true;
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
