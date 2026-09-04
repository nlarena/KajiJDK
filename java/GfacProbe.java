// Sonda de java.util.stream.Gatherer: comprueba que TODAS las fabricas que este javac si puede
// resolver andan de verdad — `ofSequential(Integrator)`, `ofSequential(Supplier, Integrator)`,
// `of(Integrator)`, `Integrator.of`, `Integrator.ofGreedy`, `defaultInitializer()` y la negativa
// de `defaultCombiner()`. Las cuatro que reciben un `BiConsumer` finalizador NO se pueden llamar
// hoy y por eso no estan aca; el repro de ese defecto es java/WcLib2.java + java/WcUse2.java.
//
//   ./bin/javac.exe --emit -cp KajiLibrary java/GfacProbe.java
//   ./bin/run-headless.exe java/GfacProbe.class run   ->  -1
import java.util.stream.Gatherer;
import java.util.stream.Stream;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

class GpSup implements Supplier<Object[]> { public Object[] get() { return new Object[1]; } }
class GpItr implements Gatherer.Integrator<Object[], String, String> {
    public boolean integrate(Object[] s, String e, Gatherer.Downstream<? super String> d) { return d.push(e); }
}
class GpItr0 implements Gatherer.Integrator<Void, String, String> {
    public boolean integrate(Void s, String e, Gatherer.Downstream<? super String> d) { return d.push(e); }
}
class GpGreedy implements Gatherer.Integrator.Greedy<Void, String, String> {
    public boolean integrate(Void s, String e, Gatherer.Downstream<? super String> d) { return d.push(e); }
}

public class GfacProbe {
    public static int run() {
        String[] a = new String[2];
        a[0] = "x"; a[1] = "y";
        // 1-arg ofSequential(Integrator)
        Gatherer<String, Void, String> g1 = Gatherer.ofSequential(new GpItr0());
        if (Stream.of(a).gather(g1).count() != 2L) { return 0; }
        // 2-arg ofSequential(Supplier, Integrator)
        Gatherer<String, Object[], String> g2 = Gatherer.ofSequential(new GpSup(), new GpItr());
        if (Stream.of(a).gather(g2).count() != 2L) { return 1; }
        // 1-arg of(Integrator)
        Gatherer<String, Void, String> g3 = Gatherer.of(new GpItr0());
        if (Stream.of(a).gather(g3).count() != 2L) { return 2; }
        // Integrator.of / ofGreedy
        Gatherer.Integrator<Void, String, String> i1 = Gatherer.Integrator.of(new GpItr0());
        Gatherer.Integrator.Greedy<Void, String, String> i2 = Gatherer.Integrator.ofGreedy(new GpGreedy());
        if (i1 == null || i2 == null) { return 3; }
        // defaultInitializer / defaultCombiner / defaultFinisher
        Supplier<Object> s = Gatherer.defaultInitializer();
        if (s.get() != null) { return 4; }
        BinaryOperator<Object> c = Gatherer.defaultCombiner();
        try { c.apply("a", "b"); return 5; } catch (UnsupportedOperationException e) { }
        return -1;
    }
}
