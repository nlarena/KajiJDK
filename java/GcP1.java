import java.util.ArrayList;
public class GcP1 {
    public static int run() {
        ArrayList<String> a = new ArrayList<String>();
        a.add("x"); a.add("y");
        int n = 0;
        int i = 0;
        while (i < 300) {
            String[] t = a.toArray(new String[0]);   // pasa por Array.newArray
            n = n + t.length;
            i = i + 1;
        }
        return n;
    }
}
