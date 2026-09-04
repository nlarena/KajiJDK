// Sonda de java.util.stream.Collector.of: la llamada variarg con caracteristicas explicitas,
// que es el camino que java/StreamGatherTest.java no cubre (alla se llama sin ninguna).
//   ./bin/javac.exe --emit -cp KajiLibrary java/ColOfProbe.java
//   ./bin/run-headless.exe java/ColOfProbe.class run   ->  -1
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;

class CSup implements Supplier<StringBuilder> { public StringBuilder get() { return new StringBuilder(); } }
class CAcc implements BiConsumer<StringBuilder, String> { public void accept(StringBuilder b, String s) { b.append(s); } }
class CComb implements BinaryOperator<StringBuilder> { public StringBuilder apply(StringBuilder a, StringBuilder b) { a.append(b); return a; } }
class CFin implements Function<StringBuilder, String> { public String apply(StringBuilder b) { return b.toString(); } }

public class ColOfProbe {
    public static int run() {
        Collector.Characteristics[] cs = new Collector.Characteristics[1];
        cs[0] = Collector.Characteristics.UNORDERED;
        Collector<String, StringBuilder, String> c =
                Collector.of(new CSup(), new CAcc(), new CComb(), new CFin(), cs);
        Set<Collector.Characteristics> m = c.characteristics();
        if (m.size() != 1) { return 0; }
        if (!m.contains(Collector.Characteristics.UNORDERED)) { return 1; }
        Collector<String, StringBuilder, StringBuilder> c2 =
                Collector.of(new CSup(), new CAcc(), new CComb(), cs);
        Set<Collector.Characteristics> m2 = c2.characteristics();
        if (m2.size() != 2) { return 2; }
        if (!m2.contains(Collector.Characteristics.IDENTITY_FINISH)) { return 3; }
        // el conjunto se niega a ser modificado
        try { m2.add(Collector.Characteristics.CONCURRENT); return 4; } catch (UnsupportedOperationException e) { }
        // values()/valueOf() del enum anidado
        if (Collector.Characteristics.values().length != 3) { return 5; }
        if (Collector.Characteristics.valueOf("CONCURRENT") != Collector.Characteristics.CONCURRENT) { return 6; }
        // la forma variarg de verdad, sin armar el arreglo a mano
        Collector<String, StringBuilder, String> c3 = Collector.of(new CSup(), new CAcc(), new CComb(),
                new CFin(), Collector.Characteristics.UNORDERED, Collector.Characteristics.CONCURRENT);
        if (c3.characteristics().size() != 2) { return 7; }
        Collector<String, StringBuilder, String> c4 =
                Collector.of(new CSup(), new CAcc(), new CComb(), new CFin());
        if (c4.characteristics().size() != 0) { return 8; }
        return -1;
    }
}
