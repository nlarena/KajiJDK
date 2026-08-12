// Finding #16 — una lambda como argumento de un constructor genérico no la soporta el codegen.
// Cuando el tipo target (la interfaz funcional) de la lambda se infiere *a través* de los
// parámetros de tipo del constructor, el generador de bytecode no puede emitir el `invokedynamic`.
// La misma lambda asignada a una local con tipo explícito compila (y las lambdas andan en todos
// lados). Falta resolver el tipo target de la lambda desde la inferencia del constructor genérico.
//
// Esperado (javac real): OK.
// Síntoma del bug:       "el generador de bytecode todavía no soporta una expresión lambda
//                        (necesita `invokedynamic`)".
// Workaround: subir la lambda a una local tipada — `Supplier<long[]> s = () -> new long[1];` y
//             pasar `s`. Surgió escribiendo `Collectors`.
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_16.java
import java.util.function.Supplier;

public class Finding16 {

    // Un "contenedor" genérico cuyo constructor toma un Supplier<A>.
    static final class Box<A> {
        Box(Supplier<A> supplier) {}
    }

    // La lambda debe inferir su target (Supplier<long[]>) a través del type-param A del Box.
    public static Box<long[]> make() {
        return new Box<long[]>(() -> new long[1]);
    }
}
