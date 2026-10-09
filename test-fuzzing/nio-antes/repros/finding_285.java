// Repro de #285 - una llamada generica ANIDADA como argumento no resuelve cuando el parametro
// destino es un tipo generico parametrizado por variables de tipo.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_285.java
//   error: el generador de bytecode todavia no soporta una llamada que no resolvio a ningun metodo
//           out.add(Map.entry(k, v));
//           ^
//
// El caret apunta al `add`, no al `entry`: el argumento no se pudo tipar, asi que ningun
// candidato de `add` resulto aplicable. El JDK 25 compila las cinco formas de abajo.
//
// Lo que decide si falla es el TIPO DEL PARAMETRO DESTINO, no la llamada anidada:
//
//   destino `String`             + generico anidado  -> compila
//   destino `V` (variable)       + generico anidado  -> compila
//   destino `Map.Entry<K, V>`    + generico anidado  -> FALLA
//
// O sea: hace falta que el destino sea un tipo generico **parametrizado por variables de tipo**.
// Con el mismo destino y un local tipado en medio anda, que es el rodeo:
//
//   Map.Entry<K, V> e = Map.entry(k, v);
//   out.add(e);                              // compila
//
// Nombrar el tipo es exactamente lo que la inferencia no dedujo — el mismo rodeo que #279, pero
// no es #279: aquel es una AMBIGUEDAD entre sobrecargas con un argumento `T[]`, y `add` tiene un
// solo candidato. Aca no hay nada que desambiguar; la inferencia del argumento no llega a
// producir un tipo.
//
// El import de Map es necesario: con el nombre completamente calificado en posicion de
// expresion (`java.util.Map.entry(...)`) lo que salta es #274, que es otro defecto.
//
// Salio agregandole `entrySet()` a las implementaciones de Map de fuera de java.util, que tienen
// que construir sus pares con `Map.entry` porque `FixedEntry` es package-private.
import java.util.Map;

public class finding_285<K, V> {

    // El caso que falla.
    public java.util.Set<java.util.Map.Entry<K, V>> falla(K k, V v) {
        java.util.HashSet<java.util.Map.Entry<K, V>> out =
            new java.util.HashSet<java.util.Map.Entry<K, V>>();
        out.add(Map.entry(k, v));
        return out;
    }

    // El rodeo: el mismo codigo con el tipo nombrado en un local.
    public java.util.Set<java.util.Map.Entry<K, V>> rodeo(K k, V v) {
        java.util.HashSet<java.util.Map.Entry<K, V>> out =
            new java.util.HashSet<java.util.Map.Entry<K, V>>();
        java.util.Map.Entry<K, V> e = Map.entry(k, v);
        out.add(e);
        return out;
    }

    static <T> T id(T t) {
        return t;
    }

    // Control: destino concreto -> compila.
    public java.util.Set<String> concreto() {
        java.util.HashSet<String> s = new java.util.HashSet<String>();
        s.add(id("x"));
        return s;
    }

    // Control: destino variable de tipo suelta -> compila.
    public java.util.Set<V> variable(V v) {
        java.util.HashSet<V> s = new java.util.HashSet<V>();
        s.add(id(v));
        return s;
    }
}
