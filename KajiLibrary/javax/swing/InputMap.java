package javax.swing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * Una tabla de tecla a nombre de accion, encadenada con otra.
 *
 * <h2>Para que sirve la cadena</h2>
 *
 * <p>Igual que en {@link ActionMap}: hay una capa del programa, una del aspecto y una de la clase.
 * Encadenarlas permite tapar un atajo sin copiar los otros y cambiar el aspecto sin perder los que
 * puso el programa.
 *
 * <p>Lo que guarda no es la accion sino su <em>nombre</em>. Esa indireccion es la que permite que
 * cambiar el atajo de una accion y cambiar lo que la accion hace sean dos cosas separadas.
 *
 * <p>{@link #keys} devuelve solo las de esta tabla y {@link #allKeys} las de toda la cadena. La
 * diferencia importa: para guardar la configuracion se quieren las propias, y para saber que teclas
 * responden hay que mirar todas.
 *
 * <h2>Poner nulo borra</h2>
 *
 * <p>{@code put(tecla, null)} saca la entrada en lugar de guardar un nulo, igual que en
 * {@link ActionMap}.
 */
public class InputMap implements Serializable {

    private transient HashMap<KeyStroke, Object> arrayTable;
    private InputMap parent;

    /** Una tabla vacia, sin padre. */
    public InputMap() {
    }

    /** La tabla que se consulta cuando esta no tiene la clave. */
    public void setParent(InputMap map) {
        this.parent = map;
    }

    public InputMap getParent() {
        return parent;
    }

    /** Guarda el nombre de la accion de esa tecla; con {@code null} la saca. */
    public void put(KeyStroke key, Object actionMapKey) {
        if (key == null) {
            return;
        }
        if (actionMapKey == null) {
            remove(key);
            return;
        }
        if (arrayTable == null) {
            arrayTable = new HashMap<KeyStroke, Object>();
        }
        arrayTable.put(key, actionMapKey);
    }

    /** El nombre de accion de esa tecla, buscando en la cadena. */
    public Object get(KeyStroke key) {
        Object value = (arrayTable == null) ? null : arrayTable.get(key);
        if (value == null) {
            InputMap parent = getParent();
            if (parent != null) {
                return parent.get(key);
            }
        }
        return value;
    }

    public void remove(KeyStroke key) {
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

    /** Las teclas de esta tabla, sin las del padre. */
    public KeyStroke[] keys() {
        if (arrayTable == null || arrayTable.isEmpty()) {
            // Vacia devuelve nulo, no un arreglo de cero. Es lo que hace el JDK y hay codigo que
            // distingue "no hay tabla" de "hay tabla sin nada"; aca los dos dan lo mismo.
            return null;
        }
        Set<KeyStroke> ks = arrayTable.keySet();
        KeyStroke[] out = new KeyStroke[ks.size()];
        int i = 0;
        for (KeyStroke k : ks) {
            out[i] = k;
            i++;
        }
        return out;
    }

    public int size() {
        return (arrayTable == null) ? 0 : arrayTable.size();
    }

    /**
     * Las teclas de toda la cadena, sin repetir.
     *
     * <p>Devuelve nulo si no hay ninguna, no un arreglo vacio. Es lo que hace el JDK y hay codigo
     * que distingue los dos casos.
     */
    public KeyStroke[] allKeys() {
        int count = size();
        InputMap parent = getParent();
        if (parent == null) {
            return keys();
        }
        KeyStroke[] pk = parent.allKeys();
        KeyStroke[] mk = keys();
        if (pk == null) {
            return mk;
        }
        if (mk == null) {
            return pk;
        }
        HashMap<KeyStroke, KeyStroke> junta = new HashMap<KeyStroke, KeyStroke>();
        for (int i = 0; i < pk.length; i++) {
            junta.put(pk[i], pk[i]);
        }
        for (int i = 0; i < mk.length; i++) {
            junta.put(mk[i], mk[i]);
        }
        KeyStroke[] out = new KeyStroke[junta.size()];
        int i = 0;
        for (KeyStroke k : junta.keySet()) {
            out[i] = k;
            i++;
        }
        return out;
    }
}
