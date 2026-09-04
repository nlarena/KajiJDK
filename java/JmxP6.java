import java.lang.reflect.Method;
public class JmxP6 {
    public static class C { public int f() { return 3; } }
    public static int run() throws Exception {
        Method m = C.class.getMethod("f");
        Object r = m.invoke(new C());
        System.out.println("clase: " + r);
        return -1;
    }
}
