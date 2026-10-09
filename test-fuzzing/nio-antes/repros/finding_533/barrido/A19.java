import java.util.function.*;
public class A19 {
    public static void run() { main(new String[0]); }
    public static void main(String[] a) {
        int base = 4;
        System.out.println(new Supplier<Integer>() { public Integer get() { return base * 10 + 1; } }.get());
        Supplier<Integer> s = new Supplier<Integer>() { public Integer get() { return base * 10 + 2; } };
        System.out.println(s.get());
    }
}
