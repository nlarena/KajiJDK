import java.util.function.*;
public class A21 {
    public static void run() { main(new String[0]); }
    static int campo = 4;
    static Integer g() { return campo * 10 + 1; }
    static class N implements Supplier<Integer> { public Integer get() { return campo + 2; } }
    public static void main(String[] a) {
        System.out.println(g());
        System.out.println(new N().get());
        class L implements Supplier<Integer> { public Integer get() { return campo + 3; } }
        System.out.println(new L().get());
        System.out.println(new Supplier<Integer>() { public Integer get() { return 5; } }.get());
    }
}
