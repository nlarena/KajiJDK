// Repro de #288 - referencia stale que llega al GC: `young_info[&obj]` sin la clave.
//
//   javac --emit -cp KajiLibrary KajiLibraryeprosinding_288.java
//   run-headless KajiLibraryeprosinding_288.class run
//
//   thread 'main' panicked at src\jvm\interpreter\gc.rs:189:42:
//   no entry found for key
//
// `gc.rs:189` es `evacuate`, en `let (size, age) = self.young_info[&obj];`: el colector recibio
// un offset que no corresponde a ningun objeto joven conocido. O sea, una referencia que
// sobrevivio a una coleccion anterior sin actualizarse, o que nunca fue un objeto.
//
// DETERMINISTA: falla 3 de 3, siempre en la misma linea. En modo verde (un solo hilo), asi que
// no es la carrera de os-parallel.
//
// NO es del codigo nuevo de colecciones, y esto es lo importante: el mismo programa revienta
// igual con `bin/run-headless.exe` congelado, que es ANTERIOR a todos los cambios de esta tanda.
// Es un defecto preexistente que aparecio al tener por fin suficiente API como para escribir un
// programa que alocara lo bastante.
//
// El lugar del crash NO es el lugar del bug. Cada pedazo de este archivo corre bien por separado:
//
//   - las factorias solas                        -> anda
//   - el bloque de defaults de Map solo          -> anda (da 96, igual que java real)
//   - HashMap.put/remove/values por separado     -> anda
//   - 400 HashMap con entrySet()/values()        -> anda
//
// Lo que hace falta es el VOLUMEN acumulado: la reduccion automatica encontro que el primer
// prefijo que falla corta justo en `m.values()`, despues de dos `remove(k, v)`. Esa linea no es
// la culpable; es donde el GC toca por primera vez con la referencia ya podrida.
//
// Para el que lo agarre: el sospechoso es algo que guarda un offset del heap a traves de una
// coleccion sin estar en las raices. Es la misma familia que el hueco de `pending_exception`
// (arreglado con parking en la pila de operandos) y que el bug de os-parallel; la diferencia es
// que este se reproduce en verde y a la primera.
import java.util.*;
import java.util.function.*;

public class finding_288 {
    public static int run() {
        int r = 0;
        // ---- factorias inmutables ------------------------------------------------------------
        List<String> lf = List.of("p", "q", "r");
        r = r + lf.size() * 10000000;                    // 30000000
        try {
            lf.add("s");
            r = r + 7777;
        } catch (UnsupportedOperationException ex) {
            r = r + 100000000;
        }

        Set<String> sf = Set.of("p", "q");
        r = r + sf.size();                               // 2
        r = r + (sf.contains("q") ? 10 : 0);
        try {
            Set.of("p", "p");                            // repetido -> IllegalArgumentException
            r = r + 7777;
        } catch (IllegalArgumentException ex) {
            r = r + 100;
        }

        Map<String, Integer> mf = Map.of("a", 1, "b", 2);
        r = r + mf.size() * 1000;                        // 2000
        r = r + mf.get("b").intValue() * 10000;          // 20000

        // ---- defaults de Map -----------------------------------------------------------------
        HashMap<String, Integer> m = new HashMap<String, Integer>();
        m.put("k", 5);
        r = r + m.getOrDefault("k", 0).intValue();       // 5
        r = r + m.getOrDefault("nope", 3).intValue();    // 3
        r = r + m.putIfAbsent("k", 9).intValue();        // 5, no pisa
        m.putIfAbsent("n", 4);                           // entra
        r = r + m.get("n").intValue();                   // 4

        java.util.function.BiFunction<Integer, Integer, Integer> suma =
            (v1, v2) -> Integer.valueOf(v1.intValue() + v2.intValue());
        m.merge("k", 10, suma);
        r = r + m.get("k").intValue();                   // 15

        java.util.function.Function<String, Integer> siete = key -> Integer.valueOf(7);
        m.computeIfAbsent("c", siete);
        r = r + m.get("c").intValue();                   // 7
        java.util.function.BiFunction<String, Integer, Integer> doble =
            (key, v) -> Integer.valueOf(v.intValue() * 2);
        m.computeIfPresent("c", doble);
        r = r + m.get("c").intValue();                   // 14
        java.util.function.BiFunction<String, Integer, Integer> masUno =
            (key, v) -> Integer.valueOf(v.intValue() + 1);
        m.compute("c", masUno);
        r = r + m.get("c").intValue();                   // 15

        r = r + (m.replace("c", Integer.valueOf(15), Integer.valueOf(20)) ? 1 : 0);
        r = r + m.get("c").intValue();                   // 20
        r = r + (m.remove("c", Integer.valueOf(99)) ? 7777 : 0);   // valor distinto: no borra
        r = r + (m.remove("c", Integer.valueOf(20)) ? 1 : 0);      // si borra

        r = r + m.values().size();                       // k, n -> 2
        r = r + m.entrySet().size();                     // 2
        r = r + m.keySet().size();                       // 2

        return r;
    }
}
