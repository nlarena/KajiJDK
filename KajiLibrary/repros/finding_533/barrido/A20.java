import java.util.function.*;
public class A20 {
    public static void run() { main(new String[0]); }
    static int campo = 4;
    public static void main(String[] a) {
        System.out.println(new Supplier<Integer>() { public Integer get() { return campo * 10 + 1; } }.get());
        int base = 4;
        System.out.println(new Supplier<Integer>() { public Integer get() { int b = base; return b; } }.get());
        System.out.println(new Supplier<Integer>() { public Integer get() { return base; } }.get());
    }
}
