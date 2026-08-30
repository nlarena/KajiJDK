import java.util.Arrays;
import java.util.List;
public class Ar3 {
    public static int run() {
        String[] s = { "a", "bb", "ccc" };
        String[] c = Arrays.copyOf(s, 2);
        return c.length * 10 + c[1].length();
    }
}
