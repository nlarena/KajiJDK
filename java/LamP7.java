import java.util.function.Predicate;
public class LamP7<E> {
    E v;
    boolean f(Predicate<? super E> p) { return p.test(this.v); }
    public static int run() {
        LamP7<String> b = new LamP7<String>();
        b.v = "ab";
        return b.f(s -> s.length() == 2) ? 1 : 0;
    }
}
