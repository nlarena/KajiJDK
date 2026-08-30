import java.util.Arrays;
import java.util.Comparator;
public class Ar9 {
    public static int run() {
        int[] p = { 1, 2, 3, 4 };
        Arrays.parallelPrefix(p, new Suma2());
        return p[3];
    }
}
