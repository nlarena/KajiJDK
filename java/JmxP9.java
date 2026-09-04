import java.lang.reflect.Method;
public class JmxP9 {
    public interface I { default int f() { return 1; } }
    public static class C implements I { public int f() { return 3; } }
    public static int run() throws Exception {
        Method m = I.class.getMethod("f");        // default: SI tiene Code
        System.out.println("default: " + m.invoke(new C()));
        return -1;
    }
}
