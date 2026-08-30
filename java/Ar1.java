import java.util.Arrays;
import java.util.List;
public class Ar1 {
    public static int run() {
        int[] a = { 5, 3, 9, 1 };
        Arrays.sort(a);
        return a[0] * 10 + a[3];
    }
}
