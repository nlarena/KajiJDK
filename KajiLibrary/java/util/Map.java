package java.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

// KajiLibrary's java.util.Map<K,V> — a set of key→value associations with unique keys. Not
// a Collection (its own root). Look up / insert / remove by key, test membership, size, and
// clear. A KajiLibrary subset (the JDK also has keySet/values/entrySet/putAll/getOrDefault/…).
// Concrete: HashMap.
public interface Map<K, V> {

    int size();

    boolean isEmpty();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    V get(Object key);

    V put(K key, V value);

    V remove(Object key);

    void clear();

    // The keys, as a Set (finding #205).
    //
    // Se agrega porque `putAll` la necesita: su argumento llega tipado como la **interfaz** `Map`, y
    // sin una forma de enumerarlo no hay manera de copiarlo. Es API real del JDK, asi que sumarla no
    // aleja a la biblioteca de la referencia — la acerca.
    //
    // **Divergencia deliberada**: la del JDK es una *vista* respaldada por el mapa (quitar del set
    // quita del mapa, y los cambios del mapa se ven en el set). Estas son **copias**. Una vista pide
    // una clase por implementacion que delegue de vuelta, y el uso que la biblioteca le da hoy es
    // recorrer; cuando alguna necesite la vista de verdad, se cambia ahi.
    Set<K> keySet();

    // Copia todos los pares de `m` en este mapa, sobrescribiendo las claves que ya esten (§Map).
    // Abstracto como en el JDK: cada implementacion sabe recorrer lo suyo, y varias pueden hacerlo
    // mas barato que el bucle generico.
    void putAll(Map<? extends K, ? extends V> m);


    // ---- los `default` del JDK 8+ ----------------------------------------------------------
    //
    // Todos se implementan sobre `keySet()`/`get()`/`put()`/`remove()`, que es lo unico que toda
    // implementacion de esta biblioteca tiene hoy. El JDK los escribe sobre `entrySet()`; el
    // resultado observable es el mismo, y el cuerpo de un `default` es interno.

    // El valor de `key`, o `defaultValue` si no esta.
    //
    // La consulta a `containsKey` no es redundante: un mapa que admite valores nulos distingue
    // "mapeada a null" de "ausente", y solo el segundo caso toma el default.
    default V getOrDefault(Object key, V defaultValue) {
        V v = this.get(key);
        if (v != null || this.containsKey(key)) {
            return v;
        }
        return defaultValue;
    }

    // Le pasa cada par a `action`.
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            action.accept(k, this.get(k));
        }
    }

    // Reemplaza cada valor por el que devuelva `function` para su par.
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            this.put(k, function.apply(k, this.get(k)));
        }
    }

    // Asocia `value` a `key` solo si no habia valor; devuelve el que ya estaba, o null.
    default V putIfAbsent(K key, V value) {
        V v = this.get(key);
        if (v == null) {
            v = this.put(key, value);
        }
        return v;
    }

    // Quita el par solo si la clave esta mapeada **a ese valor**.
    default boolean remove(Object key, Object value) {
        V cur = this.get(key);
        if (cur == null && !this.containsKey(key)) {
            return false;
        }
        if (cur == null) {
            if (value != null) {
                return false;
            }
        } else if (!cur.equals(value)) {
            return false;
        }
        this.remove(key);
        return true;
    }

    // Reemplaza el valor solo si el actual es `oldValue`.
    default boolean replace(K key, V oldValue, V newValue) {
        V cur = this.get(key);
        if (cur == null && !this.containsKey(key)) {
            return false;
        }
        if (cur == null) {
            if (oldValue != null) {
                return false;
            }
        } else if (!cur.equals(oldValue)) {
            return false;
        }
        this.put(key, newValue);
        return true;
    }

    // Reemplaza el valor solo si la clave ya estaba mapeada.
    default V replace(K key, V value) {
        V cur = this.get(key);
        if (cur != null || this.containsKey(key)) {
            return this.put(key, value);
        }
        return cur;
    }

    // El valor de `key`; si no hay, lo calcula con `mappingFunction` y lo guarda.
    //
    // Un resultado null NO se guarda: el contrato es "queda mapeada o no queda nada", y guardar
    // null dejaria una entrada que `getOrDefault` no puede distinguir de una ausencia.
    default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V v = this.get(key);
        if (v != null) {
            return v;
        }
        V nuevo = mappingFunction.apply(key);
        if (nuevo != null) {
            this.put(key, nuevo);
        }
        return nuevo;
    }

    // Recalcula el valor de `key` **solo si ya estaba**. Un resultado null **borra** la entrada.
    default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> f) {
        V v = this.get(key);
        if (v == null) {
            return null;
        }
        V nuevo = f.apply(key, v);
        if (nuevo != null) {
            this.put(key, nuevo);
            return nuevo;
        }
        this.remove(key);
        return null;
    }

    // Recalcula el valor de `key`, este o no. Un resultado null borra la entrada (o no crea nada).
    default V compute(K key, BiFunction<? super K, ? super V, ? extends V> f) {
        V v = this.get(key);
        V nuevo = f.apply(key, v);
        if (nuevo == null) {
            if (v != null || this.containsKey(key)) {
                this.remove(key);
            }
            return null;
        }
        this.put(key, nuevo);
        return nuevo;
    }

    // Si no hay valor, guarda `value`; si lo hay, guarda lo que devuelva `f` sobre los dos. Un
    // resultado null borra la entrada. Es la operacion de acumulacion: contar, sumar, concatenar.
    default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> f) {
        if (value == null) {
            throw new NullPointerException();
        }
        V v = this.get(key);
        V nuevo;
        if (v == null) {
            nuevo = value;
        } else {
            nuevo = f.apply(v, value);
        }
        if (nuevo == null) {
            this.remove(key);
        } else {
            this.put(key, nuevo);
        }
        return nuevo;
    }

    // ---- las factorias inmutables (JDK 9+) --------------------------------------------------
    //
    // Devuelven un mapa **inmutable**, que rechaza claves y valores nulos y las claves repetidas.
    // Ese rechazo es del contrato, no una decision nuestra: `Map.of("a", 1, "a", 2)` es un
    // IllegalArgumentException en el JDK, y tragarselo taparia un bug del literal.

    static <K, V> Map<K, V> of() {
        return FixedMap.fromPairs(new Object[0], 0);
    }

    static <K, V> Map<K, V> of(K k1, V v1) {
        Object[] kv = new Object[2];
        kv[0] = k1; kv[1] = v1;
        return FixedMap.fromPairs(kv, 2);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Object[] kv = new Object[4];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2;
        return FixedMap.fromPairs(kv, 4);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Object[] kv = new Object[6];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        return FixedMap.fromPairs(kv, 6);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Object[] kv = new Object[8];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2;
        kv[4] = k3; kv[5] = v3; kv[6] = k4; kv[7] = v4;
        return FixedMap.fromPairs(kv, 8);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Object[] kv = new Object[10];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5;
        return FixedMap.fromPairs(kv, 10);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6) {
        Object[] kv = new Object[12];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        return FixedMap.fromPairs(kv, 12);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6, K k7, V v7) {
        Object[] kv = new Object[14];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        kv[12] = k7; kv[13] = v7;
        return FixedMap.fromPairs(kv, 14);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6, K k7, V v7, K k8, V v8) {
        Object[] kv = new Object[16];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        kv[12] = k7; kv[13] = v7; kv[14] = k8; kv[15] = v8;
        return FixedMap.fromPairs(kv, 16);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        Object[] kv = new Object[18];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        kv[12] = k7; kv[13] = v7; kv[14] = k8; kv[15] = v8; kv[16] = k9; kv[17] = v9;
        return FixedMap.fromPairs(kv, 18);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        Object[] kv = new Object[20];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        kv[12] = k7; kv[13] = v7; kv[14] = k8; kv[15] = v8; kv[16] = k9; kv[17] = v9;
        kv[18] = k10; kv[19] = v10;
        return FixedMap.fromPairs(kv, 20);
    }

    // Un par inmutable suelto, para armar `ofEntries`.
    static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new FixedEntry<K, V>(k, v);
    }

    // El mapa de los pares dados.
    static <K, V> Map<K, V> ofEntries(Entry<? extends K, ? extends V>... entries) {
        Object[] kv = new Object[entries.length * 2];
        int i = 0;
        while (i < entries.length) {
            Entry<? extends K, ? extends V> e = entries[i];
            kv[i * 2] = e.getKey();
            kv[i * 2 + 1] = e.getValue();
            i = i + 1;
        }
        return FixedMap.fromPairs(kv, kv.length);
    }

    // Una copia inmutable de `map`. Se saca en el momento: cambios posteriores del original no
    // se ven.
    static <K, V> Map<K, V> copyOf(Map<? extends K, ? extends V> map) {
        Object[] kv = new Object[map.size() * 2];
        int i = 0;
        Iterator<? extends K> it = map.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            kv[i] = k;
            kv[i + 1] = map.get(k);
            i = i + 2;
        }
        return FixedMap.fromPairs(kv, kv.length);
    }

    // A single key→value association — the unit an entry-oriented view hands back. Nested
    // in Map exactly as in the JDK (java.util.Map.Entry).
    interface Entry<K, V> {

        K getKey();

        V getValue();

        V setValue(V value);
    }
    // Los valores de este mapa. Collection y no Set: los valores pueden repetirse.
    Collection<V> values();

    // Los pares de este mapa.
    Set<Entry<K, V>> entrySet();

}
