import java.util.List;
import java.util.Map;

/**
 * El **control** del #520: las catorce formas de literal de clase que si son legales.
 *
 * <p>Va al lado del repro porque el arreglo del #520 agrega un rechazo, y un rechazo nuevo se mide
 * por lo que **no** rompe. Cubre lo que podria confundirse con "un tipo que no resuelve": un
 * primitivo, {@code void}, un array de una y de dos dimensiones, una interfaz, una anidada del JDK,
 * una anidada propia, una interna, la clase misma, un nombre completo, y un parametro de tipo en
 * alcance (que resuelve a una variable de tipo y no a una clase).
 *
 * <p>Imprime todo en una linea: la salida tiene que ser identica a la del JDK 25.
 */
public class Legit520<T> {

    static class Anidada {}
    class Interna {}

    interface Cosa {}

    <U> void generico() {
        Class<?> a = int.class;
        Class<?> b = void.class;
        Class<?> c = String.class;
        Class<?> d = String[].class;
        Class<?> e = int[][].class;
        Class<?> f = List.class;
        Class<?> g = Map.Entry.class;
        Class<?> h = Anidada.class;
        Class<?> i = Interna.class;
        Class<?> j = Cosa.class;
        Class<?> k = Legit520.class;
        Class<?> l = java.util.ArrayList.class;
        Class<?> m = Object[].class;
        Class<?> n = Anidada[].class;
        System.out.println(a + "" + b + c + d + e + f + g + h + i + j + k + l + m + n);
    }

    public static void main(String[] x) {
        new Legit520<String>().generico();
    }
}
