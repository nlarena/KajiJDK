import java.util.Collections;
import java.util.List;
import java.util.Set;

// Repro de #502: `--emit` rechaza una inferencia que el chequeo acepta.
//
//   bin/javac.exe       -cp KajiLibrary Finding502.java   -> compila
//   bin/javac.exe --emit -cp KajiLibrary Finding502.java  -> "tipo de retorno incompatible"
//
// El JDK 25 compila el archivo entero.
//
// El disparador es el DESTINO: un tipo parametrizado con comodin acotado
// (`Set<? extends X>`), recibiendo una llamada generica cuyo argumento de tipo hay que
// inferir. No importa que sea X: falla igual con Runnable que con un tipo anidado.
//
// Ojo con el orden al reproducir: el compilador corta en el primer error, asi que los
// metodos que fallan hay que probarlos de a uno. Los tres primeros de aca compilan.
public class Finding502 {

    interface Base {
        interface Op { }
    }

    // ---- compilan con --emit ----

    // Sin comodin, inferido.
    static Set<Runnable> ok1() {
        return Collections.emptySet();
    }

    // Con comodin, pero con testigo explicito: no hay nada que inferir.
    static Set<? extends Runnable> ok2() {
        return Collections.<Runnable>emptySet();
    }

    // Sin comodin, con un tipo anidado.
    static Set<Base.Op> ok3() {
        return Collections.emptySet();
    }

    // ---- fallan con --emit, compilan sin el ----

    // El caso minimo: comodin acotado + inferencia.
    static Set<? extends Runnable> mal1() {
        return Collections.emptySet();
    }

    // Lo mismo con List, para mostrar que no es de Set.
    static List<? extends Runnable> mal2() {
        return Collections.emptyList();
    }

    // Lo mismo con un tipo anidado, que es como aparecio (StandardDoclet.getSupportedOptions).
    static Set<? extends Base.Op> mal3() {
        return Collections.emptySet();
    }
}
