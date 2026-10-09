// Repro de #286 - un lambda pasado en linea no liga su parametro cuando el tipo destino es
// `X<? super E>` con E viniendo del ARGUMENTO DE TIPO DEL RECEPTOR.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_286.java
//   error: el generador de bytecode todavia no soporta una llamada que no resolvio a ningun metodo
//           return b.conWildcard(s -> s.length() == 2);
//                                     ^
//
// El caret cae sobre el uso del parametro del lambda: `s` quedo sin tipo, asi que `s.length()`
// no resuelve. El JDK 25 compila los cuatro metodos de abajo.
//
// Lo que decide si falla es la combinacion de DOS cosas — con una sola no alcanza:
//
//   destino `Predicate<String>` (concreto)                 -> compila
//   destino `Predicate<E>`, E del receptor                 -> compila
//   destino `Predicate<? super T>`, T de otro argumento    -> compila
//   destino `Predicate<? super E>`, E del receptor         -> FALLA
//
// Hace falta el comodin `? super` Y que la variable venga del receptor. Poner el tipo del
// parametro explicito (`(String s) -> ...`) NO alcanza: sigue fallando, o sea que no es que no
// deduzca el tipo del parametro sino que no llega a tipar la llamada.
//
// El rodeo es sacar el lambda de la posicion de argumento:
//
//   Predicate<String> p = s -> s.length() == 2;
//   b.conWildcard(p);                              // compila
//
// Es el mismo rodeo que #285 y #279 —nombrar el tipo en un local—, pero el mecanismo es otro:
// alli es una llamada generica anidada o una ambiguedad de sobrecargas; aca es un lambda.
//
// Donde muerde: `Collection.removeIf(Predicate<? super E>)`, `Map.forEach(BiConsumer<? super K,
// ? super V>)` y en general los `default` del JDK 8+, que usan `? super` casi sin excepcion.
// `lista.removeIf(s -> s.length() == 2)` es codigo Java corriente y no compila.
import java.util.function.Predicate;

public class finding_286<E> {

    E v;

    boolean conWildcard(Predicate<? super E> p) {
        return p.test(this.v);
    }

    boolean sinWildcard(Predicate<E> p) {
        return p.test(this.v);
    }

    static <T> boolean deArgumento(Predicate<? super T> p, T valor) {
        return p.test(valor);
    }

    static boolean concreto(Predicate<String> p) {
        return p.test("ab");
    }

    // El que falla.
    public static int falla() {
        finding_286<String> b = new finding_286<String>();
        b.v = "ab";
        return b.conWildcard(s -> s.length() == 2) ? 1 : 0;
    }

    // El rodeo: el lambda fuera de la posicion de argumento.
    public static int rodeo() {
        finding_286<String> b = new finding_286<String>();
        b.v = "ab";
        Predicate<String> p = s -> s.length() == 2;
        return b.conWildcard(p) ? 1 : 0;
    }

    // Controles: los tres que compilan.
    public static int control1() {
        finding_286<String> b = new finding_286<String>();
        b.v = "ab";
        return b.sinWildcard(s -> s.length() == 2) ? 1 : 0;
    }

    public static int control2() {
        return deArgumento(s -> ((String) s).length() == 2, "ab") ? 1 : 0;
    }

    public static int control3() {
        return concreto(s -> s.length() == 2) ? 1 : 0;
    }
}
