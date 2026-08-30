import java.util.function.Predicate;
public class LamP8<E> {
    E v;
    boolean f(Predicate<E> p) { return p.test(this.v); }
    public static int run() {
        LamP8<String> b = new LamP8<String>();
        b.v = "ab";
        return b.f(s -> s.length() == 2) ? 1 : 0;
    }
}
