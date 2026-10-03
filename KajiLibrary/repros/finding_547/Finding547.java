import java.util.function.*;

/**
 * Finding #547: una referencia a metodo con un objetivo funcional **con comodines** lleva un
 * `instantiatedMethodType` borrado a `Object` donde el JDK pone el tipo del comodin.
 *
 * `Function<? super String, ? extends Integer> f = String::length;` sale con `(Object)Integer`; el
 * JDK escribe `(String)Integer`. En la HotSpot, la `LambdaMetafactory` lo rechaza:
 * "Invalid receiver type class java.lang.Object; not a subtype of implementation type class
 * java.lang.String". Con el objetivo sin comodines (`Function<String, Integer>`) sale bien.
 */
public class Finding547 {
    public static int run() {
        Function<? super String, ? extends Integer> f = String::length;
        Object r = f.apply("abcd");
        return (Integer) r;
    }

    public static void main(String[] args) { System.out.println("//547 " + run()); }
}
