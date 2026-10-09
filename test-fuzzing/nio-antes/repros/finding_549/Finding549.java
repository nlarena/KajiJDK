import java.util.*;
import java.util.function.*;

/**
 * Finding #549: una referencia a un metodo **sobrecargado** (`sb::append`) elige la sobrecarga sin
 * mirar el tipo del parametro de la interfaz funcional. Para un `Consumer<Object>` sale
 * `StringBuilder.append(char)`, y la HotSpot rechaza el `invokedynamic` al enlazarlo
 * ("Type mismatch for lambda argument 1: class java.lang.Object is not convertible to char").
 * El JDK elige `append(Object)` e imprime `//549 q`.
 */
public class Finding549 {
    public static int run() {
        StringBuilder sb = new StringBuilder();
        Consumer<Object> c = sb::append;
        c.accept("q");
        System.out.println("//549 " + sb);
        return sb.length();
    }

    public static void main(String[] args) { run(); }
}
