import java.util.Arrays;
public class ArrStress {
    public static int run() {
        int n = 0;
        int i = 0;
        while (i < 300) {
            int[] a = { 5, 3, 9, 1, 7 };
            Arrays.sort(a);                 // aloca el scratch del merge sort
            n = n + a[0];
            i = i + 1;
        }
        return n;
    }
    public static int soloCopias() {
        int n = 0;
        int i = 0;
        while (i < 300) {
            String[] s = { "a", "bb" };
            String[] c = Arrays.copyOf(s, 2);   // pasa por Array.newInstance
            n = n + c.length;
            i = i + 1;
        }
        return n;
    }
}
