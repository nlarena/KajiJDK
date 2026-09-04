package java.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// El gemelo vetable de PropertyChangeSupport, y la diferencia no es cosmetica: aca la notificacion
// puede FALLAR, y entonces hay que deshacerla.
//
// fireVetoableChange corre los oyentes en orden; si uno tira PropertyVetoException, los que ya
// habian dicho que si quedaron creyendo que el cambio va. Por eso se les vuelve a notificar, con
// los valores invertidos, para que reviertan lo que hayan hecho — y recien despues se propaga el
// veto. Sin esa segunda vuelta un veto dejaria al resto del sistema desincronizado.
//
// Igual que en PropertyChangeSupport, se despacha sobre una copia: un oyente puede desuscribirse
// desde adentro de vetableChange().
public class VetoableChangeSupport implements Serializable {

    private Object source;
    private List<VetoableChangeListener> globales;
    private Map<String, List<VetoableChangeListener>> porNombre;

    public VetoableChangeSupport(Object sourceBean) {
        if (sourceBean == null) {
            throw new NullPointerException();
        }
        this.source = sourceBean;
        this.globales = new ArrayList<VetoableChangeListener>();
        this.porNombre = new HashMap<String, List<VetoableChangeListener>>();
    }

    public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof VetoableChangeListenerProxy) {
            VetoableChangeListenerProxy proxy = (VetoableChangeListenerProxy) listener;
            this.agregarPorNombre(proxy.getPropertyName(), proxy.getListener());
        } else {
            this.globales.add(listener);
        }
    }

    public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof VetoableChangeListenerProxy) {
            VetoableChangeListenerProxy proxy = (VetoableChangeListenerProxy) listener;
            this.quitarPorNombre(proxy.getPropertyName(), proxy.getListener());
        } else {
            this.globales.remove(listener);
        }
    }

    public synchronized void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        this.agregarPorNombre(propertyName, listener);
    }

    public synchronized void removeVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        this.quitarPorNombre(propertyName, listener);
    }

    private void agregarPorNombre(String propertyName, VetoableChangeListener listener) {
        List<VetoableChangeListener> l = this.porNombre.get(propertyName);
        if (l == null) {
            l = new ArrayList<VetoableChangeListener>();
            this.porNombre.put(propertyName, l);
        }
        l.add(listener);
    }

    private void quitarPorNombre(String propertyName, VetoableChangeListener listener) {
        List<VetoableChangeListener> l = this.porNombre.get(propertyName);
        if (l != null) {
            l.remove(listener);
            if (l.isEmpty()) {
                this.porNombre.remove(propertyName);
            }
        }
    }

    public synchronized VetoableChangeListener[] getVetoableChangeListeners() {
        List<VetoableChangeListener> salida = new ArrayList<VetoableChangeListener>();
        for (int i = 0; i < this.globales.size(); i++) {
            salida.add(this.globales.get(i));
        }
        Object[] nombres = this.porNombre.keySet().toArray();
        for (int i = 0; i < nombres.length; i++) {
            String nombre = (String) nombres[i];
            List<VetoableChangeListener> l = this.porNombre.get(nombre);
            for (int j = 0; j < l.size(); j++) {
                salida.add(new VetoableChangeListenerProxy(nombre, l.get(j)));
            }
        }
        return this.aArreglo(salida);
    }

    public synchronized VetoableChangeListener[] getVetoableChangeListeners(String propertyName) {
        List<VetoableChangeListener> salida = new ArrayList<VetoableChangeListener>();
        if (propertyName != null) {
            List<VetoableChangeListener> l = this.porNombre.get(propertyName);
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    salida.add(l.get(i));
                }
            }
        }
        return this.aArreglo(salida);
    }

    private VetoableChangeListener[] aArreglo(List<VetoableChangeListener> l) {
        VetoableChangeListener[] a = new VetoableChangeListener[l.size()];
        for (int i = 0; i < l.size(); i++) {
            a[i] = l.get(i);
        }
        return a;
    }

    private synchronized VetoableChangeListener[] instantanea(String propertyName) {
        List<VetoableChangeListener> salida = new ArrayList<VetoableChangeListener>();
        for (int i = 0; i < this.globales.size(); i++) {
            salida.add(this.globales.get(i));
        }
        if (propertyName != null) {
            List<VetoableChangeListener> l = this.porNombre.get(propertyName);
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    salida.add(l.get(i));
                }
            }
        }
        return this.aArreglo(salida);
    }

    // Consulta a los oyentes y, si alguno veta, revierte a los que ya habian aceptado antes de
    // dejar salir la excepcion.
    public void fireVetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        Object viejo = evt.getOldValue();
        Object nuevo = evt.getNewValue();
        if (viejo == null || nuevo == null || !viejo.equals(nuevo)) {
            VetoableChangeListener[] copia = this.instantanea(evt.getPropertyName());
            int i = 0;
            PropertyVetoException veto = null;
            while (i < copia.length && veto == null) {
                try {
                    copia[i].vetoableChange(evt);
                    i = i + 1;
                } catch (PropertyVetoException e) {
                    veto = e;
                }
            }
            if (veto != null) {
                // `i` quedo en el que veto: hay que deshacer los [0, i).
                PropertyChangeEvent vuelta = new PropertyChangeEvent(
                    evt.getSource(), evt.getPropertyName(), nuevo, viejo);
                for (int j = 0; j < i; j++) {
                    try {
                        copia[j].vetoableChange(vuelta);
                    } catch (PropertyVetoException ignorada) {
                        // Vetar la reversion no tiene a donde ir: el cambio no se hizo igual.
                    }
                }
                throw veto;
            }
        }
    }

    public void fireVetoableChange(String propertyName, Object oldValue, Object newValue)
            throws PropertyVetoException {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            this.fireVetoableChange(new PropertyChangeEvent(this.source, propertyName, oldValue, newValue));
        }
    }

    public void fireVetoableChange(String propertyName, int oldValue, int newValue)
            throws PropertyVetoException {
        if (oldValue != newValue) {
            this.fireVetoableChange(propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void fireVetoableChange(String propertyName, boolean oldValue, boolean newValue)
            throws PropertyVetoException {
        if (oldValue != newValue) {
            this.fireVetoableChange(propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    public synchronized boolean hasListeners(String propertyName) {
        boolean hay = !this.globales.isEmpty();
        if (!hay && propertyName != null) {
            List<VetoableChangeListener> l = this.porNombre.get(propertyName);
            hay = l != null && !l.isEmpty();
        }
        return hay;
    }
}
