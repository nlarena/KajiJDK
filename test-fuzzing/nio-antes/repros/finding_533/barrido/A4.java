import java.util.function.Supplier;
public class A4 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    String nombre = "ext";
    public static void main(String[] a) { new A4().go("p"); }
    void go(String p) {
        String s = "s";
        Supplier<String> sup = () -> correr(new F() {
            public String f() {
                String t = "t";
                return correr(new F() { public String f() { return nombre + s + t + p; } });
            }
        });
        System.out.println(sup.get());
        F g = new F() {
            public String f() {
                Supplier<String> q = () -> correr(new F() { public String f() { return "L" + s + p + nombre; } });
                return q.get();
            }
        };
        System.out.println(g.f());
    }
}
