import java.util.function.*;
public class Lam {
    Runnable r = () -> System.out.println("hi");
    Supplier<Integer> s = () -> 42;
    Function<String,Integer> f = String::length;
}
