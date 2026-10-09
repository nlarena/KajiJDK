// La forma original de #285 --llamada a metodo, no `new`-- con las dos variantes de donde se
// declara la variable de tipo. El nuestro rechaza `deLaClase` y acepta `delMetodo`.

import java.util.HashSet;
import java.util.Map;

public final class Finding285Clase<K, V> {

    /** FALLA: variables de la CLASE. Es el repro de #285 tal como esta escrito. */
    void deLaClase(K k, V v) {
        HashSet<Map.Entry<K, V>> out = new HashSet<Map.Entry<K, V>>();
        out.add(Map.entry(k, v));
    }

    /** ANDA: la misma forma con variables del METODO. */
    static <A, B> void delMetodo(A a, B b) {
        HashSet<Map.Entry<A, B>> out = new HashSet<Map.Entry<A, B>>();
        out.add(Map.entry(a, b));
    }
}
