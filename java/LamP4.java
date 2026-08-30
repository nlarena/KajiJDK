import java.util.function.Predicate;
public class LamP4 {
    static boolean f(Predicate<String> p) { return p.test("ab"); }
    public static int run() {
        return f(s -> s.length() == 2) ? 1 : 0;
    }
}
