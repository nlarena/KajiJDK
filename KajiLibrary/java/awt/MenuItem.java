package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Una opción de menú: una línea que se puede elegir.
 *
 * <p>Cuando se elige dispara un {@link ActionEvent}, igual que un botón, y por la misma razón: para
 * quien escucha, "el usuario pidió guardar" es lo mismo venga del menú, del botón de la barra o del
 * atajo de teclado. Ese es el sentido de que la acción sea el evento de más alto nivel de AWT.
 *
 * <p>El **comando** identifica qué acción es, y si no se le pone uno se usa la etiqueta. Eso último
 * es una trampa conocida: al traducir la interfaz cambia la etiqueta y con ella el comando, y el
 * código que comparaba contra el texto en inglés deja de funcionar. Por eso conviene ponerlo
 * explícito.
 */
public class MenuItem extends MenuComponent implements Accessible {

    private static final long serialVersionUID = -21757335363267194L;

    private String label;
    private boolean enabled = true;
    private MenuShortcut shortcut;
    private String actionCommand;
    private transient ActionListener actionListener;

    /** Qué familias de eventos pidió recibir. */
    long eventMask;

    /**
     * Una opción sin etiqueta.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public MenuItem() throws HeadlessException {
        this("", null);
    }

    /**
     * Con esa etiqueta.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public MenuItem(String label) throws HeadlessException {
        this(label, null);
    }

    /**
     * Con etiqueta y atajo de teclado.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public MenuItem(String label, MenuShortcut s) throws HeadlessException {
        this.label = label;
        this.shortcut = s;
    }

    /** Avisa que puede mostrarse. */
    public void addNotify() {
    }

    /** El texto de la opción. */
    public String getLabel() {
        return this.label;
    }

    /** Cambia el texto. */
    public synchronized void setLabel(String label) {
        this.label = label;
    }

    /** Si se puede elegir. */
    public boolean isEnabled() {
        return this.enabled;
    }

    /** La habilita o la deshabilita. */
    public synchronized void setEnabled(boolean b) {
        this.enabled = b;
    }

    /**
     * La habilita.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setEnabled}.
     */
    @Deprecated
    public synchronized void enable() {
        this.setEnabled(true);
    }

    /**
     * La habilita o la deshabilita.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setEnabled}.
     */
    @Deprecated
    public void enable(boolean b) {
        this.setEnabled(b);
    }

    /**
     * La deshabilita.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setEnabled}.
     */
    @Deprecated
    public synchronized void disable() {
        this.setEnabled(false);
    }

    /** El atajo de teclado, o `null`. */
    public MenuShortcut getShortcut() {
        return this.shortcut;
    }

    /** Le pone atajo de teclado. */
    public void setShortcut(MenuShortcut s) {
        this.shortcut = s;
    }

    /** Le saca el atajo. */
    public void deleteShortcut() {
        this.shortcut = null;
    }

    /**
     * Pide recibir esas familias de eventos.
     *
     * <p>Es la contrapartida de registrar un oyente: sin la máscara prendida, el evento no se
     * entrega aunque haya quien lo escuche.
     */
    protected final void enableEvents(long eventsToEnable) {
        this.eventMask = this.eventMask | eventsToEnable;
    }

    /** Deja de recibirlas. */
    protected final void disableEvents(long eventsToDisable) {
        this.eventMask = this.eventMask & ~eventsToDisable;
    }

    /** Cambia qué acción identifica esta opción. */
    public void setActionCommand(String command) {
        this.actionCommand = command;
    }

    /**
     * Qué acción identifica.
     *
     * <p>Si no se le puso uno, la etiqueta — con la trampa de que cambia al traducir.
     */
    public String getActionCommand() {
        if (this.actionCommand == null) {
            return this.label;
        }
        return this.actionCommand;
    }

    /** Suma alguien a quien avisarle cuando se elija; un `null` se ignora. */
    public synchronized void addActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.add(this.actionListener, l);
        this.enableEvents(AWTEvent.ACTION_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.remove(this.actionListener, l);
    }

    /** Los oyentes registrados. */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(this.actionListener, ActionListener.class);
    }

    /**
     * Los oyentes de esa clase.
     *
     * @throws ClassCastException si la clase no es de oyente
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if (listenerType == ActionListener.class) {
            l = this.actionListener;
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }

    /** Reparte el evento al método que corresponda. */
    protected void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            this.processActionEvent((ActionEvent) e);
        }
    }

    /** Les avisa a los oyentes de acción. */
    protected void processActionEvent(ActionEvent e) {
        ActionListener listener = this.actionListener;
        if (listener != null) {
            listener.actionPerformed(e);
        }
    }

    public String paramString() {
        String s = ",label=" + this.label;
        if (this.shortcut != null) {
            s = s + ",shortcut=" + this.shortcut;
        }
        return super.paramString() + s;
    }

    /** La información de accesibilidad de esta opción. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTMenuItem();
        }
        return this.accessibleContext;
    }

    /** La accesibilidad de una opción de menú. */
    protected class AccessibleAWTMenuItem extends AccessibleAWTMenuComponent {

        /** Para las subclases. */
        protected AccessibleAWTMenuItem() {
        }

        /** La etiqueta. */
        public String getAccessibleName() {
            return MenuItem.this.getLabel();
        }

        /** Es una opción de menú. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_ITEM;
        }

        /** Habilitado o no, que es lo único que se puede saber sin pantalla. */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = new AccessibleStateSet();
            if (MenuItem.this.isEnabled()) {
                s.add(AccessibleState.ENABLED);
            }
            return s;
        }
    }
}
