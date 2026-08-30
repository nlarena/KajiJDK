import java.util.function.Predicate;
public class LamP5<E> {
    static <T> boolean f(Predicate<? super T> p, T v) { return p.test(v); }
    public static int run() {
        return f(s -> ((String) s).length() == 2, "ab") ? 1 : 0;
    }
}
