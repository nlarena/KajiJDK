import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// #498 -- el tipo objetivo con comodin no llega a la inferencia de un metodo generico.
//
// Hacen falta LAS DOS COSAS a la vez: que el destino tenga comodin y que el valor venga de una
// llamada generica cuyo argumento de tipo haya que inferir. Cada una por separado anda.
public class Finding498 {

    interface Nodo { }

    // --- falla -------------------------------------------------------------------------------
    List<? extends Nodo> falla() {
        return Collections.emptyList();     // error: tipo de retorno incompatible
    }

    // --- anda: el comodin solo ---------------------------------------------------------------
    List<? extends Nodo> comodinConVariable(List<Nodo> v) { return v; }

    List<? extends Nodo> comodinConNew() { return new ArrayList<Nodo>(); }

    List<? extends Nodo> comodinConElMismoTipo(List<? extends Nodo> v) { return v; }

    void recibe(List<? extends Nodo> p) { }

    void comodinComoArgumento(List<Nodo> v) { recibe(v); }

    // --- anda: el generico solo --------------------------------------------------------------
    List<Nodo> genericoSinComodin() { return Collections.emptyList(); }

    List<Nodo> genericoEnUnLocal() {
        List<Nodo> v = Collections.emptyList();
        return v;
    }

    List<Nodo> otroGenerico() { return List.of(); }

    String genericoPropio() { return uno(); }

    static <T> T uno() { return null; }

    // --- el rodeo ----------------------------------------------------------------------------
    List<? extends Nodo> conTestigoExplicito() {
        return Collections.<Nodo>emptyList();
    }
}
