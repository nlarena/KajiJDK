package javax.script;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * KajiLibrary's javax.script.SimpleBindings -- el {@link Bindings} de todos los dias.
 *
 * <p>Es un {@link Map} de respaldo mas una guarda. Toda la logica esta en `checkKey`, que corre
 * antes de cada operacion que toca una clave y decide entre tres errores distintos:
 *
 * <ul>
 *   <li>clave nula -&gt; {@link NullPointerException}, "key can not be null"
 *   <li>clave que no es {@code String} -&gt; {@link ClassCastException}, "key should be a String"
 *   <li>clave vacia -&gt; {@link IllegalArgumentException}, "key can not be empty"
 * </ul>
 *
 * <p>El orden importa y es ese: nulo antes que tipo, tipo antes que vacio. Y la guarda esta
 * tambien en los metodos de **lectura** ({@code get}, {@code containsKey}, {@code remove}), que no
 * es lo habitual en un mapa -- un `HashMap` acepta cualquier clave y devuelve nulo. Aca no: si la
 * clave no puede ser un nombre de variable, preguntar por ella es un error del que pregunta, no un
 * "no esta".
 *
 * <p>Hay un detalle del constructor con mapa que conviene tener presente: **no copia**. Se queda
 * con la referencia, asi que lo que se le meta al mapa por afuera aparece aca, incluso saltandose
 * la guarda de claves. Es a proposito -- permite envolver un mapa que ya existe -- pero significa
 * que las reglas de arriba valen para lo que entra *por esta clase*, no para lo que ya estaba.
 *
 * <p>Esta clase no redefine `equals`, `hashCode` ni `toString`, igual que el original: dos
 * `SimpleBindings` con el mismo contenido no son iguales. No es un descuido nuestro; es lo que
 * hace la implementacion de referencia y hay codigo que depende de la identidad.
 */
public class SimpleBindings implements Bindings {

    /** El mapa de respaldo. Se guarda por referencia, no se copia. */
    private final Map<String, Object> map;

    /**
     * Envuelve `m`, sin copiarlo.
     *
     * @throws NullPointerException si `m` es nulo
     */
    public SimpleBindings(Map<String, Object> m) {
        if (m == null) {
            throw new NullPointerException();
        }
        this.map = m;
    }

    /** Con un {@link HashMap} vacio de respaldo. */
    public SimpleBindings() {
        this(new HashMap<String, Object>());
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException si `name` es nulo
     * @throws IllegalArgumentException si `name` es vacio
     */
    @Override
    public Object put(String name, Object value) {
        checkKey(name);
        return map.put(name, value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Se valida clave por clave a medida que se copian, asi que un mapa con una clave mala
     * puede dejar copiadas las que venian antes. Es lo mismo que hace el original.
     *
     * @throws NullPointerException si `toMerge` es nulo, o si alguna clave lo es
     * @throws IllegalArgumentException si alguna clave es vacia
     */
    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        Objects.requireNonNull(toMerge, "toMerge map is null");
        for (Map.Entry<? extends String, ? extends Object> entry : toMerge.entrySet()) {
            String key = entry.getKey();
            checkKey(key);
            put(key, entry.getValue());
        }
    }

    /** Vacia el mapa. */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException si `key` es nulo
     * @throws ClassCastException si `key` no es un `String`
     * @throws IllegalArgumentException si `key` es vacio
     */
    @Override
    public boolean containsKey(Object key) {
        checkKey(key);
        return map.containsKey(key);
    }

    /** Si algun valor es igual a `value`. Sobre los valores no hay ninguna regla. */
    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /** Las entradas del mapa de respaldo, en vivo. */
    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException si `key` es nulo
     * @throws ClassCastException si `key` no es un `String`
     * @throws IllegalArgumentException si `key` es vacio
     */
    @Override
    public Object get(Object key) {
        checkKey(key);
        return map.get(key);
    }

    /** Si no hay ninguna entrada. */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /** Las claves del mapa de respaldo, en vivo. */
    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException si `key` es nulo
     * @throws ClassCastException si `key` no es un `String`
     * @throws IllegalArgumentException si `key` es vacio
     */
    @Override
    public Object remove(Object key) {
        checkKey(key);
        return map.remove(key);
    }

    /** Cuantas entradas hay. */
    @Override
    public int size() {
        return map.size();
    }

    /** Los valores del mapa de respaldo, en vivo. */
    @Override
    public Collection<Object> values() {
        return map.values();
    }

    /**
     * La guarda: nulo, despues tipo, despues vacio. Ese orden es parte del contrato observable.
     */
    private void checkKey(Object key) {
        if (key == null) {
            throw new NullPointerException("key can not be null");
        }
        if (!(key instanceof String)) {
            throw new ClassCastException("key should be a String");
        }
        if (((String) key).isEmpty()) {
            throw new IllegalArgumentException("key can not be empty");
        }
    }
}
