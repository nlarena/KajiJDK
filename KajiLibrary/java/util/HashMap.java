package java.util;

// Compiled with `-cp KajiLibrary` so Map binds to KajiLibrary's own (subset) type.
import java.util.Map;

// KajiLibrary's java.util.HashMap<K,V> — a hash table keyed by `hashCode`/`equals`. This
// implementation uses open addressing with linear probing over two parallel Object[] arrays
// (keys and values), doubling and rehashing past a ~50% load factor. A `null` slot in `keys`
// marks an empty bucket; removal re-inserts the trailing cluster to preserve the probe
// invariant. (Null keys are not supported, unlike the JDK.) Map has no iteration in our subset,
// so no helper class is needed.
public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

    private Object[] keys;
    private Object[] values;
    private int size;

    public HashMap() {
        this.keys = new Object[16];
        this.values = new Object[16];
        this.size = 0;
    }

    /**
     * Un mapa vacio con lugar para `initialCapacity` cubetas.
     *
     * <p>Sirve para lo de siempre: si se sabe cuantos pares van a entrar, dimensionar de entrada
     * evita las rehashes del crecimiento. Ojo con el nombre --y es el mismo malentendido que en el
     * JDK--: `initialCapacity` es la cantidad de **cubetas**, no de pares. Con el factor de carga de
     * esta implementacion (~50 %) entran aproximadamente la mitad antes de la primera rehash. El que
     * quiere pensar en pares tiene `newHashMap`.
     *
     * @throws IllegalArgumentException si la capacidad es negativa
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Idem, con factor de carga.
     *
     * <p>El `loadFactor` se **valida y se ignora**, y conviene decirlo de frente: esta tabla usa
     * direccionamiento abierto con sondeo lineal y duplica pasado ~50 %, un umbral que es parte de
     * como esta escrita y no un parametro. Aceptar el valor y no usarlo seria mentir; rechazarlo
     * seria romper codigo que compila contra el JDK y solo pasa el 0.75 de siempre. Se valida
     * --un factor no positivo o NaN es un error, igual que en el JDK-- y despues se descarta, que es
     * la unica de las tres opciones que no le miente a nadie.
     *
     * @throws IllegalArgumentException si la capacidad es negativa o el factor no es positivo
     */
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (!(loadFactor > 0)) {   // negado, para que NaN caiga aca
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        int cap = 16;
        while (cap < initialCapacity) {
            cap = cap * 2;
        }
        this.keys = new Object[cap];
        this.values = new Object[cap];
        this.size = 0;
    }

    /**
     * Un mapa dimensionado para `numMappings` **pares**, sin rehashes.
     *
     * <p>Es el que la gente queria cuando escribia `new HashMap<>(n)`: aquel toma cubetas y este
     * toma pares. Java 19 lo agrego justamente porque el otro se usaba mal.
     *
     * @throws IllegalArgumentException si `numMappings` es negativo
     */
    public static <K, V> HashMap<K, V> newHashMap(int numMappings) {
        if (numMappings < 0) {
            throw new IllegalArgumentException("Negative number of mappings: " + numMappings);
        }
        return new HashMap<K, V>(numMappings * 2 + 1);
    }

    // Copia los pares de otro mapa. El de siempre para quedarse con una foto de un mapa ajeno.
    public HashMap(Map<? extends K, ? extends V> m) {
        this.keys = new Object[16];
        this.values = new Object[16];
        this.size = 0;
        this.putAll(m);
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    // The index of `key`'s bucket: the slot holding it, or the first empty slot on its
    // probe sequence if absent.
    private int slotFor(Object key) {
        int cap = this.keys.length;
        int i = key.hashCode() & (cap - 1);
        while (this.keys[i] != null) {
            if (this.keys[i].equals(key)) {
                return i;
            }
            i = (i + 1) & (cap - 1);
        }
        return i;
    }

    public V get(Object key) {
        return (V) this.values[this.slotFor(key)];
    }

    public boolean containsKey(Object key) {
        return this.keys[this.slotFor(key)] != null;
    }

    // Direccionamiento abierto: las claves vivas son los slots no nulos de `keys` (finding #205).
    public Set<K> keySet() {
        HashSet<K> out = new HashSet<K>();
        int i = 0;
        while (i < this.keys.length) {
            if (this.keys[i] != null) {
                out.add((K) this.keys[i]);
            }
            i = i + 1;
        }
        return out;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        Iterator<? extends K> it = m.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            this.put(k, m.get(k));
        }
    }

    public V put(K key, V value) {
        if (this.size * 2 >= this.keys.length) {
            this.resize();
        }
        int i = this.slotFor(key);
        V old = (V) this.values[i];
        if (this.keys[i] == null) {
            this.size = this.size + 1;
        }
        this.keys[i] = key;
        this.values[i] = value;
        return old;
    }

    public V remove(Object key) {
        int cap = this.keys.length;
        int i = this.slotFor(key);
        if (this.keys[i] == null) {
            return null;
        }
        V old = (V) this.values[i];
        this.keys[i] = null;
        this.values[i] = null;
        this.size = this.size - 1;
        // Re-insert the rest of this probe cluster so no lookup is cut short.
        int j = (i + 1) & (cap - 1);
        while (this.keys[j] != null) {
            K k = (K) this.keys[j];
            V v = (V) this.values[j];
            this.keys[j] = null;
            this.values[j] = null;
            this.size = this.size - 1;
            this.put(k, v);
            j = (j + 1) & (cap - 1);
        }
        return old;
    }

    public boolean containsValue(Object value) {
        for (int i = 0; i < this.values.length; i++) {
            if (this.keys[i] != null) {
                Object v = this.values[i];
                if (value == null) {
                    if (v == null) {
                        return true;
                    }
                } else {
                    if (value.equals(v)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void clear() {
        for (int i = 0; i < this.keys.length; i++) {
            this.keys[i] = null;
            this.values[i] = null;
        }
        this.size = 0;
    }

    // Double the table and re-insert every live entry into the fresh, larger arrays.
    private void resize() {
        Object[] oldKeys = this.keys;
        Object[] oldValues = this.values;
        int newCap = oldKeys.length * 2;
        this.keys = new Object[newCap];
        this.values = new Object[newCap];
        this.size = 0;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != null) {
                this.put((K) oldKeys[i], (V) oldValues[i]);
            }
        }
    }

    /**
     * Los valores de este mapa.
     *
     * <p>**Divergencia deliberada**, la misma que ya declara `keySet()`: la del JDK es una *vista*
     * respaldada por el mapa; esta es una copia sacada en el momento. Y a diferencia de `keySet()`
     * es una `Collection` y no un `Set`, porque los valores **si** pueden repetirse.
     */
    public java.util.Collection<V> values() {
        java.util.ArrayList<V> out = new java.util.ArrayList<V>();
        java.util.Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            out.add(this.get(it.next()));
        }
        return out;
    }

    /**
     * Los pares de este mapa.
     *
     * <p>Misma divergencia que `values()`: copia, no vista. Los pares que devuelve son inmutables,
     * asi que `setValue` sobre uno de ellos lanza en vez de escribir en el mapa — que es lo
     * coherente con que sea una copia: escribir en un par que nadie mira seria peor que negarse.
     */
    public java.util.Set<java.util.Map.Entry<K, V>> entrySet() {
        java.util.HashSet<java.util.Map.Entry<K, V>> out =
            new java.util.HashSet<java.util.Map.Entry<K, V>>();
        java.util.Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            out.add(new FixedEntry<K, V>(k, this.get(k)));
        }
        return out;
    }
}
