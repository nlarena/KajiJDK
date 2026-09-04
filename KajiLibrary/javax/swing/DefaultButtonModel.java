package javax.swing;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * El modelo de boton de siempre: cinco bits en un entero y tres listas de escuchas.
 *
 * <p>Los bits van en {@link #stateMask} para que el estado entero se lea de un golpe y para que
 * agregar uno no cambie la forma de la clase. Cada cambio de bit dispara
 * {@link #fireStateChanged}, que es lo que hace que el boton se repinte; ademas, soltar estando
 * armado dispara la accion, y seleccionar o deseleccionar dispara el evento de item.
 *
 * <p>Los modificadores del {@code ActionEvent} salen del evento que se esta despachando —el del
 * mouse o del teclado que provoco el click—, por {@link EventQueue#getCurrentEvent}. Un
 * {@code doClick} desde un programa no tiene evento en curso y los modificadores son cero.
 */
public class DefaultButtonModel implements ButtonModel, Serializable {

    /** Los bits de estado; ver las constantes. */
    protected int stateMask = 0;

    protected String actionCommand = null;

    protected ButtonGroup group = null;

    protected int mnemonic = 0;

    /** El evento de cambio, creado una vez: no lleva nada mas que el origen. */
    protected transient ChangeEvent changeEvent = null;

    protected EventListenerList listenerList = new EventListenerList();

    private boolean menuItem = false;

    public static final int ARMED = 1;
    public static final int SELECTED = 1 << 1;
    public static final int PRESSED = 1 << 2;
    public static final int ENABLED = 1 << 3;
    public static final int ROLLOVER = 1 << 4;

    /** Un modelo habilitado y en reposo. */
    public DefaultButtonModel() {
        stateMask = 0;
        setEnabled(true);
    }

    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public boolean isArmed() {
        return (stateMask & ARMED) != 0;
    }

    public boolean isSelected() {
        return (stateMask & SELECTED) != 0;
    }

    public boolean isEnabled() {
        return (stateMask & ENABLED) != 0;
    }

    public boolean isPressed() {
        return (stateMask & PRESSED) != 0;
    }

    public boolean isRollover() {
        return (stateMask & ROLLOVER) != 0;
    }

    /**
     * Arma o desarma; un modelo deshabilitado no se arma.
     *
     * <p>El JDK deja armarse a un item de menu deshabilitado cuando el aspecto lo pide
     * ({@code MenuItem.disabledAreNavigable}), para que el teclado pueda pasar por el. Sin
     * {@code UIManager} que lo diga, la regla es la misma para todos: deshabilitado no se arma.
     */
    public void setArmed(boolean b) {
        if (isArmed() == b || !isEnabled()) {
            return;
        }
        if (b) {
            stateMask = stateMask | ARMED;
        } else {
            stateMask = stateMask & ~ARMED;
        }
        fireStateChanged();
    }

    /** Habilita o deshabilita; deshabilitar tambien desarma y suelta. */
    public void setEnabled(boolean b) {
        if (isEnabled() == b) {
            return;
        }
        if (b) {
            stateMask = stateMask | ENABLED;
        } else {
            stateMask = stateMask & ~ENABLED;
            stateMask = stateMask & ~ARMED;
            stateMask = stateMask & ~PRESSED;
        }
        fireStateChanged();
    }

    /** Selecciona o deselecciona, avisando a los escuchas de item y de cambio. */
    public void setSelected(boolean b) {
        if (isSelected() == b) {
            return;
        }
        if (b) {
            stateMask = stateMask | SELECTED;
        } else {
            stateMask = stateMask & ~SELECTED;
        }
        fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                b ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
        fireStateChanged();
    }

    /**
     * Aprieta o suelta; soltar estando armado dispara la accion.
     *
     * <p>Es el corazon del click: la accion no sale al apretar sino al soltar, y solo si el
     * modelo sigue armado, que es lo que se pierde al sacar el mouse del boton.
     */
    public void setPressed(boolean b) {
        if (isPressed() == b || !isEnabled()) {
            return;
        }
        if (b) {
            stateMask = stateMask | PRESSED;
        } else {
            stateMask = stateMask & ~PRESSED;
        }
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
        fireStateChanged();
    }

    /** El cursor entro o salio; un modelo deshabilitado no se entera. */
    public void setRollover(boolean b) {
        if (isRollover() == b || !isEnabled()) {
            return;
        }
        if (b) {
            stateMask = stateMask | ROLLOVER;
        } else {
            stateMask = stateMask & ~ROLLOVER;
        }
        fireStateChanged();
    }

    public void setMnemonic(int key) {
        mnemonic = key;
        fireStateChanged();
    }

    public int getMnemonic() {
        return mnemonic;
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /** Avisa que algun bit cambio; el evento se crea la primera vez y se reusa. */
    protected void fireStateChanged() {
        Object[] escuchas = listenerList.getListenerList();
        for (int i = escuchas.length - 2; i >= 0; i = i - 2) {
            if (escuchas[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) escuchas[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    protected void fireActionPerformed(ActionEvent e) {
        Object[] escuchas = listenerList.getListenerList();
        for (int i = escuchas.length - 2; i >= 0; i = i - 2) {
            if (escuchas[i] == ActionListener.class) {
                ((ActionListener) escuchas[i + 1]).actionPerformed(e);
            }
        }
    }

    public void addItemListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }

    public void removeItemListener(ItemListener l) {
        listenerList.remove(ItemListener.class, l);
    }

    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    protected void fireItemStateChanged(ItemEvent e) {
        Object[] escuchas = listenerList.getListenerList();
        for (int i = escuchas.length - 2; i >= 0; i = i - 2) {
            if (escuchas[i] == ItemListener.class) {
                ((ItemListener) escuchas[i + 1]).itemStateChanged(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** Ninguno: un modelo no sabe que objeto representa; eso lo sabe el boton. */
    public Object[] getSelectedObjects() {
        return null;
    }

    public void setGroup(ButtonGroup group) {
        this.group = group;
    }

    public ButtonGroup getGroup() {
        return group;
    }

    /** Si este modelo es de un item de menu; ver {@link #setArmed}. */
    public boolean isMenuItem() {
        return menuItem;
    }

    public void setMenuItem(boolean menuItem) {
        this.menuItem = menuItem;
    }
}
