/**
 * Finding #543: un `import static` de un campo, de un metodo o de un tipo suelto no se resuelve en
 * ninguna expresion ni declaracion. El JDK 25 compila las cuatro formas; nosotros rechazamos las
 * cuatro con "no se encuentra el simbolo". Dentro de una anotacion (`@Target({FIELD})`) si anda.
 */
import static java.lang.Math.max;
import static java.lang.Math.*;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Map.Entry;

public class Finding543 {
    static Entry<String, String> e;                 // tipo por import estatico simple

    public static int run() {
        int a = max(1, 2);                          // metodo por import estatico simple
        double b = PI;                              // campo por import estatico on-demand
        int c = MAX_VALUE;                          // campo por import estatico simple
        System.out.println("//543 " + a + " " + (b > 3) + " " + (c > 0) + " " + e);
        return a;
    }

    public static void main(String[] args) { run(); }
}
