import java.util.Arrays;
public class CopyQ {
    public static int run() {
        int[] orden = { 1, 3, 5 };
        int[] c = Arrays.copyOf(orden, 2);
        return c.length;
    }
}
