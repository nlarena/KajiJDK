// Finding #15 — un método heredado de una superinterfaz GENÉRICA no resuelve.
// `BinaryOperator<T>.apply(a,b)`: `apply` está declarado en `BiFunction<T,U,R>` y se hereda vía
// `BinaryOperator extends BiFunction<T,T,T>`. Un método heredado de una superinterfaz NO genérica
// sí resuelve (p.ej. `Collection.size()` sobre un `List`). Lo que falta es sustituir los argumentos
// de tipo de la superinterfaz (`BiFunction<T,U,R>` → `<T,T,T>`) al buscar el miembro.
//
// Esperado (javac real): OK.
// Síntoma del bug:       "no se encuentra el método: apply".
// Workaround: ensanchar a la superinterfaz declarante — `BiFunction<T,T,T> op = binOp; op.apply(...)`.
// Surgió escribiendo `Stream.reduce(BinaryOperator)`.
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_15.java
import java.util.function.BinaryOperator;

public class Finding15 {
    public static Integer reduce(BinaryOperator<Integer> op, Integer a, Integer b) {
        return op.apply(a, b);
    }
}
