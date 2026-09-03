package javax.management.openmbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La implementación de {@link TabularData}, sobre un `HashMap` de clave a fila.
 *
 * <p>Implementa además `Map&lt;Object, Object&gt;`, y de ahí sale la única rareza de esta clase:
 * **hay dos juegos de métodos con el mismo nombre**. `get(Object[])` es el de la tabla y
 * `get(Object)` el del mapa; `put(CompositeData)` es el de la tabla y `put(Object, Object)` el del
 * mapa. No son sobrecargas cómodas: los del mapa existen porque `Map` los exige.
 *
 * <p>Los del mapa se comportan como los de la tabla en lo que se puede, con dos diferencias que hay
 * que saber:
 *
 * <ul>
 * <li>`put(clave, valor)` **ignora la clave** y usa la que se calcula del valor. Poner una clave
 *     distinta de la que la fila implica describiría una tabla imposible, así que se descarta en
 *     vez de guardarse.</li>
 * <li>`get(Object)` espera un `Object[]`; con cualquier otra cosa devuelve nulo, que es lo que un
 *     `Map` contesta para una clave que no tiene.</li>
 * </ul>
 *
 * <p>La clave interna es una `List` y no el `Object[]`: dos arreglos con el mismo contenido no son
 * iguales ni comparten `hashCode`, así que usarlos de clave haría que ninguna fila se encontrara
 * nunca. La lista sí compara por contenido.
 */
public class TabularDataSupport
        implements TabularData, Map<Object, Object>, Cloneable, Serializable {

    private static final long serialVersionUID = 5720150593236309827L;

    private final TabularType tabularType;
    private final Map<Object, Object> dataMap;
    // Los nombres de índice, en orden: se leen una vez porque se usan en cada `put`.
    private final transient String[] indexNames;

    /** Una tabla vacía de ese tipo. */
    public TabularDataSupport(TabularType tabularType) {
        this(tabularType, 16, 0.75f);
    }

    /**
     * Una tabla vacía de ese tipo, con esa capacidad inicial y ese factor de carga.
     *
     * @throws IllegalArgumentException si el tipo es nulo
     */
    public TabularDataSupport(TabularType tabularType, int initialCapacity, float loadFactor) {
        if (tabularType == null) {
            throw new IllegalArgumentException("el tipo tabular no puede ser nulo");
        }
        this.tabularType = tabularType;
        this.dataMap = new HashMap<Object, Object>(initialCapacity, loadFactor);
        List<String> names = tabularType.getIndexNames();
        this.indexNames = names.toArray(new String[0]);
    }

    public TabularType getTabularType() {
        return this.tabularType;
    }

    public Object[] calculateIndex(CompositeData value) {
        this.requireRow(value);
        Object[] key = new Object[this.indexNames.length];
        for (int i = 0; i < this.indexNames.length; i++) {
            key[i] = value.get(this.indexNames[i]);
        }
        return key;
    }

    private void requireRow(CompositeData value) {
        if (value == null) {
            throw new NullPointerException("la fila no puede ser nula");
        }
        if (!this.tabularType.getRowType().equals(value.getCompositeType())) {
            throw new InvalidOpenTypeException("la fila no es de tipo "
                    + this.tabularType.getRowType().getTypeName());
        }
    }

    // La clave de verdad: una lista, que compara por contenido. Ver la nota de la clase.
    private static List<Object> asKey(Object[] key) {
        List<Object> l = new ArrayList<Object>();
        for (int i = 0; i < key.length; i++) {
            l.add(key[i]);
        }
        // De solo lectura porque `keySet()` las expone: una clave que el llamador pudiera cambiar
        // desincronizaria el mapa de su propio indice.
        return java.util.Collections.unmodifiableList(l);
    }

    // El reparto entre las dos excepciones no es simetrico y esta comprobado contra el JDK 25:
    // una clave nula O VACIA es `NullPointerException`, y una de largo equivocado pero no vacia es
    // `InvalidKeyException`. Es raro y es el contrato; escribirlo al reves hace que un cliente que
    // atrapa una de las dos deje de funcionar contra el JDK real.
    private void requireKey(Object[] key) {
        if (key == null || key.length == 0) {
            throw new NullPointerException("la clave no puede ser nula ni vacia");
        }
        if (key.length != this.indexNames.length) {
            throw new InvalidKeyException("la clave tiene " + key.length
                    + " valores y el tipo pide " + this.indexNames.length);
        }
        for (int i = 0; i < key.length; i++) {
            OpenType<?> t = this.tabularType.getRowType().getType(this.indexNames[i]);
            if (key[i] != null && !t.isValue(key[i])) {
                throw new InvalidKeyException("el valor de índice " + this.indexNames[i]
                        + " no es de tipo " + t.getTypeName());
            }
        }
    }

    public boolean containsKey(Object[] key) {
        if (key == null || key.length != this.indexNames.length) {
            return false;
        }
        return this.dataMap.containsKey(asKey(key));
    }

    public boolean containsKey(Object key) {
        if (!(key instanceof Object[])) {
            return false;
        }
        return this.containsKey((Object[]) key);
    }

    public boolean containsValue(CompositeData value) {
        if (value == null) {
            return false;
        }
        return this.dataMap.containsValue(value);
    }

    public boolean containsValue(Object value) {
        return this.dataMap.containsValue(value);
    }

    public CompositeData get(Object[] key) {
        this.requireKey(key);
        return (CompositeData) this.dataMap.get(asKey(key));
    }

    public Object get(Object key) {
        if (!(key instanceof Object[])) {
            return null;
        }
        Object[] k = (Object[]) key;
        if (k.length != this.indexNames.length) {
            return null;
        }
        return this.dataMap.get(asKey(k));
    }

    public void put(CompositeData value) {
        Object[] key = this.calculateIndex(value);
        List<Object> k = asKey(key);
        if (this.dataMap.containsKey(k)) {
            throw new KeyAlreadyExistsException("ya hay una fila con esa key");
        }
        this.dataMap.put(k, value);
    }

    /**
     * Agrega esa fila, **ignorando la clave**. Ver la nota de la clase.
     *
     * @return siempre nulo: no puede reemplazar, así que nunca hay un valor anterior que devolver
     */
    public Object put(Object key, Object value) {
        this.put((CompositeData) value);
        return null;
    }

    public CompositeData remove(Object[] key) {
        this.requireKey(key);
        return (CompositeData) this.dataMap.remove(asKey(key));
    }

    public Object remove(Object key) {
        if (!(key instanceof Object[])) {
            return null;
        }
        Object[] k = (Object[]) key;
        if (k.length != this.indexNames.length) {
            return null;
        }
        return this.dataMap.remove(asKey(k));
    }

    /**
     * Agrega todas esas filas, o ninguna.
     *
     * <p>Se valida todo primero y recién después se escribe. Sin eso, un arreglo con la última fila
     * repetida dejaría las anteriores puestas y la tabla a medio cargar -- que es peor que no haber
     * empezado, porque el que llamó no sabe dónde quedó.
     */
    public void putAll(CompositeData[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        Map<Object, Object> pending = new HashMap<Object, Object>();
        for (int i = 0; i < values.length; i++) {
            Object[] key = this.calculateIndex(values[i]);
            List<Object> k = asKey(key);
            if (this.dataMap.containsKey(k) || pending.containsKey(k)) {
                throw new KeyAlreadyExistsException("ya hay una fila con esa key");
            }
            pending.put(k, values[i]);
        }
        this.dataMap.putAll(pending);
    }

    /** Agrega todas las filas de ese mapa. Las claves se ignoran, como en {@link #put}. */
    public void putAll(Map<?, ?> t) {
        if (t == null || t.isEmpty()) {
            return;
        }
        CompositeData[] rows = new CompositeData[t.size()];
        int i = 0;
        for (Object v : t.values()) {
            rows[i] = (CompositeData) v;
            i = i + 1;
        }
        this.putAll(rows);
    }

    public void clear() {
        this.dataMap.clear();
    }

    public int size() {
        return this.dataMap.size();
    }

    public boolean isEmpty() {
        return this.dataMap.isEmpty();
    }

    public Set<Object> keySet() {
        return this.dataMap.keySet();
    }

    public Collection<Object> values() {
        return this.dataMap.values();
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return this.dataMap.entrySet();
    }

    /**
     * Una copia superficial.
     *
     * <p>Superficial alcanza: las filas son {@link CompositeData}, que son inmutables, así que
     * compartirlas entre la copia y el original no permite que una cambie a la otra.
     */
    public Object clone() {
        TabularDataSupport copy = new TabularDataSupport(this.tabularType);
        copy.dataMap.putAll(this.dataMap);
        return copy;
    }

    /** Igualdad por tipo y filas, contra cualquier {@link TabularData}. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TabularData)) {
            return false;
        }
        TabularData other = (TabularData) obj;
        if (!this.tabularType.equals(other.getTabularType())) {
            return false;
        }
        if (this.size() != other.size()) {
            return false;
        }
        for (Object v : this.dataMap.values()) {
            if (!other.containsValue((CompositeData) v)) {
                return false;
            }
        }
        return true;
    }

    /** La suma del hash del tipo y de los de las filas, como manda el contrato. */
    public int hashCode() {
        int h = this.tabularType.hashCode();
        for (Object v : this.dataMap.values()) {
            h = h + v.hashCode();
        }
        return h;
    }

    public String toString() {
        return TabularDataSupport.class.getName()
                + "(tabularType=" + this.tabularType.toString()
                + ",contents=" + this.dataMap.toString() + ")";
    }
}
