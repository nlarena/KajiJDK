import java.util.ArrayList;
public class GcP2 {
    public static int run() {
        ArrayList<String> a = new ArrayList<String>();
        a.add("x"); a.add("y");
        int n = 0;
        int i = 0;
        while (i < 300) {
            Object[] t = a.toArray();                // NO pasa por el nativo
            n = n + t.length;
            i = i + 1;
        }
        return n;
    }
}
