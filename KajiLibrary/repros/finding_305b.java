// La otra mitad del #305: esto NO tiene que compilar.
//
// `javac` real dice "incompatible types: <null> cannot be converted to long". El nuestro lo
// aceptaba y emitia `aconst_null` contra `(J)V`.
public class finding_305b {

    static int g(long x) {
        return 1;
    }

    public static int run() {
        return g(null);
    }
}
