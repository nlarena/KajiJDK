/**
 * Finding #556: una clase **local** declarada adentro de un metodo de una clase **anonima** no
 * resuelve su propia llamada.
 *
 * El JDK 25 imprime `//556 6`. Nuestro javac corta con *"el generador de bytecode todavia no soporta
 * una llamada que no resolvio a ningun metodo"* sobre `new Mas().v()`, que esta declarada tres
 * lineas mas arriba. Es la forma espejo del #555.
 */
import java.util.function.Supplier;

public class Finding556 {

    public static int run() {
        Supplier<Integer> otra = new Supplier<Integer>() {
            public Integer get() {
                class Mas {
                    Integer v() {
                        return 6;
                    }
                }
                return new Mas().v();
            }
        };
        System.out.println("//556 " + otra.get());
        return 0;
    }

    public static void main(String[] args) {
        run();
    }
}
