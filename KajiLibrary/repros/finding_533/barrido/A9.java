import java.util.function.Supplier;
public class A9 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    public static void main(String[] a) {
        String s = "s";
        Supplier<F> sup = () -> new F() { public String f() { return "lam" + s; } };
        System.out.println(sup.get().f());
    }
}
