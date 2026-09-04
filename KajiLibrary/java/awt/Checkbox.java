package java.awt;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;

/**
 * Una casilla con dos estados: marcada o no.
 *
 * <p>Sola es una casilla de verificación. Metida en un {@link CheckboxGroup} se transforma en un
 * botón de radio, porque el grupo se encarga de desmarcar a las hermanas. Es el mismo widget
 * haciendo dos trabajos distintos, que es una de las rarezas de AWT.
 *
 * <p>Cambiar el estado con {@link #setState} **no dispara** ningún evento. Los eventos son de la
 * interacción del usuario, no del programa: si un `setState` avisara, el oyente que responde
 * poniendo otra casilla armaría una cascada.
 */
public class Checkbox extends Component implements ItemSelectable, Accessible {

    private static final long serialVersionUID = 7270714317450821763L;

    private static int checkboxCounter = 0;

    /** La leyenda. */
    String label;

    /** Si está marcada. */
    boolean state;

    /** El grupo al que pertenece, o `null` si es una casilla suelta. */
    CheckboxGroup group;

    /** Los oyentes, encadenados. */
    transient ItemListener itemListener;

    /**
     * Cambia el estado sin pasar por el grupo.
     *
     * <p>Es lo que usa {@link CheckboxGroup} para desmarcar a la hermana: si llamara a
     * {@link #setState} volvería a entrar al grupo y se quedaría dando vueltas.
     */
    void setStateInternal(boolean state) {
        this.state = state;
    }

    /** Una casilla sin leyenda, sin marcar y sin grupo. */
    public Checkbox() throws HeadlessException {
        this("", false, null);
    }

    /** Una casilla con esa leyenda, sin marcar. */
    public Checkbox(String label) throws HeadlessException {
        this(label, false, null);
    }

    /** Una casilla con esa leyenda y ese estado. */
    public Checkbox(String label, boolean state) throws HeadlessException {
        this(label, state, null);
    }

    /** Una casilla con esa leyenda y ese estado, dentro de ese grupo. */
    public Checkbox(String label, boolean state, CheckboxGroup group) throws HeadlessException {
        this.label = label;
        this.state = state;
        this.group = group;
        if (state && group != null) {
            group.setSelectedCheckbox(this);
        }
    }

    /**
     * Lo mismo, con los dos últimos argumentos al revés.
     *
     * <p>Los dos constructores existen porque nadie se acuerda de cuál va primero.
     */
    public Checkbox(String label, CheckboxGroup group, boolean state) throws HeadlessException {
        this(label, state, group);
    }

    String constructComponentName() {
        synchronized (Checkbox.class) {
            String n = "checkbox" + checkboxCounter;
            checkboxCounter = checkboxCounter + 1;
            return n;
        }
    }

    /** La declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * La leyenda.
     *
     * @return la leyenda, o `null` si no tiene
     */
    public String getLabel() {
        return this.label;
    }

    /** Cambia la leyenda. */
    public void setLabel(String label) {
        boolean cambio;
        synchronized (this) {
            cambio = label != this.label && (this.label == null || !this.label.equals(label));
            if (cambio) {
                this.label = label;
            }
        }
        if (cambio) {
            this.invalidate();
        }
    }

    /** Si está marcada. */
    public boolean getState() {
        return this.state;
    }

    /**
     * La marca o la desmarca.
     *
     * <p>Desmarcar la casilla **marcada de un grupo no hace nada**: el grupo no puede quedar vacío
     * desde acá, igual que no puede quedar vacío desde la interfaz. La única forma de vaciarlo es
     * {@link CheckboxGroup#setSelectedCheckbox CheckboxGroup.setSelectedCheckbox(null)}, que es el
     * método del grupo y no el de la casilla.
     */
    public void setState(boolean state) {
        CheckboxGroup g = this.group;
        if (g != null) {
            if (state) {
                g.setSelectedCheckbox(this);
                return;
            }
            if (g.getSelectedCheckbox() == this) {
                state = true;
            }
        }
        this.setStateInternal(state);
    }

    /**
     * Lo que está seleccionado.
     *
     * @return un arreglo con la leyenda si está marcada, o `null` si no
     */
    public Object[] getSelectedObjects() {
        if (!this.state) {
            return null;
        }
        Object[] items = new Object[1];
        items[0] = this.label;
        return items;
    }

    /**
     * El grupo al que pertenece.
     *
     * @return el grupo, o `null` si es una casilla suelta
     */
    public CheckboxGroup getCheckboxGroup() {
        return this.group;
    }

    /**
     * La cambia de grupo.
     *
     * <p>Al entrar a un grupo la casilla se **desmarca**, salvo que el grupo no tuviera nada marcado.
     * Si no, entrar rompería la única regla del grupo.
     */
    public void setCheckboxGroup(CheckboxGroup g) {
        CheckboxGroup anterior;
        synchronized (this) {
            anterior = this.group;
            if (anterior == g) {
                return;
            }
            this.group = g;
        }
        if (anterior != null && anterior.getSelectedCheckbox() == this) {
            anterior.setSelectedCheckbox(null);
        }
        if (g != null) {
            if (g.getSelectedCheckbox() != null) {
                this.state = false;
            } else if (this.state) {
                g.setSelectedCheckbox(this);
            }
        }
    }

    /** Agrega un oyente; `null` no hace nada. */
    public synchronized void addItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.add(this.itemListener, l);
        this.enableEvents(AWTEvent.ITEM_EVENT_MASK);
    }

    /** Saca un oyente. */
    public synchronized void removeItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.remove(this.itemListener, l);
    }

    /** Los oyentes puestos. */
    public synchronized ItemListener[] getItemListeners() {
        return AWTEventMulticaster.getListeners(this.itemListener, ItemListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ItemListener.class) {
            return AWTEventMulticaster.getListeners(this.itemListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof ItemEvent) {
            this.processItemEvent((ItemEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /** Les avisa a los oyentes de selección. */
    protected void processItemEvent(ItemEvent e) {
        ItemListener l = this.itemListener;
        if (l != null) {
            l.itemStateChanged(e);
        }
    }

    protected String paramString() {
        String s = super.paramString() + ",label=" + this.label + ",state=" + this.state;
        if (this.group != null) {
            s = s + ",group=" + this.group;
        }
        return s;
    }

    /** La accesibilidad de la casilla. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTCheckbox();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de una casilla.
     *
     * <p>Informa el estado `CHECKED` y ofrece la acción de marcarla, que es lo que un lector de
     * pantalla necesita para operarla. El valor accesible es 1 marcada y 0 sin marcar.
     */
    protected class AccessibleAWTCheckbox extends AccessibleAWTComponent
            implements AccessibleAction, AccessibleValue {

        /** Para las subclases. */
        protected AccessibleAWTCheckbox() {
        }

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public String getAccessibleName() {
            if (Checkbox.this.getLabel() == null) {
                return super.getAccessibleName();
            }
            return Checkbox.this.getLabel();
        }

        /**
         * Una casilla, tenga grupo o no.
         *
         * <p>Sería tentador informar `RADIO_BUTTON` cuando está en un grupo, porque es lo que
         * parece. El JDK informa `CHECK_BOX` siempre, y se comprobó: cambiarlo haría que un lector
         * de pantalla anunciara distinto que con AWT de verdad.
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (Checkbox.this.getState()) {
                s.add(AccessibleState.CHECKED);
            }
            return s;
        }

        /** Una sola: darla vuelta. */
        public int getAccessibleActionCount() {
            return 1;
        }

        public String getAccessibleActionDescription(int i) {
            if (i == 0) {
                return "toggle";
            }
            return null;
        }

        public boolean doAccessibleAction(int i) {
            if (i != 0) {
                return false;
            }
            Checkbox.this.setState(!Checkbox.this.getState());
            return true;
        }

        /** 1 si está marcada, 0 si no. */
        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(Checkbox.this.getState() ? 1 : 0);
        }

        /**
         * La marca si el valor no es cero.
         *
         * @return `true` si el valor no era `null`
         */
        public boolean setCurrentAccessibleValue(Number n) {
            if (n == null) {
                return false;
            }
            Checkbox.this.setState(n.intValue() != 0);
            return true;
        }

        /** Cero: sin marcar. */
        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(0);
        }

        /** Uno: marcada. */
        public Number getMaximumAccessibleValue() {
            return Integer.valueOf(1);
        }
    }
}
