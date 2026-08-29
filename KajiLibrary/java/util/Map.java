package java.util;

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

    // A single key→value association — the unit an entry-oriented view hands back. Nested
    // in Map exactly as in the JDK (java.util.Map.Entry).
    interface Entry<K, V> {

        K getKey();

        V getValue();

        V setValue(V value);
    }
}
