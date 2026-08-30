import java.util.Arrays;
import java.util.Comparator;
public class Ar6 {
    public static int run() {
        double[] d = { 1.0d, 0.0d / 0.0d, -0.0d, 0.0d, -1.0d };
        Arrays.sort(d);
        return (d[0] == -1.0d ? 1 : 0) + (Double.isNaN(d[4]) ? 10 : 0);
    }
}
