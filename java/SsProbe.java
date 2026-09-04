// Sonda de java.util.stream.StreamSupport: comprueba que las OCHO fabricas se pueden llamar de
// verdad, incluidas las cuatro diferidas, cuyo parametro `Supplier<? extends Spliterator<T>>`
// anida una variable de tipo (la forma que rompe en java/WcLib2.java).
//
//   ./bin/javac.exe --emit -cp KajiLibrary java/SsProbe.java
//   ./bin/run-headless.exe java/SsProbe.class run   ->  -1
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

class SpSup implements Supplier<Spliterator<String>> {
    private final String[] a;
    SpSup(String[] a) { this.a = a; }
    public Spliterator<String> get() { return Stream.of(this.a).spliterator(); }
}
class IntSpSup implements Supplier<Spliterator.OfInt> {
    public Spliterator.OfInt get() { int[] v = new int[3]; v[0]=1; v[1]=2; v[2]=3; return IntStream.of(v).spliterator(); }
}
class LongSpSup implements Supplier<Spliterator.OfLong> {
    public Spliterator.OfLong get() { long[] v = new long[2]; v[0]=1L; v[1]=2L; return LongStream.of(v).spliterator(); }
}
class DblSpSup implements Supplier<Spliterator.OfDouble> {
    public Spliterator.OfDouble get() { double[] v = new double[2]; v[0]=1.5d; v[1]=2.5d; return DoubleStream.of(v).spliterator(); }
}

public class SsProbe {
    public static int run() {
        String[] a = new String[3];
        a[0] = "x"; a[1] = "y"; a[2] = "z";
        if (StreamSupport.stream(Stream.of(a).spliterator(), false).count() != 3L) { return 0; }
        if (StreamSupport.stream(new SpSup(a), 0, false).count() != 3L) { return 1; }
        int[] v = new int[3]; v[0]=1; v[1]=2; v[2]=3;
        if (StreamSupport.intStream(IntStream.of(v).spliterator(), false).sum() != 6) { return 2; }
        if (StreamSupport.intStream(new IntSpSup(), 0, false).sum() != 6) { return 3; }
        long[] w = new long[2]; w[0]=1L; w[1]=2L;
        if (StreamSupport.longStream(LongStream.of(w).spliterator(), false).sum() != 3L) { return 4; }
        if (StreamSupport.longStream(new LongSpSup(), 0, false).sum() != 3L) { return 5; }
        double[] d = new double[2]; d[0]=1.5d; d[1]=2.5d;
        if (StreamSupport.doubleStream(DoubleStream.of(d).spliterator(), false).sum() != 4.0d) { return 6; }
        if (StreamSupport.doubleStream(new DblSpSup(), 0, false).sum() != 4.0d) { return 7; }
        return -1;
    }
}
