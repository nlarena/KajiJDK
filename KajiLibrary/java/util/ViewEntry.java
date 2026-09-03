package java.util;

// El par que devuelven las vistas `entrySet()` de los mapas: inmutable y **tolerante al nulo**.
//
// Existe por un bug concreto: antes las vistas usaban `FixedEntry`, que rechaza nulos en su
// constructor. Eso es correcto para `Map.entry(k, v)` --el JDK tambien los rechaza ahi-- pero es
// falso para una vista: un `HashMap` o un `TreeMap` PUEDEN tener un valor nulo, y con `FixedEntry`
// pedirles `entrySet()` tiraba `NullPointerException` en vez de devolver el par. El sintoma era
// desconcertante porque la excepcion no salia de donde estaba el nulo sino de recorrer el mapa.
//
// La clave si tiene que ser no nula en un `TreeMap`, pero no es asunto de esta clase: quien lo
// exige es el mapa, al poner. Aca se acepta lo que el mapa haya guardado.
//
// `setValue` lanza, como en `FixedEntry`: estas vistas son copias, no ventanas, asi que escribir en
// un par no llegaria al mapa y quedarse callado seria peor que negarse.
final class ViewEntry<K, V> implements Map.Entry<K, V> {

    private final K key;
    private final V value;

    ViewEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    // Dos pares son iguales cuando lo son sus dos mitades (§Map.Entry). Con nulos: dos nulos son
    // iguales entre si, que es lo que dice el contrato y lo que `FixedEntry` no podia expresar.
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
        return same(this.key, other.getKey()) && same(this.value, other.getValue());
    }

    private static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    // key.hashCode() ^ value.hashCode(), con cero para el nulo: es exactamente lo que especifica
    // `Map.Entry.hashCode()`.
    public int hashCode() {
        int k = this.key == null ? 0 : this.key.hashCode();
        int v = this.value == null ? 0 : this.value.hashCode();
        return k ^ v;
    }

    public String toString() {
        return this.key + "=" + this.value;
    }
}
