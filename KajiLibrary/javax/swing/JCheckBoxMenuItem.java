package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * Un item de menu que ademas se prende y se apaga.
 *
 * <h2>Dos nombres para el mismo estado</h2>
 *
 * <p>{@link #getState} y {@code isSelected} son lo mismo, y {@link #setState} y {@code setSelected}
 * tambien. El par con "state" existe porque un tilde de menu se piensa como prendido o apagado y no
 * como elegido; los dos escriben en el mismo modelo de boton, asi que no se pueden desincronizar.
 *
 * <h2>Se queda prendido solo</h2>
 *
 * <p>A diferencia de un {@link JRadioButtonMenuItem}, nada lo apaga cuando se prende otro: no
 * pertenece a ningun grupo salvo que se lo ponga en uno.
 */
public class JCheckBoxMenuItem extends JMenuItem implements SwingConstants, Accessible {

    private static final String uiClassID = "CheckBoxMenuItemUI";

    /** Sin texto ni icono, apagado. */
    public JCheckBoxMenuItem() {
        this(null, null, false);
    }

    /** Con ese icono, apagado. */
    public JCheckBoxMenuItem(Icon icon) {
        this(null, icon, false);
    }

    /** Con ese texto, apagado. */
    public JCheckBoxMenuItem(String text) {
        this(text, null, false);
    }

    /** Tomando texto, icono y demas de esa accion. */
    public JCheckBoxMenuItem(Action a) {
        this();
        setAction(a);
    }

    /** Con texto e icono, apagado. */
    public JCheckBoxMenuItem(String text, Icon icon) {
        this(text, icon, false);
    }

    /** Con ese texto, prendido o no. */
    public JCheckBoxMenuItem(String text, boolean b) {
        this(text, null, b);
    }

    /** Con texto, icono y estado. */
    public JCheckBoxMenuItem(String text, Icon icon, boolean b) {
        super(text, icon);
        setModel(new JToggleButton.ToggleButtonModel());
        setSelected(b);
        setFocusable(false);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Si esta prendido; ver la nota de la clase. */
    public boolean getState() {
        return isSelected();
    }

    public synchronized void setState(boolean b) {
        setSelected(b);
    }

    /** El texto del item si esta prendido, y nulo si no; es lo que pide {@code ItemSelectable}. */
    public Object[] getSelectedObjects() {
        if (!isSelected()) {
            return null;
        }
        Object[] selectedObjects = new Object[1];
        selectedObjects[0] = getText();
        return selectedObjects;
    }

    protected String paramString() {
        return super.paramString();
    }

    /** Un tilde si toma su estado de la accion; ver {@code AbstractButton}. */
    boolean shouldUpdateSelectedStateFromAction() {
        return true;
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
