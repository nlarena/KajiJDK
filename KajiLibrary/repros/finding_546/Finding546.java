import java.util.*;

/**
 * Finding #546: una llamada generica **sin argumentos** que la fijen, usada como argumento de un
 * metodo, no recibe el tipo del parametro como objetivo. Sola, no compila; con una sobrecarga
 * `Object` al lado, compila **eligiendo la otra**, en silencio.
 *
 * Contra el JDK 25: `run()` da 1 y `main` imprime `//546 list list opt`. Nosotros, comentando la
 * primera linea de `run`, imprimimos `//546 obj obj obj`.
 */
public class Finding546 {
    interface Nodo {}

    static int recibe(List<Nodo> p) { return 1; }

    static String g(List<Nodo> p) { return "list"; }
    static String g(Object o) { return "obj"; }

    static String h(List<? extends Nodo> p) { return "list"; }
    static String h(Object o) { return "obj"; }

    static String k(Optional<? extends CharSequence> p) { return "opt"; }
    static String k(Object o) { return "obj"; }

    public static int run() {
        int r = recibe(Collections.emptyList());   // no se encontro `recibe(List<Object>)`
        System.out.println("//546 " + g(Collections.emptyList()) + " " + h(Collections.emptyList())
                + " " + k(Optional.empty()));
        return r;
    }

    public static void main(String[] args) { run(); }
}
