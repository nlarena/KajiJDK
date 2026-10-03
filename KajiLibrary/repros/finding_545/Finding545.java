import java.util.*;

/**
 * Finding #545: una llamada generica cuyo argumento es **otra** llamada generica no infiere nada si
 * a su vez es argumento de un metodo. La de adentro cae a `Object` y el metodo de afuera no aplica.
 * No hace falta ninguna variable de tipo de la clase (eso era el #285): todo es concreto.
 */
public class Finding545 {
    static <T> T id(T t) { return t; }
    static int take(List<String> l) { return l.size(); }
    static int takeE(Map.Entry<String, Integer> e) { return e.getValue(); }

    public static int run() {
        int a = take(List.of(id("a")));                 // no se encontro `take(List<Object>)`
        int b = takeE(Map.entry(id("a"), 1));           // no se encontro `takeE(Map$Entry<Object, Integer>)`
        List<String> ok = List.of(id("a"));             // control: en un local, anda
        return a + b + ok.size();
    }

    public static void main(String[] args) {
        System.out.println("//545 " + run());
    }
}
