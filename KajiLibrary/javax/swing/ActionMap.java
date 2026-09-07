package javax.swing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * Una tabla de nombre a accion, encadenada con otra.
 *
 * <h2>Para que sirve la cadena</h2>
 *
 * <p>Un componente tiene tres capas de acciones: las que le puso el programa, las que le puso el
 * aspecto, y las que hereda de su clase. Encadenar tres tablas permite que el programa tape una sola
 * accion del aspecto sin copiar las otras, y que cambiar el aspecto reemplace su capa sin tocar lo
 * que puso el programa.
 *
 * <p>{@link #keys} devuelve solo las de esta tabla y {@link #allKeys} las de toda la cadena. La
 * diferencia importa: para guardar la configuracion se quieren las propias, y para saber que teclas
 * responden hay que mirar todas.
 *
 * <h2>Poner nulo borra</h2>
 *
 * <p>{@code put(clave, null)} saca la entrada en lugar de guardar un nulo. Es lo que hace el JDK, y
 * la consecuencia util es que no se puede tapar una accion del padre con "ninguna accion": para eso
 * hay que sacarla de la cadena entera.
 */
public class ActionMap implements Serializable {

    private transient HashMap<Object, Action> arrayTable;
    private ActionMap parent;

    /** Una tabla vacia, sin padre. */
    public ActionMap() {
    }

    /** La tabla que se consulta cuando esta no tiene la clave. */
    public void setParent(ActionMap map) {
        this.parent = map;
    }

    public ActionMap getParent() {
        return parent;
    }

    /** Guarda una accion; con {@code null} la saca. Ver la nota de la clase. */
    public void put(Object key, Action action) {
        if (key == null) {
            return;
        }
        if (action == null) {
            remove(key);
            return;
        }
        if (arrayTable == null) {
            arrayTable = new HashMap<Object, Action>();
        }
        arrayTable.put(key, action);
    }

    /** La accion de esa clave, buscando en la cadena. */
    public Action get(Object key) {
        Action value = (arrayTable == null) ? null : arrayTable.get(key);
        if (value == null) {
            ActionMap parent = getParent();
            if (parent != null) {
                return parent.get(key);
            }
        }
        return value;
    }

    public void remove(Object key) {
        if (arrayTable != null) {
            arrayTable.remove(key);
        }
    }

    /** Vacia esta tabla; el padre no se toca. */
    public void clear() {
        if (arrayTable != null) {
            arrayTable.clear();
        }
    }

    /** Las claves de esta tabla, sin las del padre. */
    public Object[] keys() {
        if (arrayTable == null || arrayTable.isEmpty()) {
            // Vacia devuelve nulo, no un arreglo de cero. Es lo que hace el JDK y hay codigo que
            // distingue "no hay tabla" de "hay tabla sin nada"; aca los dos dan lo mismo.
            return null;
        }
        Set<Object> ks = arrayTable.keySet();
        Object[] out = new Object[ks.size()];
        int i = 0;
        for (Object k : ks) {
            out[i] = k;
            i++;
        }
        return out;
    }

    public int size() {
        return (arrayTable == null) ? 0 : arrayTable.size();
    }

    /**
     * Las claves de toda la cadena, sin repetir.
     *
     * <p>Devuelve nulo si no hay ninguna, no un arreglo vacio. Es lo que hace el JDK y hay codigo
     * que distingue los dos casos.
     */
    public Object[] allKeys() {
        int count = size();
        ActionMap parent = getParent();
        if (parent == null) {
            return keys();
        }
        Object[] pk = parent.allKeys();
        Object[] mk = keys();
        if (pk == null) {
            return mk;
        }
        if (mk == null) {
            return pk;
        }
        HashMap<Object, Object> junta = new HashMap<Object, Object>();
        for (int i = 0; i < pk.length; i++) {
            junta.put(pk[i], pk[i]);
        }
        for (int i = 0; i < mk.length; i++) {
            junta.put(mk[i], mk[i]);
        }
        Object[] out = new Object[junta.size()];
        int i = 0;
        for (Object k : junta.keySet()) {
            out[i] = k;
            i++;
        }
        return out;
    }
}
