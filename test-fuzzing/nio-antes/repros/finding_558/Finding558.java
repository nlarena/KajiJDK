import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Finding #558: `collect(Collector<? super T, A, R>)` no infiere su `R` del argumento cuando el
 * argumento es **otra llamada generica en linea**. El colector, medido solo, sale con el tipo bien
 * --`Collector<String, cap#0, Map<Integer, List<String>>>`--; la llamada de afuera se queda con
 * `Map<Object, List<Object>>`.
 *
 * El JDK 25 imprime `//558 2 3`. Nosotros cortamos con *"tipo incompatible en `porLargo`"*.
 *
 * Lo que lo delimita: ligar el colector a un **local** con su tipo escrito y despues pasarlo
 * compila, asi que no es el comodin de la posicion `A` ni el `collect`; es el argumento poly en
 * linea, la familia del #286 / #545.
 */
class Largo558 implements Function<String, Integer> {
    public Integer apply(String s) { return Integer.valueOf(s.length()); }
}

public class Finding558 {

    public static int run() {
        List<String> palabras = new ArrayList<String>();
        palabras.add("aa");
        palabras.add("bb");
        palabras.add("c");
        palabras.add("dd");

        // En linea: no resuelve.
        Map<Integer, List<String>> porLargo =
                palabras.stream().collect(Collectors.groupingBy(new Largo558()));

        // Control, con el colector en un local: esta forma SI compila.
        Collector<String, ?, Map<Integer, List<String>>> c =
                Collectors.<String, Integer>groupingBy(new Largo558());
        Map<Integer, List<String>> control = palabras.stream().collect(c);

        System.out.println("//558 " + porLargo.size() + " " + control.get(Integer.valueOf(2)).size());
        return 0;
    }

    public static void main(String[] args) {
        run();
    }
}
