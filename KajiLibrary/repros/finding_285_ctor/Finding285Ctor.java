// #285 sobre un CONSTRUCTOR, y la delimitacion que faltaba: variable de tipo de la CLASE.
//
// El JDK 25 compila las cinco. El nuestro rechaza solo `falla`.
//
//   javac --emit -cp KajiLibrary KajiLibrary/repros/finding_285_ctor/Finding285Ctor.java

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Finding285Ctor<V> {

    /** FALLA: "un `new` con argumentos que no resolvio a ningun constructor". */
    List<V> falla(V[] xs) {
        return new ArrayList<V>(Arrays.asList(xs));
    }

    /** CONTROL 1 -- el mismo `new`, con el tipo nombrado en un local: anda. */
    List<V> conLocal(V[] xs) {
        List<V> t = Arrays.asList(xs);
        return new ArrayList<V>(t);
    }

    /** CONTROL 2 -- el mismo `new`, con la variable de tipo del METODO: anda. */
    static <W> List<W> delMetodo(W[] xs) {
        return new ArrayList<W>(Arrays.asList(xs));
    }

    /** CONTROL 3 -- la misma llamada generica sola, en contexto de asignacion: anda. */
    List<V> soloLaLlamada(V[] xs) {
        List<V> t = Arrays.asList(xs);
        return t;
    }

    /** CONTROL 4 -- la misma llamada generica como argumento de un METODO: anda. */
    static <W> void sumidero(List<W> t) {
    }

    void comoArgumentoDeMetodo(V[] xs) {
        sumidero(Arrays.asList(xs));
    }
}
