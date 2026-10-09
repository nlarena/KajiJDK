package javax.swing;

import java.beans.PropertyChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link Action} ready to be inherited from: it keeps the properties and gives notice when
 * they change; whoever inherits only puts in {@code actionPerformed}.
 *
 * <p>The properties go in a map by key; "enabled" does not, which has a field and a notifier
 * of its own because it is the one everybody consults. The notices go out through a
 * {@link SwingPropertyChangeSupport} with the action as the source, and {@link #putValue} gives
 * notice only if the value really changed: a button that listens has no reason to repaint
 * itself for a {@code putValue} that changed nothing.
 *
 * <p>The JDK keeps the properties in a table of its own ({@code ArrayTable}) that is an array up
 * to eight entries and a map afterwards; here it is a map from the start. It is a detail of
 * memory, not of behaviour.
 */
public abstract class AbstractAction implements Action, Cloneable, Serializable {

    /** Whether it is enabled; {@link #isEnabled} consults it. */
    protected boolean enabled = true;

    /** The change notifier; it is created with the first listener. */
    protected SwingPropertyChangeSupport changeSupport;

    private Map<String, Object> values;

    public AbstractAction() {
    }

    public AbstractAction(String name) {
        putValue(Action.NAME, name);
    }

    public AbstractAction(String name, Icon icon) {
        this(name);
        putValue(Action.SMALL_ICON, icon);
    }

    /** The property with that key; "enabled" can be asked for through here too. */
    public Object getValue(String key) {
        if ("enabled".equals(key)) {
            return Boolean.valueOf(enabled);
        }
        if (values == null) {
            return null;
        }
        return values.get(key);
    }

    /**
     * It sets a property, giving notice if it changed.
     *
     * <p>A {@code null} value erases the key. "enabled" with a {@code Boolean} goes to
     * {@link #setEnabled}, so that both paths give notice the same way.
     */
    public void putValue(String key, Object newValue) {
        Object old = null;
        if ("enabled".equals(key)) {
            if (newValue == null || !(newValue instanceof Boolean)) {
                newValue = Boolean.FALSE;
            }
            old = Boolean.valueOf(enabled);
            enabled = ((Boolean) newValue).booleanValue();
        } else {
            if (values == null) {
                values = new HashMap<String, Object>();
            }
            if (values.containsKey(key)) {
                old = values.get(key);
            }
            if (newValue == null) {
                values.remove(key);
            } else {
                values.put(key, newValue);
            }
        }
        firePropertyChange(key, old, newValue);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean newValue) {
        boolean old = this.enabled;
        if (old != newValue) {
            this.enabled = newValue;
            firePropertyChange("enabled", Boolean.valueOf(old), Boolean.valueOf(newValue));
        }
    }

    /** The keys with a value, in a new array; {@code null} if none was ever set. */
    public Object[] getKeys() {
        if (values == null) {
            return null;
        }
        return values.keySet().toArray();
    }

    /**
     * It gives notice of a property change, unless the value is the same.
     *
     * <p>The same by {@code equals}, not by identity: two equal strings are not a change.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport == null || (oldValue != null && newValue != null
                && oldValue.equals(newValue))) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }
        changeSupport.removePropertyChangeListener(listener);
    }

    /** The registered listeners, in a new array; empty if there are none. */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners();
    }

    /** A copy with the same properties, in a map of its own; the listeners are not copied. */
    protected Object clone() throws CloneNotSupportedException {
        AbstractAction copy = (AbstractAction) super.clone();
        if (values != null) {
            copy.values = new HashMap<String, Object>(values);
        }
        copy.changeSupport = null;
        return copy;
    }
}
