// Repro de #248 - una llamada generica estatica no resuelve cuando la inferencia tiene que bajar
// a una variable de tipo ANIDADA dentro de un argumento de tipo. SIGUE ABIERTO.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_248.java
//
// La forma: `Function<T, Stream<U>>` — la `U` esta anidada dentro de `Stream<U>`, y eso es lo que
// rompe la inferencia. Es la firma de `Collectors.flatMapping`. Declarar el parametro como lo
// declara el JDK (`Function<T, ? extends Stream<? extends U>>`) lo hace resolver, que es el
// control `tomaComodin` de abajo.
//
// EL SINTOMA CAMBIO, y conviene tenerlo escrito porque el reporte viejo ya no describe lo que
// pasa. Antes compilaba **mudo** y dejaba el call site mal armado, sin el `invokestatic`:
//
//   15: aload_1     // mapper, colgado en la pila
//   16: aload_0     // downstream
//   17: astore_2    // c = downstream  (!!)
//
// Hoy falla fuerte, con un diagnostico que nombra el problema:
//
//   error: no se encontro un metodo `tomaInvariante(Function<String, Stream<Integer>>, Integer)`
//          aplicable
//     metodo finding_248.tomaInvariante(Function<T, Stream<U>>, A) no es aplicable
//       (los argumentos no coinciden: Function<String, Stream<Integer>> no se convierte a
//        Function<T, Stream<U>>)
//
// Es una mejora —"no compila" es mejor que "compila y revienta lejos"— pero el defecto es el
// mismo y sigue abierto: la inferencia no liga `T` ni `U` a traves del anidamiento. El javac real
// compila este archivo sin chistar.
//
// Objetivo cuando se arregle: `invariante()` y `conComodin()` emiten ambas `invokestatic` y
// devuelven 7.
import java.util.function.Function;
import java.util.stream.Stream;

public class finding_248 {

    // La U esta anidada dentro de Stream<U>: eso es lo que rompia la inferencia.
    static <T, U, A> A tomaInvariante(Function<T, Stream<U>> mapper, A downstream) {
        return downstream;
    }

    // La misma forma pero con comodines, como la declara el JDK.
    static <T, U, A> A tomaComodin(Function<T, ? extends Stream<? extends U>> mapper, A downstream) {
        return downstream;
    }

    public static int invariante() {
        Function<String, Stream<Integer>> f = null;
        Integer c = tomaInvariante(f, Integer.valueOf(7));
        return c.intValue();
    }

    public static int conComodin() {
        Function<String, Stream<Integer>> f = null;
        Integer c = tomaComodin(f, Integer.valueOf(7));
        return c.intValue();
    }
}
