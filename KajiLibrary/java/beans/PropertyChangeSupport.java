package java.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// El ayudante que un bean con propiedades ligadas delega para llevar la lista de oyentes y
// despacharles los eventos. Se usa por composicion, no por herencia: el bean tiene uno y le
// reenvia add/remove/fire.
//
// Dos decisiones que no son de comodidad sino de correccion:
//
// 1. **Se notifica sobre una copia.** Un oyente tiene todo el derecho de desuscribirse a si mismo
//    —o de agregar otro— desde adentro de propertyChange(). Si se iterara la lista viva, esa
//    modificacion la corrompe en pleno recorrido. Copiar antes de despachar es lo que hace que el
//    caso mas natural del mundo no rompa nada. Los oyentes agregados durante una notificacion no
//    reciben ESE evento, y los que se van si lo reciben: es el precio de la copia y es el que
//    cobra el JDK.
//
// 2. **Los oyentes por nombre se guardan envueltos en un proxy.** getPropertyChangeListeners()
//    tiene que devolver ambas clases de oyente en un solo arreglo; envolver los que estan atados a
//    una propiedad en PropertyChangeListenerProxy es lo que deja distinguirlos al leerlos.
//
// La sincronizacion va a nivel de metodo y no en bloques: en este arbol un `synchronized` en bloque
// con un `return` temprano no emite el monitorexit (hallazgo #105), asi que los metodos que tocan
// las estructuras son `synchronized` enteros.
public class PropertyChangeSupport implements Serializable {

    // El bean que figura como `source` de los eventos. El JDK tambien rechaza null aca: un evento
    // sin origen no le sirve a ningun oyente.
    private Object source;

    // Oyentes registrados sin nombre: reciben todo.
    private List<PropertyChangeListener> globales;

    // Oyentes atados a una propiedad, por nombre.
    private Map<String, List<PropertyChangeListener>> porNombre;

    public PropertyChangeSupport(Object sourceBean) {
        if (sourceBean == null) {
            throw new NullPointerException();
        }
        this.source = sourceBean;
        this.globales = new ArrayList<PropertyChangeListener>();
        this.porNombre = new HashMap<String, List<PropertyChangeListener>>();
    }

    // Registra un oyente para todas las propiedades.
    //
    // Si viene un PropertyChangeListenerProxy se lo desarma y se registra por su nombre: asi
    // getPropertyChangeListeners() puede devolver su resultado a addPropertyChangeListener() y
    // reconstruir el mismo registro, que es lo que espera cualquiera que clone un bean.
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof PropertyChangeListenerProxy) {
            PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
            this.agregarPorNombre(proxy.getPropertyName(), proxy.getListener());
        } else {
            this.globales.add(listener);
        }
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof PropertyChangeListenerProxy) {
            PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
            this.quitarPorNombre(proxy.getPropertyName(), proxy.getListener());
        } else {
            this.globales.remove(listener);
        }
    }

    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        this.agregarPorNombre(propertyName, listener);
    }

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        this.quitarPorNombre(propertyName, listener);
    }

    private void agregarPorNombre(String propertyName, PropertyChangeListener listener) {
        List<PropertyChangeListener> l = this.porNombre.get(propertyName);
        if (l == null) {
            l = new ArrayList<PropertyChangeListener>();
            this.porNombre.put(propertyName, l);
        }
        l.add(listener);
    }

    private void quitarPorNombre(String propertyName, PropertyChangeListener listener) {
        List<PropertyChangeListener> l = this.porNombre.get(propertyName);
        if (l != null) {
            l.remove(listener);
            if (l.isEmpty()) {
                this.porNombre.remove(propertyName);
            }
        }
    }

    // Todos los oyentes: los globales tal cual, y los atados a una propiedad envueltos en un proxy
    // que dice a cual.
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        List<PropertyChangeListener> salida = new ArrayList<PropertyChangeListener>();
        for (int i = 0; i < this.globales.size(); i++) {
            salida.add(this.globales.get(i));
        }
        Object[] nombres = this.porNombre.keySet().toArray();
        for (int i = 0; i < nombres.length; i++) {
            String nombre = (String) nombres[i];
            List<PropertyChangeListener> l = this.porNombre.get(nombre);
            for (int j = 0; j < l.size(); j++) {
                salida.add(new PropertyChangeListenerProxy(nombre, l.get(j)));
            }
        }
        return this.aArreglo(salida);
    }

    // Solo los oyentes atados a `propertyName`, sin envolver: quien pregunto ya sabe el nombre.
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        List<PropertyChangeListener> salida = new ArrayList<PropertyChangeListener>();
        if (propertyName != null) {
            List<PropertyChangeListener> l = this.porNombre.get(propertyName);
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    salida.add(l.get(i));
                }
            }
        }
        return this.aArreglo(salida);
    }

    private PropertyChangeListener[] aArreglo(List<PropertyChangeListener> l) {
        PropertyChangeListener[] a = new PropertyChangeListener[l.size()];
        for (int i = 0; i < l.size(); i++) {
            a[i] = l.get(i);
        }
        return a;
    }

    // La copia que se despacha: globales + los atados al nombre del evento. Se arma bajo el
    // candado y se recorre afuera, que es justamente lo que hace seguro desuscribirse desde
    // adentro de un oyente.
    private synchronized PropertyChangeListener[] instantanea(String propertyName) {
        List<PropertyChangeListener> salida = new ArrayList<PropertyChangeListener>();
        for (int i = 0; i < this.globales.size(); i++) {
            salida.add(this.globales.get(i));
        }
        if (propertyName != null) {
            List<PropertyChangeListener> l = this.porNombre.get(propertyName);
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    salida.add(l.get(i));
                }
            }
        }
        return this.aArreglo(salida);
    }

    public void firePropertyChange(PropertyChangeEvent evt) {
        Object viejo = evt.getOldValue();
        Object nuevo = evt.getNewValue();
        // Dos valores conocidos e iguales no son un cambio. Si alguno es null no se sabe, y ante
        // la duda se notifica.
        if (viejo == null || nuevo == null || !viejo.equals(nuevo)) {
            PropertyChangeListener[] copia = this.instantanea(evt.getPropertyName());
            for (int i = 0; i < copia.length; i++) {
                copia[i].propertyChange(evt);
            }
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            this.firePropertyChange(new PropertyChangeEvent(this.source, propertyName, oldValue, newValue));
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (oldValue != newValue) {
            this.firePropertyChange(propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            this.firePropertyChange(propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            this.firePropertyChange(
                new IndexedPropertyChangeEvent(this.source, propertyName, oldValue, newValue, index));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
        if (oldValue != newValue) {
            this.fireIndexedPropertyChange(propertyName, index, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            this.fireIndexedPropertyChange(propertyName, index, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    // Si hay alguien escuchando esta propiedad. Sirve para saltearse el trabajo de calcular el
    // valor viejo cuando no hay a quien contarselo.
    public synchronized boolean hasListeners(String propertyName) {
        boolean hay = !this.globales.isEmpty();
        if (!hay && propertyName != null) {
            List<PropertyChangeListener> l = this.porNombre.get(propertyName);
            hay = l != null && !l.isEmpty();
        }
        return hay;
    }
}
