// Repro de #248 - una llamada generica estatica se descarta en silencio cuando la inferencia
// tiene que bajar a una variable de tipo ANIDADA en un argumento de tipo.
//
// Con el parametro declarado INVARIANTE (Function<T, Stream<U>>) el call site compilaba sin
// invokestatic, y el llamador se quedaba con el argumento equivocado:
//
//   15: aload_1     // mapper, colgado en la pila
//   16: aload_0     // downstream
//   17: astore_2    // c = downstream  (!!)
//
// Es la forma de Collectors.flatMapping. Declararlo como el JDK
// (Function<T, ? extends Stream<? extends U>>) lo hacia resolver.
//
// Esperado: invariante() y conComodin() emiten ambas `invokestatic` y devuelven 7.
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
