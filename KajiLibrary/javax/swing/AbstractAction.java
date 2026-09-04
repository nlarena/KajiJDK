package javax.swing;

import java.beans.PropertyChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Una {@link Action} lista para heredar: guarda las propiedades y avisa cuando cambian; el que
 * hereda pone solo {@code actionPerformed}.
 *
 * <p>Las propiedades van en un mapa por clave; "enabled" no, que tiene su campo y su avisador
 * propios porque es la que todos consultan. Los avisos salen por un {@link SwingPropertyChangeSupport}
 * con la accion como origen, y {@link #putValue} avisa solo si el valor cambio de verdad: un
 * boton que escucha no tiene por que repintarse por un {@code putValue} que no cambio nada.
 *
 * <p>El JDK guarda las propiedades en una tabla propia ({@code ArrayTable}) que es un arreglo hasta
 * ocho entradas y un mapa despues; aca es un mapa desde el principio. Es un detalle de memoria,
 * no de comportamiento.
 */
public abstract class AbstractAction implements Action, Cloneable, Serializable {

    /** Si esta habilitada; la consulta {@link #isEnabled}. */
    protected boolean enabled = true;

    /** El avisador de cambios; se crea con el primer escucha. */
    protected SwingPropertyChangeSupport changeSupport;

    private Map<String, Object> valores;

    public AbstractAction() {
    }

    public AbstractAction(String name) {
        putValue(Action.NAME, name);
    }

    public AbstractAction(String name, Icon icon) {
        this(name);
        putValue(Action.SMALL_ICON, icon);
    }

    /** La propiedad con esa clave; "enabled" tambien se puede pedir por aca. */
    public Object getValue(String key) {
        if ("enabled".equals(key)) {
            return Boolean.valueOf(enabled);
        }
        if (valores == null) {
            return null;
        }
        return valores.get(key);
    }

    /**
     * Pone una propiedad, avisando si cambio.
     *
     * <p>Un valor {@code null} borra la clave. "enabled" con un {@code Boolean} va a
     * {@link #setEnabled}, para que los dos caminos avisen igual.
     */
    public void putValue(String key, Object newValue) {
        Object viejo = null;
        if ("enabled".equals(key)) {
            if (newValue == null || !(newValue instanceof Boolean)) {
                newValue = Boolean.FALSE;
            }
            viejo = Boolean.valueOf(enabled);
            enabled = ((Boolean) newValue).booleanValue();
        } else {
            if (valores == null) {
                valores = new HashMap<String, Object>();
            }
            if (valores.containsKey(key)) {
                viejo = valores.get(key);
            }
            if (newValue == null) {
                valores.remove(key);
            } else {
                valores.put(key, newValue);
            }
        }
        firePropertyChange(key, viejo, newValue);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean newValue) {
        boolean viejo = this.enabled;
        if (viejo != newValue) {
            this.enabled = newValue;
            firePropertyChange("enabled", Boolean.valueOf(viejo), Boolean.valueOf(newValue));
        }
    }

    /** Las claves con valor, en un arreglo nuevo; {@code null} si nunca se puso ninguna. */
    public Object[] getKeys() {
        if (valores == null) {
            return null;
        }
        return valores.keySet().toArray();
    }

    /**
     * Avisa un cambio de propiedad, salvo que el valor sea el mismo.
     *
     * <p>El mismo por {@code equals}, no por identidad: dos cadenas iguales no son un cambio.
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

    /** Los escuchas registrados, en un arreglo nuevo; vacio si no hay. */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners();
    }

    /** Una copia con las mismas propiedades, en un mapa propio; los escuchas no se copian. */
    protected Object clone() throws CloneNotSupportedException {
        AbstractAction copia = (AbstractAction) super.clone();
        if (valores != null) {
            copia.valores = new HashMap<String, Object>(valores);
        }
        copia.changeSupport = null;
        return copia;
    }
}
