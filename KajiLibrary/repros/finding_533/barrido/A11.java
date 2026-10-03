import java.util.function.Supplier;
public class A11 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        String s = "s";
        System.out.println(correr(new F() {
            public String f() { Supplier<String> q = () -> "lam" + s; return q.get(); }
        }));
    }
}
