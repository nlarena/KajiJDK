import java.util.*;
public class Sig2<T extends Number, U> {
    Map<String, List<T>> m;
    T[] arr;
    List<? extends Number> lo;
    List<? super Integer> hi;
    List<?> any;
    <V> V pick(V a, U b) { return a; }
    void w(List<? extends T> x) {}
}
