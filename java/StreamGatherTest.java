import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.PrimitiveIterator;
import java.util.IntSummaryStatistics;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.StreamSupport;

// Piezas con nombre en vez de lambdas: es la regla de la casa para todo lo que termine guardado
// en un campo de otro objeto (ver el encabezado de Collectors.java). Compilan igual con los dos
// javac, asi que la prueba corre en las dos VMs sin cambiar una linea.

final class SArrGen implements IntFunction<String[]> {
    public String[] apply(int n) {
        return new String[n];
    }
}

final class Largo implements ToIntFunction<String> {
    public int applyAsInt(String s) {
        return s.length();
    }
}

final class PrimeraLetra implements Function<String, String> {
    public String apply(String s) {
        return s.substring(0, 1);
    }
}

final class Identidad implements Function<String, String> {
    public String apply(String s) {
        return s;
    }
}

final class Cero implements Supplier<Integer> {
    public Integer get() {
        return Integer.valueOf(0);
    }
}

final class SumaLargo implements BiFunction<Integer, String, Integer> {
    public Integer apply(Integer acumulado, String s) {
        return Integer.valueOf(acumulado.intValue() + s.length());
    }
}

// Pega dos String con un StringBuilder y no con `+`: la concatenacion en tiempo de ejecucion no
// esta disponible en nuestra VM (#226), y esta prueba tiene que compilar y correr con las dos.
final class Pegar implements BinaryOperator<String> {
    public String apply(String a, String b) {
        StringBuilder sb = new StringBuilder();
        sb.append(a);
        sb.append(b);
        return sb.toString();
    }
}

final class AMayuscula implements Function<String, String> {
    public String apply(String s) {
        return s.toUpperCase();
    }
}

// Un Gatherer propio: duplica cada elemento. Sin estado.
final class Duplicar implements Gatherer.Integrator<Void, String, String> {
    public boolean integrate(Void estado, String e, Gatherer.Downstream<? super String> abajo) {
        abajo.push(e);
        abajo.push(e);
        return true;
    }
}

// Otro: deja pasar dos y corta. Con estado, para probar que el corte funciona.
final class TomarDos implements Gatherer.Integrator<int[], String, String> {
    public boolean integrate(int[] estado, String e, Gatherer.Downstream<? super String> abajo) {
        estado[0] = estado[0] + 1;
        abajo.push(e);
        return estado[0] < 2;
    }
}

final class ContadorCero implements Supplier<int[]> {
    public int[] get() {
        return new int[1];
    }
}

// Las cuatro piezas de un Collector armado a mano con Collector.of.
final class SbSup implements Supplier<StringBuilder> {
    public StringBuilder get() {
        return new StringBuilder();
    }
}

final class SbAcc implements BiConsumer<StringBuilder, String> {
    public void accept(StringBuilder sb, String s) {
        sb.append(s);
    }
}

final class SbComb implements BinaryOperator<StringBuilder> {
    public StringBuilder apply(StringBuilder a, StringBuilder b) {
        a.append(b);
        return a;
    }
}

final class SbFin implements Function<StringBuilder, String> {
    public String apply(StringBuilder sb) {
        return sb.toString();
    }
}

/**
 * Prueba de comportamiento de lo que se agrego a java.util.stream: `Gatherer`, `Gatherers`,
 * `StreamSupport`, `Collector.characteristics()`/`of`, los resumenes, los colectores concurrentes,
 * `Stream.of(T)`, `Stream.toArray(IntFunction)` y los iteradores/spliterators primitivos.
 *
 * <p>`run()` no toca nada que difiera entre las dos VMs: se corre con las dos y se compara. Las
 * negativas de `generate`/`iterate` --que son una divergencia deliberada de esta biblioteca-- van
 * aparte, en `negativas()`, porque en el JDK real no tiran nada.
 *
 * <p>El orden de iteracion de los mapas no se compara nunca: `groupingBy` devuelve un `HashMap` y
 * el orden puede diferir entre las dos VMs. Se compara contenido.
 */
public class StreamGatherTest {

    public static int run() {
        String[] abc = new String[3];
        abc[0] = "aa";
        abc[1] = "bbb";
        abc[2] = "c";

        // --- Stream.of: la variarg no le cede la llamada a la de un elemento ---
        if (Stream.of(abc).count() != 3L) {
            return 0;
        }
        if (Stream.of("z").count() != 1L) {
            return 1;
        }

        // --- toArray(IntFunction) ---
        String[] vuelta = Stream.of(abc).toArray(new SArrGen());
        if (vuelta.length != 3) {
            return 2;
        }
        if (!vuelta[1].equals("bbb")) {
            return 3;
        }

        // --- gather con un Gatherer propio, sin estado ---
        Gatherer<String, Void, String> dup = Gatherer.ofSequential(new Duplicar());
        Object[] duplicado = Stream.of(abc).gather(dup).toArray();
        if (duplicado.length != 6) {
            return 4;
        }
        if (!duplicado[0].equals("aa")) {
            return 5;
        }
        if (!duplicado[1].equals("aa")) {
            return 6;
        }
        if (!duplicado[5].equals("c")) {
            return 7;
        }

        // --- gather con estado y corte ---
        Gatherer<String, int[], String> dos = Gatherer.ofSequential(new ContadorCero(), new TomarDos());
        Object[] cortado = Stream.of(abc).gather(dos).toArray();
        if (cortado.length != 2) {
            return 8;
        }
        if (!cortado[1].equals("bbb")) {
            return 9;
        }

        // --- andThen: duplicar y despues tomar dos ---
        Gatherer<String, ?, String> compuesto = dup.andThen(dos);
        Object[] comp = Stream.of(abc).gather(compuesto).toArray();
        if (comp.length != 2) {
            return 10;
        }
        if (!comp[0].equals("aa")) {
            return 11;
        }
        if (!comp[1].equals("aa")) {
            return 12;
        }

        // --- Gatherers.windowFixed: la ultima ventana sale corta ---
        String[] cinco = new String[5];
        cinco[0] = "a";
        cinco[1] = "b";
        cinco[2] = "c";
        cinco[3] = "d";
        cinco[4] = "e";
        Gatherer<String, ?, List<String>> fijas = Gatherers.windowFixed(2);
        Object[] vent = Stream.of(cinco).gather(fijas).toArray();
        if (vent.length != 3) {
            return 13;
        }
        List<String> v0 = (List<String>) vent[0];
        if (v0.size() != 2) {
            return 14;
        }
        if (!v0.get(0).equals("a")) {
            return 15;
        }
        List<String> v2 = (List<String>) vent[2];
        if (v2.size() != 1) {
            return 16;
        }
        if (!v2.get(0).equals("e")) {
            return 17;
        }

        // --- Gatherers.windowSliding ---
        Gatherer<String, ?, List<String>> desliz = Gatherers.windowSliding(2);
        Object[] des = Stream.of(abc).gather(desliz).toArray();
        if (des.length != 2) {
            return 18;
        }
        List<String> d0 = (List<String>) des[0];
        if (!d0.get(0).equals("aa")) {
            return 19;
        }
        if (!d0.get(1).equals("bbb")) {
            return 20;
        }
        List<String> d1 = (List<String>) des[1];
        if (!d1.get(0).equals("bbb")) {
            return 21;
        }

        // ...y con la ventana mas grande que la entrada: sale UNA ventana con todo
        Gatherer<String, ?, List<String>> grande = Gatherers.windowSliding(5);
        Object[] uno = Stream.of(abc).gather(grande).toArray();
        if (uno.length != 1) {
            return 22;
        }
        List<String> u0 = (List<String>) uno[0];
        if (u0.size() != 3) {
            return 23;
        }

        // --- Gatherers.fold: un solo elemento al final ---
        Gatherer<String, ?, Integer> pliegue = Gatherers.fold(new Cero(), new SumaLargo());
        Object[] plegado = Stream.of(abc).gather(pliegue).toArray();
        if (plegado.length != 1) {
            return 24;
        }
        Integer total = (Integer) plegado[0];
        if (total.intValue() != 6) {
            return 25;
        }

        // --- Gatherers.scan: uno por elemento ---
        Gatherer<String, ?, Integer> corrida = Gatherers.scan(new Cero(), new SumaLargo());
        Object[] parciales = Stream.of(abc).gather(corrida).toArray();
        if (parciales.length != 3) {
            return 26;
        }
        Integer p0 = (Integer) parciales[0];
        if (p0.intValue() != 2) {
            return 27;
        }
        Integer p2 = (Integer) parciales[2];
        if (p2.intValue() != 6) {
            return 28;
        }

        // --- Gatherers.mapConcurrent: mismo resultado y mismo orden ---
        Gatherer<String, ?, String> mayus = Gatherers.mapConcurrent(2, new AMayuscula());
        Object[] altas = Stream.of(abc).gather(mayus).toArray();
        if (altas.length != 3) {
            return 29;
        }
        if (!altas[0].equals("AA")) {
            return 30;
        }
        if (!altas[2].equals("C")) {
            return 31;
        }

        // --- IntStream.summaryStatistics ---
        int[] nums = new int[4];
        nums[0] = 3;
        nums[1] = 1;
        nums[2] = 4;
        nums[3] = 2;
        IntSummaryStatistics stats = IntStream.of(nums).summaryStatistics();
        if (stats.getCount() != 4L) {
            return 32;
        }
        if (stats.getSum() != 10L) {
            return 33;
        }
        if (stats.getMin() != 1) {
            return 34;
        }
        if (stats.getMax() != 4) {
            return 35;
        }
        if (stats.getAverage() != 2.5d) {
            return 36;
        }

        // --- iterator() primitivo: nextInt() sin embolsar ---
        PrimitiveIterator.OfInt it = IntStream.of(nums).iterator();
        int suma = 0;
        while (it.hasNext()) {
            suma = suma + it.nextInt();
        }
        if (suma != 10) {
            return 37;
        }

        // --- spliterator() primitivo, y de vuelta a un flujo por StreamSupport ---
        Spliterator.OfInt sp = IntStream.of(nums).spliterator();
        IntStream devuelto = StreamSupport.intStream(sp, false);
        if (devuelto.sum() != 10) {
            return 38;
        }

        // --- StreamSupport sobre un Spliterator de referencias ---
        Spliterator<String> spo = Stream.of(abc).spliterator();
        Stream<String> rearmado = StreamSupport.stream(spo, false);
        if (rearmado.count() != 3L) {
            return 39;
        }

        // --- Collectors.summarizingInt ---
        IntSummaryStatistics porLargo = Stream.of(abc).collect(Collectors.summarizingInt(new Largo()));
        if (porLargo.getCount() != 3L) {
            return 40;
        }
        if (porLargo.getSum() != 6L) {
            return 41;
        }
        if (porLargo.getMax() != 3) {
            return 42;
        }

        // --- Collectors.toConcurrentMap (contenido, nunca orden) ---
        ConcurrentMap<String, String> mapa =
                Stream.of(abc).collect(Collectors.<String, String, String>toConcurrentMap(new PrimeraLetra(), new Identidad()));
        if (mapa.size() != 3) {
            return 43;
        }
        if (!mapa.get("b").equals("bbb")) {
            return 44;
        }
        if (!mapa.get("c").equals("c")) {
            return 45;
        }

        // --- Collectors.groupingByConcurrent (contenido, nunca orden) ---
        String[] seis = new String[4];
        seis[0] = "ala";
        seis[1] = "aro";
        seis[2] = "bar";
        seis[3] = "bis";
        // El colector se ata a un local con el tipo del acumulador ESCRITO (`Object`) en vez de
        // pasarlo en linea: nuestro javac no infiere una variable de tipo que solo aparece detras
        // de un `?` cuando el resultado es a su vez generico anidado (`ConcurrentMap<K, List<T>>`).
        // Es una limitacion vieja y no de este paquete: `Collectors.groupingBy` la tiene igual
        // (repro java/GbProbe.java). El rodeo compila con los dos javac.
        Collector<String, Object, ConcurrentMap<String, List<String>>> colGrupos =
                (Collector<String, Object, ConcurrentMap<String, List<String>>>)
                        Collectors.<String, String>groupingByConcurrent(new PrimeraLetra());
        ConcurrentMap<String, List<String>> grupos = Stream.of(seis).collect(colGrupos);
        if (grupos.size() != 2) {
            return 46;
        }
        List<String> conA = grupos.get("a");
        if (conA.size() != 2) {
            return 47;
        }
        if (!conA.get(0).equals("ala")) {
            return 48;
        }
        List<String> conB = grupos.get("b");
        if (conB.size() != 2) {
            return 49;
        }

        // --- groupingByConcurrent con downstream ---
        // El `downstream` tambien se ata con su acumulador escrito, por lo mismo: `counting()`
        // declara `Collector<T, ?, Long>` y los dos javac necesitan un `A` concreto aca.
        Collector<String, Object, Long> contar = (Collector<String, Object, Long>) Collectors.<String>counting();
        Collector<String, Object, ConcurrentMap<String, Long>> colCuentas =
                (Collector<String, Object, ConcurrentMap<String, Long>>)
                        Collectors.<String, String, Object, Long>groupingByConcurrent(new PrimeraLetra(), contar);
        ConcurrentMap<String, Long> cuentas = Stream.of(seis).collect(colCuentas);
        if (cuentas.size() != 2) {
            return 50;
        }
        Long cuentaA = cuentas.get("a");
        if (cuentaA.longValue() != 2L) {
            return 51;
        }

        // --- Collector.of, con y sin finalizador propio ---
        Collector<String, StringBuilder, String> concat =
                Collector.of(new SbSup(), new SbAcc(), new SbComb(), new SbFin());
        String pegado = Stream.of(abc).collect(concat);
        if (!pegado.equals("aabbbc")) {
            return 52;
        }
        if (concat.characteristics().size() != 0) {
            return 53;
        }

        Collector<String, StringBuilder, StringBuilder> sinFin =
                Collector.of(new SbSup(), new SbAcc(), new SbComb());
        StringBuilder acumulado = Stream.of(abc).collect(sinFin);
        if (!acumulado.toString().equals("aabbbc")) {
            return 54;
        }
        if (!sinFin.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return 55;
        }
        if (sinFin.characteristics().size() != 1) {
            return 56;
        }

        // --- characteristics() de los colectores de fabrica, donde coincide con el JDK ---
        Collector<String, ?, List<String>> aLista = Collectors.toList();
        if (aLista.characteristics().size() != 1) {
            return 57;
        }
        if (!aLista.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return 58;
        }

        Collector<String, ?, Set<String>> aConjunto = Collectors.toSet();
        if (aConjunto.characteristics().size() != 2) {
            return 59;
        }
        if (!aConjunto.characteristics().contains(Collector.Characteristics.UNORDERED)) {
            return 60;
        }

        Collector<CharSequence, ?, String> juntando = Collectors.joining();
        if (juntando.characteristics().size() != 0) {
            return 61;
        }

        Collector<String, ?, IntSummaryStatistics> resumen = Collectors.summarizingInt(new Largo());
        if (resumen.characteristics().size() != 1) {
            return 62;
        }

        Collector<String, ?, ConcurrentMap<String, String>> aConcurrente =
                Collectors.<String, String, String>toConcurrentMap(new PrimeraLetra(), new Identidad());
        if (aConcurrente.characteristics().size() != 3) {
            return 63;
        }
        if (!aConcurrente.characteristics().contains(Collector.Characteristics.CONCURRENT)) {
            return 64;
        }

        // --- toConcurrentMap con merge: es el camino que ejercita el bucle putIfAbsent/replace ---
        Collector<String, Object, ConcurrentMap<String, String>> colMerge =
                (Collector<String, Object, ConcurrentMap<String, String>>)
                        Collectors.<String, String, String>toConcurrentMap(new PrimeraLetra(),
                                new Identidad(), new Pegar());
        ConcurrentMap<String, String> pegados = Stream.of(seis).collect(colMerge);
        if (pegados.size() != 2) {
            return 65;
        }
        if (!pegados.get("a").equals("alaaro")) {
            return 66;
        }
        if (!pegados.get("b").equals("barbis")) {
            return 67;
        }

        // --- los argumentos invalidos de Gatherers se rechazan igual en las dos VMs ---
        try {
            Gatherers.<String>windowFixed(0);
            return 68;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        try {
            Gatherers.<String>windowSliding(0);
            return 69;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        try {
            Gatherers.<String, String>mapConcurrent(0, new AMayuscula());
            return 70;
        } catch (IllegalArgumentException e) {
            // esperado
        }

        // --- el combinador por omision de un Gatherer se niega, igual que en el JDK ---
        BinaryOperator<Object> comb = Gatherer.defaultCombiner();
        try {
            comb.apply("a", "b");
            return 71;
        } catch (UnsupportedOperationException e) {
            // esperado
        }

        return -1;
    }

    /**
     * Las negativas de esta biblioteca: los cinco constructores de flujos infinitos que se niegan.
     *
     * <p>NO forma parte de `run()` a proposito. Es la divergencia deliberada del paquete --los
     * flujos son ansiosos--, asi que en el JDK real ninguna de las cinco tira nada y comparar las
     * dos VMs aca no querria decir nada. Se corre solo con la nuestra.
     *
     * @return -1 si las cinco se negaron, o el indice de la primera que no
     */
    public static int negativas() {
        Cero cero = new Cero();
        try {
            Stream.generate(cero);
            return 0;
        } catch (UnsupportedOperationException e) {
            // esperado
        }
        try {
            Stream.iterate("a", new IdentidadUnaria());
            return 1;
        } catch (UnsupportedOperationException e) {
            // esperado
        }
        try {
            IntStream.generate(new UnoInt());
            return 2;
        } catch (UnsupportedOperationException e) {
            // esperado
        }
        try {
            IntStream.iterate(0, new MasUno());
            return 3;
        } catch (UnsupportedOperationException e) {
            // esperado
        }
        try {
            java.util.stream.LongStream.generate(new UnoLong());
            return 4;
        } catch (UnsupportedOperationException e) {
            // esperado
        }
        try {
            java.util.stream.DoubleStream.generate(new UnoDouble());
            return 5;
        } catch (UnsupportedOperationException e) {
            // esperado
        }
        return -1;
    }

    public static void main(String[] args) {
        System.out.println(StreamGatherTest.run());
    }
}

final class IdentidadUnaria implements java.util.function.UnaryOperator<String> {
    public String apply(String s) {
        return s;
    }
}

final class UnoInt implements java.util.function.IntSupplier {
    public int getAsInt() {
        return 1;
    }
}

final class MasUno implements java.util.function.IntUnaryOperator {
    public int applyAsInt(int v) {
        return v + 1;
    }
}

final class UnoLong implements java.util.function.LongSupplier {
    public long getAsLong() {
        return 1L;
    }
}

final class UnoDouble implements java.util.function.DoubleSupplier {
    public double getAsDouble() {
        return 1.0d;
    }
}
