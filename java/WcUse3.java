// Ver el encabezado de WcLib3.java. Este archivo NO COMPILA a proposito: r1, r3 y r4 son las tres
// llamadas que fallan. No dejar .class.
import java.util.function.BiConsumer;
import java.util.function.Supplier;
class SD implements Supplier<Down3<String>> { public Down3<String> get() { return null; } }
class BD implements BiConsumer<Object, Down3<String>> { public void accept(Object o, Down3<String> d) { } }
class BD2 implements BiConsumer<Object[], Down3<String>> { public void accept(Object[] o, Down3<String> d) { } }
public class WcUse3 {
    public static int run() {
        Supplier<Down3<String>> sd = new SD();
        BiConsumer<Object, Down3<String>> bd = new BD();
        BiConsumer<Object[], Down3<String>> bd2 = new BD2();
        String r1 = WcLib3.c1(sd);
        String r2 = WcLib3.c2(sd);
        String r3 = WcLib3.c3(bd);
        String r4 = WcLib3.c4(bd2);
        String r5 = WcLib3.c5(bd2);
        return -1;
    }
}
