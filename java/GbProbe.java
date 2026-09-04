// REPRO (java.util.stream, 2026-09-01) — una variable de tipo que solo aparece detras de un `?`
// no se infiere cuando el resultado es a su vez generico anidado:
//
//   Map<String, List<String>> m = Stream.of(a).collect(Collectors.<String, String>groupingBy(f));
//
// da "tipo incompatible en `m`". Es una limitacion vieja (no la trajo esta pasada: `groupingBy` ya
// estaba) y le pega igual a `groupingByConcurrent`. El rodeo --el que quedo escrito abajo y el que
// usa java/StreamGatherTest.java-- es atar el colector a un local con el acumulador ESCRITO:
//
//   Collector<String, Object[], Map<String, List<String>>> col = (Collector<...>) Collectors...;
//   Map<String, List<String>> m = Stream.of(a).collect(col);
//
// Compila con los dos javac. Corre: ./bin/run-headless.exe java/GbProbe.class run  ->  -1
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentMap;

class Pl implements Function<String, String> { public String apply(String s) { return s.substring(0,1); } }

public class GbProbe {
    public static int run() {
        String[] a = new String[2];
        a[0] = "ax"; a[1] = "bx";
        // (1) groupingBy normal, que ya existia
        Collector<String, Object[], Map<String, List<String>>> col =
                (Collector<String, Object[], Map<String, List<String>>>) Collectors.<String, String>groupingBy(new Pl());
        Map<String, List<String>> m = Stream.of(a).collect(col);
        if (m.size() != 2) { return 0; }
        return -1;
    }
}
