/**
 * Finding #555: una clase **anonima** declarada adentro de un metodo de una clase **local** no se
 * puede emitir.
 *
 * El JDK 25 imprime `//555 5`. Nuestro javac corta con *"el generador de bytecode todavia no soporta
 * una clase anonima (necesita una clase sintetica anidada)"*. El #533 cubrio la anonima adentro de
 * otra **anonima**; esta forma --anonima adentro de una **local con nombre**-- no.
 */
import java.util.function.Supplier;

public class Finding555 {

    public static int run() {
        class Afuera {
            Supplier<Integer> adentro() {
                return new Supplier<Integer>() {
                    public Integer get() {
                        return 5;
                    }
                };
            }
        }
        System.out.println("//555 " + new Afuera().adentro().get());
        return 0;
    }

    public static void main(String[] args) {
        run();
    }
}
