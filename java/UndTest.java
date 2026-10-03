// Variables sin nombre (JEP 456): las siete posiciones donde `_` declara algo.
//
// `dosLocales` es la que custodia que DOS `_` en el mismo alcance no choquen --la atribucion chequea
// redeclaracion por nombre y hubo que exceptuar `_`--, y da 2 porque los dos efectos ocurren.
// `compSolo` es la que custodia que un `_` solo (que es `var _`) NO se declare: declararlo daba un
// slot de referencia donde despues se guarda un `int`, y la JVM real rechazaba la clase con *Bad
// type on operand stack*. Nuestra VM, que no verifica, contestaba bien de casualidad -- por eso esta
// prueba se corre por los tres caminos y no solo por el nuestro.
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

record Par(int a, int b) {}

public class UndTest {
    static int efectos = 0;
    static int conEfecto() { UndTest.efectos = UndTest.efectos + 1; return 7; }

    // Local sin nombre, y DOS en el mismo alcance (slots distintos, nada que choque).
    static int dosLocales() {
        UndTest.efectos = 0;
        int _ = UndTest.conEfecto();
        var _ = UndTest.conEfecto();
        return UndTest.efectos;
    }

    // for-each sin nombre.
    static int cuenta(List<String> l) {
        int n = 0;
        for (String _ : l) { n = n + 1; }
        return n;
    }

    // catch sin nombre.
    static int atrapa() {
        try { throw new IllegalStateException("x"); }
        catch (IllegalStateException _) { return 42; }
    }

    // Parametro de lambda sin nombre.
    static int lambda() {
        BiFunction<Integer, Integer, Integer> g = (a, _) -> a * 10;
        return g.apply(5, 99);
    }

    // Patron de tipo sin nombre en un switch.
    static int tipo(Object o) {
        return switch (o) { case Integer _ -> 1; case String _ -> 2; default -> 3; };
    }

    // Componente sin nombre en una deconstruccion, en switch y en instanceof.
    static int compSwitch(Object o) {
        return switch (o) { case Par(int _, int b) -> b; default -> -1; };
    }
    static int compIof(Object o) {
        if (o instanceof Par(int _, int b)) return b * 100;
        return -1;
    }
    static int compSolo(Object o) {
        if (o instanceof Par(_, int b)) return b * 1000;
        return -1;
    }

    public static void main(String[] z) {
        System.out.println(dosLocales());
        List<String> l = new ArrayList<String>();
        l.add("a"); l.add("b"); l.add("c");
        System.out.println(cuenta(l));
        System.out.println(atrapa());
        System.out.println(lambda());
        System.out.println(tipo(1));
        System.out.println(tipo("s"));
        System.out.println(tipo(2.5));
        System.out.println(compSwitch(new Par(8, 9)));
        System.out.println(compIof(new Par(8, 9)));
        System.out.println(compSolo(new Par(8, 9)));
    }
}
