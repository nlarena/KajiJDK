package javax.management.openmbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * La implementación de {@link CompositeData}: un mapa de nombre a valor, congelado al construirse.
 *
 * <p>Lo que hace el constructor y conviene tener presente: **valida cada valor contra su tipo**. Un
 * item declarado `SimpleType.INTEGER` al que se le pasa un `String` no entra, y el error sale en el
 * momento de armar el valor en vez de en el otro extremo de la conexión. Eso es todo lo que un tipo
 * abierto compra, y por eso el constructor tira `OpenDataException` en vez de confiar.
 *
 * <p>Un nulo **sí** se acepta para cualquier item: significa "sin valor" y es distinto de que el
 * item no exista. `containsKey` de un item con valor nulo devuelve `true`.
 */
public class CompositeDataSupport implements CompositeData, Serializable {

    private static final long serialVersionUID = 8003518976613702244L;

    private final CompositeType compositeType;
    // Ordenado por nombre, igual que los items del tipo: `values()` promete ese orden.
    private final Map<String, Object> contents;

    /**
     * Un valor compuesto con esos items.
     *
     * <p>Los dos arreglos van en paralelo.
     *
     * @throws OpenDataException si falta un item del tipo, si sobra uno que el tipo no tiene, o si
     *     algún valor no es del tipo que su item declara
     * @throws IllegalArgumentException si el tipo o los arreglos son nulos, si no tienen el mismo
     *     largo, o si algún nombre está en blanco
     */
    public CompositeDataSupport(CompositeType compositeType, String[] itemNames,
            Object[] itemValues) throws OpenDataException {
        this(compositeType, asMap(compositeType, itemNames, itemValues));
    }

    /**
     * Un valor compuesto con los items de ese mapa.
     *
     * @throws OpenDataException si falta un item del tipo, si sobra uno, o si algún valor no es
     *     del tipo que su item declara
     * @throws IllegalArgumentException si el tipo o el mapa son nulos, o si alguna clave está en
     *     blanco
     */
    public CompositeDataSupport(CompositeType compositeType, Map<String, ?> items)
            throws OpenDataException {
        if (compositeType == null) {
            throw new IllegalArgumentException("el tipo compuesto no puede ser nulo");
        }
        if (items == null) {
            throw new IllegalArgumentException("el mapa de items no puede ser nulo");
        }
        Set<String> expected = compositeType.keySet();
        Map<String, Object> given = new TreeMap<String, Object>();
        for (Map.Entry<String, ?> e : items.entrySet()) {
            String n = e.getKey();
            if (n == null || n.trim().length() == 0) {
                throw new IllegalArgumentException("hay una key en blanco");
            }
            n = n.trim();
            if (!expected.contains(n)) {
                throw new OpenDataException(n + " no es un item de " + compositeType.getTypeName());
            }
            Object v = e.getValue();
            // El nulo pasa siempre: es "sin valor", y ningún `isValue` lo acepta. Comprobarlo
            // contra el tipo lo rechazaría, que es justo lo contrario de lo que define el contrato.
            if (v != null && !compositeType.getType(n).isValue(v)) {
                throw new OpenDataException("el valor de " + n + " no es de tipo "
                        + compositeType.getType(n).getTypeName());
            }
            given.put(n, v);
        }
        // Faltar un item es un error y no un nulo implícito. La diferencia importa: un valor
        // compuesto describe algo completo, y "no me acordé de poner este item" y "este item vale
        // nulo" son dos cosas distintas que el que lee no podría separar.
        for (String n : expected) {
            if (!given.containsKey(n)) {
                throw new OpenDataException("falta el item " + n);
            }
        }
        this.compositeType = compositeType;
        this.contents = Collections.unmodifiableMap(given);
    }

    // Se arma el mapa antes de llamar al otro constructor porque `this(...)` tiene que ser la
    // primera sentencia y la validación de los arreglos necesita correr antes que él.
    private static Map<String, Object> asMap(CompositeType compositeType, String[] itemNames,
            Object[] itemValues) throws OpenDataException {
        if (itemNames == null || itemValues == null) {
            throw new IllegalArgumentException("los arreglos de items no pueden ser nulos");
        }
        if (itemNames.length != itemValues.length) {
            throw new IllegalArgumentException(
                    "los arreglos de names y valores tienen que tener el mismo largo");
        }
        Map<String, Object> m = new TreeMap<String, Object>();
        for (int i = 0; i < itemNames.length; i++) {
            if (itemNames[i] == null || itemNames[i].trim().length() == 0) {
                throw new IllegalArgumentException("el nombre del item " + i + " está en blanco");
            }
            String n = itemNames[i].trim();
            if (m.containsKey(n)) {
                throw new OpenDataException("el item " + n + " está repetido");
            }
            m.put(n, itemValues[i]);
        }
        return m;
    }

    public CompositeType getCompositeType() {
        return this.compositeType;
    }

    public Object get(String key) {
        this.requireItem(key);
        return this.contents.get(key.trim());
    }

    public Object[] getAll(String[] keys) {
        if (keys == null || keys.length == 0) {
            return new Object[0];
        }
        Object[] out = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            out[i] = this.get(keys[i]);
        }
        return out;
    }

    private void requireItem(String key) {
        if (key == null || key.trim().length() == 0) {
            throw new IllegalArgumentException("el nombre del item está en blanco");
        }
        if (!this.contents.containsKey(key.trim())) {
            throw new InvalidKeyException(key + " no es un item de este valor");
        }
    }

    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        return this.contents.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return this.contents.containsValue(value);
    }

    /** Los valores, en el orden de los nombres. Ver {@link CompositeData#values}. */
    public Collection<?> values() {
        List<Object> out = new ArrayList<Object>(this.contents.values());
        return Collections.unmodifiableList(out);
    }

    /**
     * Igualdad por tipo y valores, contra **cualquier** {@link CompositeData}.
     *
     * <p>No se compara la clase: ver la nota de {@link CompositeData}.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CompositeData)) {
            return false;
        }
        CompositeData other = (CompositeData) obj;
        if (!this.compositeType.equals(other.getCompositeType())) {
            return false;
        }
        for (Map.Entry<String, Object> e : this.contents.entrySet()) {
            Object mine = e.getValue();
            Object theirs = other.get(e.getKey());
            if (mine == null ? theirs != null : !deepEquals(mine, theirs)) {
                return false;
            }
        }
        return true;
    }

    // Un item puede ser un arreglo, y `Object.equals` de dos arreglos distintos con el mismo
    // contenido es `false`. Comparar por contenido es lo que hace que dos valores compuestos
    // iguales que viajaron por separado se reconozcan.
    private static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getClass().isArray() && b.getClass().isArray()) {
            if (!a.getClass().equals(b.getClass())) {
                return false;
            }
            int n = java.lang.reflect.Array.getLength(a);
            if (n != java.lang.reflect.Array.getLength(b)) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                if (!deepEquals(java.lang.reflect.Array.get(a, i),
                        java.lang.reflect.Array.get(b, i))) {
                    return false;
                }
            }
            return true;
        }
        return a.equals(b);
    }

    /** La suma de los hashes del tipo y de los valores no nulos, como manda el contrato. */
    public int hashCode() {
        int h = this.compositeType.hashCode();
        for (Object v : this.contents.values()) {
            if (v != null) {
                h = h + hashOf(v);
            }
        }
        return h;
    }

    private static int hashOf(Object v) {
        if (v.getClass().isArray()) {
            int h = 1;
            int n = java.lang.reflect.Array.getLength(v);
            for (int i = 0; i < n; i++) {
                Object e = java.lang.reflect.Array.get(v, i);
                h = 31 * h + (e == null ? 0 : hashOf(e));
            }
            return h;
        }
        return v.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CompositeDataSupport.class.getName());
        sb.append("(compositeType=").append(this.compositeType.toString());
        sb.append(",contents={");
        boolean first = true;
        for (Map.Entry<String, Object> e : this.contents.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(e.getKey()).append("=").append(String.valueOf(e.getValue()));
        }
        sb.append("})");
        return sb.toString();
    }
}
