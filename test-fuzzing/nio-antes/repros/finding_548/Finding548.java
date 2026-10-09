import java.util.*;

/**
 * Finding #548: el lub de dos clases que comparten interfaces genericas, y el de una captura, no dan
 * un tipo usable. Las dos formas compilan en el JDK 25; aca, "tipo incompatible".
 */
public class Finding548 {
    public static int run() {
        var l = Arrays.asList("a", 1);
        Comparable<?> c = l.get(0);                       // lub(String, Integer) sin `Comparable`
        List<? extends Number> x = List.of(1);
        List<? extends Number> y = Collections.singletonList(x.get(0));   // argumento de captura
        return y.size() + (c == null ? 0 : 1);
    }

    public static void main(String[] args) { System.out.println("//548 " + run()); }
}
