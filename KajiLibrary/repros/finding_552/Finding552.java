import java.util.function.*;

/**
 * Finding #552: el `LambdaMetafactory` de nuestra VM solo arma implementaciones `REF_invokeStatic`, y
 * aun esas sin adaptar el boxing entre el SAM y la implementacion.
 *
 * - `sumar`: referencia estatica que pide unboxing (`Long` -> `long`): la VM revienta con
 *   "conversion: expected a long, found Reference(...)". El JDK da 3.
 * - `largo`: referencia a un metodo de instancia (`REF_invokeVirtual`, kind 5): la VM revienta en un
 *   `assert_eq!` ("only REF_invokeStatic implementations are spun so far"). El JDK da 4.
 * - `conThis`: lambda que usa `this` (`REF_invokeSpecial`, kind 7, igual que en el JDK): idem.
 *
 * Los tres `.class` que emitimos corren bien en la HotSpot.
 */
public class Finding552 {
    int base = 10;

    static int suma(int a, long b) { return a + (int) b; }

    public static int sumar() {
        BiFunction<Integer, Long, Integer> f = Finding552::suma;
        return f.apply(1, 2L);
    }

    public static int largo() {
        Function<String, Integer> f = String::length;
        return f.apply("abcd");
    }

    int conThis() {
        IntSupplier s = () -> base + 1;
        return s.getAsInt();
    }

    public static int instancia() { return new Finding552().conThis(); }

    public static void main(String[] args) {
        System.out.println("//552 " + sumar() + " " + largo() + " " + instancia());
    }
}
