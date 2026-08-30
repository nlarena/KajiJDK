// Repro del finding #306: una lambda pasada a un metodo generico de OTRA clase no compilaba.
//
//   error: el generador de bytecode no puede resolver el tipo `X`
//
// El metodo sintetico de la lambda declaraba un retorno `X` --la variable de tipo del metodo al que
// se la pasa-- y `X` esta declarada en otra clase: aca no hay nada que la nombre. Andaba solo si el
// metodo generico estaba en la MISMA clase que la lambda, que es una asimetria que no tiene sentido.
//
// Aparecio con `Optional.orElseThrow(Supplier<? extends X>) throws X`, que es exactamente esa forma.
// Arreglado borrando a su erasure las variables de tipo que el sintetico no puede nombrar: el
// descriptor usa la erasure igual, asi que el bytecode no cambia.
import java.util.function.Supplier;

class Ajena {
    static <X extends Throwable> int tira(Supplier<? extends X> s) throws X {
        throw s.get();
    }
}

public class finding_306 {
    public static int run() {
        try {
            Ajena.tira(() -> new IllegalStateException("v"));
        } catch (IllegalStateException e) {
            return 1;
        }
        return 0;
    }
}
