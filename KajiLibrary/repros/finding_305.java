// Repro del finding #305: `null` era aplicable a un parametro PRIMITIVO.
//
// `null` se tipaba `RType::Unresolved`, que es el comodin indulgente del compilador, y por lo tanto
// convertible a cualquier cosa. Dos consecuencias, las dos malas:
//
//   f(null)  ->  elegia f(long) en vez de f(Void)          (sobrecarga equivocada)
//   g(null)  ->  compilaba, emitiendo `aconst_null` contra un descriptor `(J)V`
//
// Lo segundo es lo grave: una referencia empujada donde va un `long`, con un solo slot de pila en
// vez de dos. Bytecode que ningun verificador acepta, salido de un compilador que no dijo nada.
//
// Salio de `Random.from`, cuyo adaptador llama `super(null)` para saltear el constructor que
// llamaria al `setSeed` que el adaptador se niega a atender.
//
// Con el arreglo (RType::Null, subtipo de todo tipo referencia y de ningun primitivo, JLS 4.10.2)
// esto devuelve 2, igual que `java` real. El caso que debe ser ERROR esta en finding_305b.java.
public class finding_305 {

    static int f(long x) {
        return 1;
    }

    static int f(Void v) {
        return 2;
    }

    public static int run() {
        return f(null);
    }
}
