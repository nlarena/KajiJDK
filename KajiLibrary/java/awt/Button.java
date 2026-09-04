package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * Un botón: se aprieta y pasa algo.
 *
 * <p>Al apretarlo dispara un {@link ActionEvent} con un **comando**, que es la cadena que identifica
 * qué hay que hacer. Si nadie lo fijó, el comando es la leyenda del botón, y ahí está la trampa
 * clásica: traducir la interfaz a otro idioma cambia la leyenda y, con ella, el comando, así que el
 * `if` que comparaba contra "Aceptar" deja de andar. Por eso conviene fijar el comando a mano con
 * {@link #setActionCommand}.
 */
public class Button extends Component implements Accessible {

    private static final long serialVersionUID = -8774683716313001058L;

    private static int buttonCounter = 0;

    /** La leyenda. */
    String label;

    /** El comando que manda al apretarlo, o `null` para usar la leyenda. */
    String actionCommand;

    /** Los oyentes, encadenados. */
    transient ActionListener actionListener;

    /** Un botón sin leyenda. */
    public Button() throws HeadlessException {
        this("");
    }

    /** Un botón con esa leyenda. */
    public Button(String label) throws HeadlessException {
        this.label = label;
    }

    String constructComponentName() {
        synchronized (Button.class) {
            String n = "button" + buttonCounter;
            buttonCounter = buttonCounter + 1;
            return n;
        }
    }

    /** Lo declara mostrable. */
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

    /**
     * Fija el comando que manda al apretarlo.
     *
     * @param command el comando, o `null` para volver a usar la leyenda
     */
    public void setActionCommand(String command) {
        this.actionCommand = command;
    }

    /**
     * El comando que manda al apretarlo.
     *
     * @return el comando, o la leyenda si no se fijó ninguno
     */
    public String getActionCommand() {
        return this.actionCommand == null ? this.label : this.actionCommand;
    }

    /** Agrega un oyente; `null` no hace nada. */
    public synchronized void addActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.add(this.actionListener, l);
        this.enableEvents(AWTEvent.ACTION_EVENT_MASK);
    }

    /** Saca un oyente. */
    public synchronized void removeActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.remove(this.actionListener, l);
    }

    /** Los oyentes puestos. */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(this.actionListener, ActionListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ActionListener.class) {
            return AWTEventMulticaster.getListeners(this.actionListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            this.processActionEvent((ActionEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /** Les avisa a los oyentes de acción. */
    protected void processActionEvent(ActionEvent e) {
        ActionListener l = this.actionListener;
        if (l != null) {
            l.actionPerformed(e);
        }
    }

    protected String paramString() {
        return super.paramString() + ",label=" + this.label;
    }

    /** La accesibilidad del botón. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTButton();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de un botón.
     *
     * <p>Ofrece **una** acción, apretarlo, y ejecutarla dispara el mismo evento que un clic. Eso es
     * lo que le permite a un lector de pantalla activar el botón sin mouse.
     */
    protected class AccessibleAWTButton extends AccessibleAWTComponent
            implements AccessibleAction {

        /** Para las subclases. */
        protected AccessibleAWTButton() {
        }

        public String getAccessibleName() {
            if (Button.this.getLabel() == null) {
                return super.getAccessibleName();
            }
            return Button.this.getLabel();
        }

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PUSH_BUTTON;
        }

        /** Una sola: apretarlo. */
        public int getAccessibleActionCount() {
            return 1;
        }

        /**
         * Cómo se llama esa acción.
         *
         * @return "click" para la 0, `null` para cualquier otra
         */
        public String getAccessibleActionDescription(int i) {
            if (i == 0) {
                return "click";
            }
            return null;
        }

        /**
         * Aprieta el botón.
         *
         * @return `true` si la acción existía
         */
        public boolean doAccessibleAction(int i) {
            if (i != 0) {
                return false;
            }
            Button.this.processActionEvent(new ActionEvent(Button.this,
                    ActionEvent.ACTION_PERFORMED, Button.this.getActionCommand()));
            return true;
        }
    }
}
