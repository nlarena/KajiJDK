import java.lang.reflect.Method;
public class JmxP8 {
    public static abstract class A { public abstract int f(); }
    public static class C extends A { public int f() { return 3; } }
    public static int run() throws Exception {
        Method m = A.class.getMethod("f");        // abstracto, pero de una CLASE
        System.out.println("abstracta: " + m.invoke(new C()));
        return -1;
    }
}
