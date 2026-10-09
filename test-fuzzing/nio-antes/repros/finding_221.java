// Repro de #221 - un retorno `A[]` (array de variable de tipo del metodo) no llamaba al generador,
// y el llamador recibia un array de longitud 0 sin excepcion.
//
// La causa estaba en la APLICABILIDAD, no en la emision: el argumento de tipo del parametro es
// `A[]`, o sea `Array(TypeVar)` y no un `TypeVar` pelado. La indulgencia que deja pasar un
// `Box<A>` contra un `Box<String>` -- porque `A` la fija la inferencia -- no atravesaba el array,
// asi que se caia a la invariancia y se comparaba `String[]` contra `A[]`.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_221.java
//   bin\run-headless.exe KajiLibrary\repros\finding_221.class run   -> 0
import java.util.function.IntFunction;

public class finding_221 {

    static class Gen implements IntFunction<String[]> {
        public String[] apply(int n) { return new String[n]; }
    }

    static <A> A[] arma(IntFunction<A[]> g) { return g.apply(3); }

    public static int run() {
        String[] r = finding_221.arma(new Gen());
        if (r == null) { return 1; }
        if (r.length != 3) { return 2; }
        /* Control: invocar el generador directo siempre habia andado. */
        if (new Gen().apply(5).length != 5) { return 3; }
        return 0;
    }
}
