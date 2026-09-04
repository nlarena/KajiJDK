// REPRO (java.util.stream, 2026-09-01) — una llamada NO SE RESUELVE cuando el parametro del
// metodo mete una variable de tipo dentro de un argumento de tipo INVARIANTE.
//
//   ../bin/javac.exe --emit -cp ../KajiLibrary java/WcLib3.java
//   ../bin/javac.exe --emit -cp "../KajiLibrary;java" java/WcUse3.java
//      -> "el generador de bytecode todavia no soporta una llamada que no resolvio a ningun metodo"
//
// Las cinco fabricas de abajo, llamadas cada una con un argumento cuyo tipo estatico es
// exactamente el parametro despues de sustituir:
//
//   c1  Supplier<Down3<T>>                 FALLA   (anidado invariante)
//   c2  Supplier<? extends Down3<T>>       ok      (el comodin lo salva)
//   c3  BiConsumer<Object, Down3<R>>       FALLA   (una sola variable, igual falla)
//   c4  BiConsumer<A, Down3<R>>            FALLA
//   c5  BiConsumer<A, ? extends Down3<R>>  ok
//
// Lo que separa las que andan de las que no es el comodin en la posicion anidada, no la cantidad
// de variables ni que el tipo anidado sea de primer nivel o miembro. Un witness explicito de tipos
// no ayuda. Es la misma familia que la nota vieja de `Stream.mapMulti`, pero aca es error duro.
//
// A quien le pega hoy: las cuatro fabricas de java.util.stream.Gatherer que reciben un finalizador
// `BiConsumer<A, Downstream<? super R>>` (el comodin esta adentro de Downstream, no en la posicion
// anidada, asi que no salva) quedan declaradas y no se pueden llamar. Por eso
// java.util.stream.Gatherers arma sus GathererImpl con el constructor, donde los argumentos de
// tipo se escriben en vez de inferirse. En cambio java.util.stream.StreamSupport si se puede
// llamar entera: sus fabricas diferidas declaran `Supplier<? extends Spliterator<T>>`, que es el
// caso c2 (sonda: java/SsProbe.java).
import java.util.function.BiConsumer;
import java.util.function.Supplier;
interface Down3<T> { boolean push(T t); }
public class WcLib3 {
    static <T> String c1(Supplier<Down3<T>> g) { return "c1"; }
    static <T> String c2(Supplier<? extends Down3<T>> g) { return "c2"; }
    static <R> String c3(BiConsumer<Object, Down3<R>> g) { return "c3"; }
    static <A, R> String c4(BiConsumer<A, Down3<R>> g) { return "c4"; }
    static <A, R> String c5(BiConsumer<A, ? extends Down3<R>> g) { return "c5"; }
}
