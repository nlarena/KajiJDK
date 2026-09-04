package javax.swing;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.util.Enumeration;

import javax.accessibility.AccessibleContext;

import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

/**
 * Un boton con dos estados: cada click lo selecciona o lo deselecciona.
 *
 * <p>Toda la diferencia con {@link JButton} esta en el modelo: {@link ToggleButtonModel} cambia
 * la seleccion al soltar, y consulta al {@link ButtonGroup} si hay uno, que es lo que hace que
 * los botones de radio se excluyan. {@link JCheckBox} y {@link JRadioButton} heredan de aca y
 * solo cambian el aspecto.
 *
 * <p>El foco, dentro de un grupo, va al seleccionado: recorrer con Tab un grupo de radios para en
 * el que esta marcado, no en el primero. Es {@link #requestFocus(FocusEvent.Cause)} redirigiendo
 * la peticion cuando la causa es un recorrido o una activacion.
 */
public class JToggleButton extends AbstractButton implements Accessible {

    private static final String uiClassID = "ToggleButtonUI";

    public JToggleButton() {
        this(null, null, false);
    }

    public JToggleButton(Icon icon) {
        this(null, icon, false);
    }

    public JToggleButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public JToggleButton(String text) {
        this(text, null, false);
    }

    public JToggleButton(String text, boolean selected) {
        this(text, null, selected);
    }

    public JToggleButton(Action a) {
        this();
        setAction(a);
    }

    public JToggleButton(String text, Icon icon) {
        this(text, icon, false);
    }

    public JToggleButton(String text, Icon icon, boolean selected) {
        setModel(new ToggleButtonModel());
        model.setSelected(selected);
        init(text, icon);
    }

    /** Instala el aspecto basico; ver {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ButtonUI) BasicToggleButtonUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Si: un boton con estado sigue la seleccion de su accion, y ella la de el. */
    boolean shouldUpdateSelectedStateFromAction() {
        return true;
    }

    /**
     * A quien va el foco pedido con esa causa: al seleccionado del grupo si la causa es un
     * recorrido o una activacion, y a este boton en cualquier otro caso.
     */
    private JToggleButton seleccionDelGrupo(FocusEvent.Cause causa) {
        boolean recorrido = causa == FocusEvent.Cause.ACTIVATION
                || causa == FocusEvent.Cause.TRAVERSAL
                || causa == FocusEvent.Cause.TRAVERSAL_UP
                || causa == FocusEvent.Cause.TRAVERSAL_DOWN
                || causa == FocusEvent.Cause.TRAVERSAL_FORWARD
                || causa == FocusEvent.Cause.TRAVERSAL_BACKWARD;
        if (!recorrido) {
            return this;
        }
        ButtonGroup grupo = getModel().getGroup();
        if (grupo == null) {
            return this;
        }
        ButtonModel seleccion = grupo.getSelection();
        if (seleccion == null || seleccion == getModel()) {
            return this;
        }
        Enumeration<AbstractButton> miembros = grupo.getElements();
        while (miembros.hasMoreElements()) {
            AbstractButton miembro = miembros.nextElement();
            if (miembro instanceof JToggleButton && miembro.getModel() == seleccion) {
                return (JToggleButton) miembro;
            }
        }
        return this;
    }

    public void requestFocus(FocusEvent.Cause cause) {
        seleccionDelGrupo(cause).pedirFocoSinRedirigir(cause);
    }

    private void pedirFocoSinRedirigir(FocusEvent.Cause causa) {
        super.requestFocus(causa);
    }

    public boolean requestFocusInWindow(FocusEvent.Cause cause) {
        return seleccionDelGrupo(cause).pedirFocoEnVentanaSinRedirigir(cause);
    }

    private boolean pedirFocoEnVentanaSinRedirigir(FocusEvent.Cause causa) {
        return super.requestFocusInWindow(causa);
    }

    protected String paramString() {
        return super.paramString();
    }

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva que lo lea en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /**
     * El modelo de un boton con estado: soltar cambia la seleccion, y el grupo manda.
     *
     * <p>{@link #setSelected} pasa primero por el grupo, si hay: es el grupo el que decide que se
     * selecciona y que se deselecciona, y este modelo toma lo que el grupo diga. Sin grupo, se
     * comporta como {@link DefaultButtonModel} salvo por {@link #setPressed}, que al soltar
     * estando armado invierte la seleccion antes de disparar la accion.
     */
    public static class ToggleButtonModel extends DefaultButtonModel {

        public ToggleButtonModel() {
        }

        public boolean isSelected() {
            return (stateMask & SELECTED) != 0;
        }

        public void setSelected(boolean b) {
            ButtonGroup grupo = getGroup();
            if (grupo != null) {
                grupo.setSelected(this, b);
                b = grupo.isSelected(this);
            }
            if (isSelected() == b) {
                return;
            }
            if (b) {
                stateMask = stateMask | SELECTED;
            } else {
                stateMask = stateMask & ~SELECTED;
            }
            fireStateChanged();
            fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                    isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
        }

        public void setPressed(boolean b) {
            if (isPressed() == b || !isEnabled()) {
                return;
            }
            if (!b && isArmed()) {
                setSelected(!isSelected());
            }
            if (b) {
                stateMask = stateMask | PRESSED;
            } else {
                stateMask = stateMask & ~PRESSED;
            }
            fireStateChanged();
            if (!isPressed() && isArmed()) {
                int modificadores = 0;
                AWTEvent actual = EventQueue.getCurrentEvent();
                if (actual instanceof InputEvent) {
                    modificadores = ((InputEvent) actual).getModifiers();
                } else if (actual instanceof ActionEvent) {
                    modificadores = ((ActionEvent) actual).getModifiers();
                }
                fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                        getActionCommand(), EventQueue.getMostRecentEventTime(), modificadores));
            }
        }
    }
}
