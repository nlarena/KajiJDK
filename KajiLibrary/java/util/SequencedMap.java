package java.util;


// A map with a defined encounter order over its entries (Java 21): LinkedHashMap, or a sorted
// map. It can be asked for its first and last entry, and reversed as a view.
//
// The entry operations are defaults so an implementation only has to provide reversed() plus
// the ordinary Map methods.
public interface SequencedMap<K, V> extends Map<K, V> {

    SequencedMap<K, V> reversed();

    /**
     * Las claves como conjunto secuenciado.
     *
     * <p>Los tres `sequenced*` son `default` **y lanzan**, igual que `firstEntry` y compania de mas
     * abajo. La razon es la misma: construir la vista pide saber recorrer el mapa, y esta interfaz no
     * tiene con que -- nuestro `Map` no expone `keySet`/`values`/`entrySet`. Cada implementacion que
     * si sabe los sobreescribe; `LinkedHashMap` lo hace, y sus vistas son vistas de verdad.
     *
     * <p>Lanzar es lo unico honesto que puede hacer una interfaz que no puede cumplir: devolver un
     * conjunto vacio compilaria y mentiria.
     */
    default SequencedSet<K> sequencedKeySet() {
        throw new UnsupportedOperationException();
    }

    default SequencedCollection<V> sequencedValues() {
        throw new UnsupportedOperationException();
    }

    default SequencedSet<Map.Entry<K, V>> sequencedEntrySet() {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> firstEntry() {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> lastEntry() {
        throw new UnsupportedOperationException();
    }

    // Remove and return the first entry, or null if the map is empty.
    default Map.Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    // Put the mapping at the front of the encounter order.
    default V putFirst(K k, V v) {
        throw new UnsupportedOperationException();
    }

    default V putLast(K k, V v) {
        throw new UnsupportedOperationException();
    }
}
