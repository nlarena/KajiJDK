package javax.management.openmbean;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * El tipo de un {@link CompositeData}: un conjunto de items con nombre, cada uno con su tipo
 * abierto.
 *
 * <p>Es el equivalente abierto de una clase con campos, y la diferencia importante con una clase es
 * que **la identidad no depende del nombre sino de la forma**: dos `CompositeType` con el mismo
 * `typeName`, la misma descripción y los mismos items son iguales aunque los haya construido gente
 * distinta. Eso es lo que hace que un cliente remoto pueda reconocer el tipo sin compartir código.
 *
 * <p>Los items se guardan **ordenados por nombre**, no en el orden en que se pasaron. No es un
 * detalle de implementación: {@link #keySet} lo expone, y el `hashCode` que el contrato define es la
 * suma de los hashes de nombres y tipos, que también es independiente del orden. Dicho de otro modo,
 * el orden de los arreglos del constructor **no** es parte del tipo.
 */
public class CompositeType extends OpenType<CompositeData> {

    private static final long serialVersionUID = -5366242454346948798L;

    // Ordenado: ver la nota de la clase sobre por qué el orden de entrada no cuenta.
    private final Map<String, String> descriptions;
    private final Map<String, OpenType<?>> types;

    // Se calcula una vez porque es inmutable y porque un `CompositeType` se usa como clave de mapa
    // con frecuencia --cada `CompositeDataSupport` consulta el suyo--.
    private transient int hash;

    /**
     * Un tipo compuesto con esos items.
     *
     * <p>Los tres arreglos van en paralelo: el item `i` se llama `itemNames[i]`, se describe con
     * `itemDescriptions[i]` y es de tipo `itemTypes[i]`.
     *
     * @throws OpenDataException si hay nombres repetidos
     * @throws IllegalArgumentException si algún arreglo es nulo o vacío, si no tienen el mismo
     *     largo, o si algún elemento es nulo o una cadena en blanco
     */
    public CompositeType(String typeName, String description, String[] itemNames,
            String[] itemDescriptions, OpenType<?>[] itemTypes) throws OpenDataException {
        super(CompositeData.class.getName(), typeName, description);

        if (itemNames == null || itemDescriptions == null || itemTypes == null) {
            throw new IllegalArgumentException("los arreglos de items no pueden ser nulos");
        }
        if (itemNames.length == 0) {
            throw new IllegalArgumentException("un tipo compuesto necesita al menos un item");
        }
        if (itemNames.length != itemDescriptions.length
                || itemNames.length != itemTypes.length) {
            throw new IllegalArgumentException(
                    "los tres arreglos de items tienen que tener el mismo largo");
        }

        Map<String, String> ds = new TreeMap<String, String>();
        Map<String, OpenType<?>> ts = new TreeMap<String, OpenType<?>>();
        for (int i = 0; i < itemNames.length; i++) {
            String n = itemNames[i];
            if (n == null || n.trim().length() == 0) {
                throw new IllegalArgumentException("el nombre del item " + i + " está en blanco");
            }
            n = n.trim();
            if (itemDescriptions[i] == null || itemDescriptions[i].trim().length() == 0) {
                throw new IllegalArgumentException(
                        "la descripción del item " + n + " está en blanco");
            }
            if (itemTypes[i] == null) {
                throw new IllegalArgumentException("el tipo del item " + n + " es nulo");
            }
            // Repetido es `OpenDataException` y no `IllegalArgumentException`: el JDK lo distingue
            // así, y tiene sentido -- los nombres pueden venir de datos, los nulos son un error de
            // quien llama.
            if (ts.containsKey(n)) {
                throw new OpenDataException("el item " + n + " está repetido");
            }
            ds.put(n, itemDescriptions[i].trim());
            ts.put(n, itemTypes[i]);
        }
        this.descriptions = Collections.unmodifiableMap(ds);
        this.types = Collections.unmodifiableMap(ts);
    }

    /** Si hay un item con ese nombre. Un nulo o una cadena vacía dan `false`, no un error. */
    public boolean containsKey(String itemName) {
        if (itemName == null) {
            return false;
        }
        return this.types.containsKey(itemName);
    }

    /** La descripción de ese item, o nulo si no existe. */
    public String getDescription(String itemName) {
        if (itemName == null) {
            return null;
        }
        return this.descriptions.get(itemName);
    }

    /** El tipo de ese item, o nulo si no existe. */
    public OpenType<?> getType(String itemName) {
        if (itemName == null) {
            return null;
        }
        return this.types.get(itemName);
    }

    /** Los nombres de los items, ordenados y de sólo lectura. */
    public Set<String> keySet() {
        return this.types.keySet();
    }

    /**
     * Si `obj` es un {@link CompositeData} cuyo tipo es éste.
     *
     * <p>Se compara con {@link #equals} y no por identidad a propósito: el dato puede haber llegado
     * de otra máquina con un `CompositeType` reconstruido, y ser el mismo tipo es una cuestión de
     * forma, no de objeto.
     */
    public boolean isValue(Object obj) {
        if (!(obj instanceof CompositeData)) {
            return false;
        }
        return this.equals(((CompositeData) obj).getCompositeType());
    }

    /** Igualdad por nombre de tipo e items; la descripción del tipo no cuenta. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CompositeType)) {
            return false;
        }
        CompositeType other = (CompositeType) obj;
        if (!this.getTypeName().equals(other.getTypeName())) {
            return false;
        }
        if (!this.types.keySet().equals(other.types.keySet())) {
            return false;
        }
        for (Map.Entry<String, OpenType<?>> e : this.types.entrySet()) {
            if (!e.getValue().equals(other.types.get(e.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** El nombre del tipo más los nombres y tipos de los items. Sin el orden. */
    public int hashCode() {
        if (this.hash == 0) {
            int h = this.getTypeName().hashCode();
            for (Map.Entry<String, OpenType<?>> e : this.types.entrySet()) {
                h = h + e.getKey().hashCode() + e.getValue().hashCode();
            }
            this.hash = h;
        }
        return this.hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CompositeType.class.getName());
        sb.append("(name=").append(this.getTypeName()).append(",items=(");
        boolean first = true;
        // Se recorre en orden de nombre, que es como están guardados: dos tipos iguales imprimen
        // igual aunque se hayan construido con los items en otro orden.
        Map<String, OpenType<?>> ordered = new LinkedHashMap<String, OpenType<?>>(this.types);
        for (Map.Entry<String, OpenType<?>> e : ordered.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("(itemName=").append(e.getKey());
            sb.append(",itemType=").append(e.getValue().toString()).append(")");
        }
        sb.append("))");
        return sb.toString();
    }
}
