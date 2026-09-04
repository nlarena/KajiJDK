import java.lang.reflect.Method;
public class JmxP7 {
    public interface I { int f(); }
    public static class C implements I { public int f() { return 3; } }
    public static int run() throws Exception {
        Method m = I.class.getMethod("f");
        Object r = m.invoke(new C());
        System.out.println("iface: " + r);
        return -1;
    }
}
