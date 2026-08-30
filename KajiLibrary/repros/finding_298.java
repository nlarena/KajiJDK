// Repro de #298 - un arreglo pasado a un varargs GENERICO se envolvia en otro arreglo en vez de
// pasar tal cual.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_298.java
//   bin\run-headless.exe KajiLibrary\repros\finding_298.class asList
//
// ANTES:
//
//   String[] a = { "x", "y", "z" };
//   Arrays.asList(a).size()      ->  1     (deberia ser 3)
//   Stream.of(a).count()         ->  1     (idem)
//
// La lista tenia UN elemento, y ese elemento era el arreglo. Compilaba, corria, y devolvia algo
// razonable de tipo: por eso podia estar ahi mucho tiempo sin que nada reventara.
//
// La regla (§15.12.4.2) es que si el unico argumento **ya es** el arreglo del varargs, se pasa tal
// cual en vez de envolverlo. El chequeo existia, pero preguntaba por subtipado: `String[]` contra
// `Object...` da true, y contra `T...` da **false**, porque `T` todavia no esta instanciada. Asi
// que los varargs no genericos andaban y los genericos no -- que son justo los mas usados:
// `Arrays.asList`, `List.of`, `Set.of`, `Stream.of`, `Collections.addAll`.
//
// AHORA: se agrega el caso de la variable de tipo, con la misma regla del §4.5.1 que cerro #290 --
// una variable de tipo solo liga tipos REFERENCIA:
//
//   String[]  contra T...   ->  T liga String, el arreglo pasa tal cual
//   int[]     contra T...   ->  T no puede ligar int, asi que liga int[] y el arreglo SE ENVUELVE
//
// El segundo no es un descuido sino lo que hace el javac real, y es la razon por la que
// `Arrays.asList(new int[]{1,2})` da una lista de un solo elemento. La prueba lo fija.
//
// Lo que destapo el arreglo: 22 clases de la biblioteca emitian bytecode distinto -- entre ellas
// `String`, `Arrays`, `Collection`, `Pattern`, `Matcher` y el paquete `stream` entero.
//
// `asList` -> 3, `stream` -> 3, `sueltos` -> 3, `primitivos` -> 1, `noGenerico` -> 3.
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class finding_298 {

    static <T> int cuentaGenerico(T... xs) {
        return xs.length;
    }

    static int cuentaObjetos(Object... xs) {
        return xs.length;
    }

    // El caso del finding, contra la biblioteca.
    public static int asList() {
        String[] a = { "x", "y", "z" };
        List<String> l = Arrays.asList(a);
        return l.size();
    }

    public static int stream() {
        String[] a = { "x", "y", "z" };
        return (int) Stream.of(a).count();
    }

    // Argumentos sueltos: estos SI se envuelven, y siempre funcionaron.
    public static int sueltos() {
        return cuentaGenerico("x", "y", "z");
    }

    // Un arreglo de primitivos contra `T...`: se envuelve, y esta bien que asi sea.
    public static int primitivos() {
        int[] a = { 1, 2, 3 };
        return cuentaGenerico(a);
    }

    // Control: el varargs NO generico nunca estuvo roto.
    public static int noGenerico() {
        Object[] a = { "x", "y", "z" };
        return cuentaObjetos(a);
    }
}
