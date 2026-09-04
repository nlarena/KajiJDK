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
 * Una entrada de menú con tilde: se prende y se apaga en vez de ejecutar algo.
 *
 * <p>Es lo que se usa para las opciones de "ver esto sí o no": mostrar la barra de herramientas,
 * ajustar el texto al ancho, y demás. A diferencia de un {@link MenuItem} común, que dispara un
 * {@link java.awt.event.ActionEvent} y se olvida, ésta guarda estado y avisa con un
 * {@link ItemEvent}.
 */
public class CheckboxMenuItem extends MenuItem implements ItemSelectable, Accessible {

    private static final long serialVersionUID = 6190621106981774043L;

    private static int checkboxMenuItemCounter = 0;

    /** Si está tildada. */
    private boolean state;

    /** Los oyentes, encadenados. */
    private transient ItemListener itemListener;

    /** Una entrada sin leyenda y sin tildar. */
    public CheckboxMenuItem() throws HeadlessException {
        this("", false);
    }

    /** Una entrada con esa leyenda, sin tildar. */
    public CheckboxMenuItem(String label) throws HeadlessException {
        this(label, false);
    }

    /** Una entrada con esa leyenda y ese estado. */
    public CheckboxMenuItem(String label, boolean state) throws HeadlessException {
        super(label);
        this.state = state;
    }

    String constructComponentName() {
        synchronized (CheckboxMenuItem.class) {
            String n = "chkmenuitem" + checkboxMenuItemCounter;
            checkboxMenuItemCounter = checkboxMenuItemCounter + 1;
            return n;
        }
    }

    /** La declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Si está tildada. */
    public boolean getState() {
        return this.state;
    }

    /** La tilda o la destilda; no dispara ningún evento. */
    public synchronized void setState(boolean b) {
        this.state = b;
    }

    /**
     * Lo que está seleccionado.
     *
     * @return un arreglo con la leyenda si está tildada, o `null` si no
     */
    public synchronized Object[] getSelectedObjects() {
        if (!this.state) {
            return null;
        }
        Object[] items = new Object[1];
        items[0] = this.getLabel();
        return items;
    }

    /** Agrega un oyente; `null` no hace nada. */
    public synchronized void addItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.add(this.itemListener, l);
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

    /**
     * Atiende la elección de la entrada por parte del usuario.
     *
     * <p>Acá sí se da vuelta el estado **y** se avisa, porque esto viene de una acción del usuario,
     * que es exactamente lo que {@link #setState} no es.
     */
    void doMenuEvent(long when, int modifiers) {
        this.setState(!this.state);
        this.processItemEvent(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this.getLabel(),
                this.state ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
    }

    public String paramString() {
        return super.paramString() + ",state=" + this.state;
    }

    /** La accesibilidad de la entrada. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTCheckboxMenuItem();
        }
        return this.accessibleContext;
    }

    /** La accesibilidad de una entrada de menú con tilde. */
    protected class AccessibleAWTCheckboxMenuItem extends AccessibleAWTMenuItem
            implements AccessibleAction, AccessibleValue {

        /** Para las subclases. */
        protected AccessibleAWTCheckboxMenuItem() {
        }

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (CheckboxMenuItem.this.getState()) {
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
            CheckboxMenuItem.this.doMenuEvent(0, 0);
            return true;
        }

        /** 1 si está tildada, 0 si no. */
        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(CheckboxMenuItem.this.getState() ? 1 : 0);
        }

        public boolean setCurrentAccessibleValue(Number n) {
            if (n == null) {
                return false;
            }
            CheckboxMenuItem.this.setState(n.intValue() != 0);
            return true;
        }

        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(0);
        }

        public Number getMaximumAccessibleValue() {
            return Integer.valueOf(1);
        }
    }
}
