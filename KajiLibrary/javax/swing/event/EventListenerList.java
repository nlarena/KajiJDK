package javax.swing.event;

import java.util.EventListener;

/**
 * La lista de oyentes que usa todo Swing, guardada como pares {@code (clase, oyente)}.
 *
 * <h2>Por que un arreglo plano y no un mapa</h2>
 *
 * <p>Un componente escucha muchos tipos de evento y casi nunca tiene oyentes de mas de uno o dos.
 * Un {@code Map<Class, List>} costaria varios objetos por componente para guardar, tipicamente, un
 * elemento. El arreglo plano cuesta uno solo, y con dos oyentes recorrerlo entero es mas rapido que
 * hashear.
 *
 * <p>Es una optimizacion medida del JDK sobre un caso que se repite miles de veces en una interfaz.
 *
 * <h2>Copiar al escribir, que es lo que la hace segura</h2>
 *
 * <p>{@link #add} y {@link #remove} crean un arreglo nuevo en vez de modificar el que hay, y
 * {@link #getListenerList} devuelve el arreglo <strong>sin copiar</strong>. Eso permite repartir un
 * evento recorriendolo sin sincronizar y sin miedo a que alguien se de de baja en medio del reparto:
 * quien esta recorriendo tiene una foto.
 *
 * <p>Por eso mismo el arreglo devuelto <strong>no se toca</strong>. Es el precio del trato.
 */
public class EventListenerList implements java.io.Serializable {

    private static final long serialVersionUID = -5677132037850737084L;

    private static final Object[] VACIO = new Object[0];

    /** Los pares. Volatil: se reemplaza entero, y quien lee tiene que ver el reemplazo. */
    protected transient volatile Object[] listenerList = VACIO;

    /** Una lista vacia. */
    public EventListenerList() {
    }

    /**
     * El arreglo crudo de pares, <strong>sin copiar</strong>.
     *
     * <p>Se recorre de a dos: en {@code i} la clase, en {@code i+1} el oyente. No se modifica.
     */
    public Object[] getListenerList() {
        return this.listenerList;
    }

    /** Los oyentes de {@code t}, en un arreglo nuevo del tipo pedido. */
    public <T extends EventListener> T[] getListeners(Class<T> t) {
        Object[] lista = this.listenerList;
        int n = getListenerCount(lista, t);
        @SuppressWarnings("unchecked")
        T[] resultado = (T[]) java.lang.reflect.Array.newInstance(t, n);
        int j = 0;
        for (int i = lista.length - 2; i >= 0; i = i - 2) {
            if (lista[i] == t) {
                resultado[j] = (T) lista[i + 1];
                j = j + 1;
            }
        }
        return resultado;
    }

    /** Cuantos oyentes hay, de todos los tipos. */
    public int getListenerCount() {
        return this.listenerList.length / 2;
    }

    /** Cuantos oyentes hay de {@code t}. */
    public int getListenerCount(Class<?> t) {
        return getListenerCount(this.listenerList, t);
    }

    private int getListenerCount(Object[] lista, Class<?> t) {
        int n = 0;
        for (int i = 0; i < lista.length; i = i + 2) {
            if (t == lista[i]) {
                n = n + 1;
            }
        }
        return n;
    }

    /**
     * Agrega un oyente.
     *
     * @throws IllegalArgumentException si {@code l} no es del tipo {@code t}
     */
    public synchronized <T extends EventListener> void add(Class<T> t, T l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException("El oyente no es de " + t.getName());
        }
        Object[] viejo = this.listenerList;
        Object[] nuevo = new Object[viejo.length + 2];
        for (int i = 0; i < viejo.length; i++) {
            nuevo[i] = viejo[i];
        }
        nuevo[viejo.length] = t;
        nuevo[viejo.length + 1] = l;
        this.listenerList = nuevo;
    }

    /** Saca un oyente. Si estaba mas de una vez, saca uno solo. */
    public synchronized <T extends EventListener> void remove(Class<T> t, T l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException("El oyente no es de " + t.getName());
        }
        Object[] viejo = this.listenerList;
        int donde = -1;
        for (int i = viejo.length - 2; i >= 0; i = i - 2) {
            if (viejo[i] == t && viejo[i + 1].equals(l)) {
                donde = i;
                break;
            }
        }
        if (donde < 0) {
            return;
        }
        Object[] nuevo = new Object[viejo.length - 2];
        int j = 0;
        for (int i = 0; i < viejo.length; i = i + 2) {
            if (i != donde) {
                nuevo[j] = viejo[i];
                nuevo[j + 1] = viejo[i + 1];
                j = j + 2;
            }
        }
        this.listenerList = nuevo;
    }

    public String toString() {
        Object[] lista = this.listenerList;
        StringBuilder sb = new StringBuilder("EventListenerList: ");
        sb.append(String.valueOf(lista.length / 2));
        sb.append(" listeners: ");
        for (int i = 0; i < lista.length; i = i + 2) {
            sb.append(" type ");
            sb.append(((Class) lista[i]).getName());
            sb.append(" listener ");
            sb.append(lista[i + 1]);
        }
        return sb.toString();
    }
}
