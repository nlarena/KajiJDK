import java.util.Arrays;
import java.util.Comparator;
public class Ar7 {
    public static int run() {
        String[] s = { "dddd", "bb", "cc", "a" };
        Comparator<String> c = new PorLargo2();
        Arrays.sort(s, c);
        return Arrays.toString(s).length();
    }
}
