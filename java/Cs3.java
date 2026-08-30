import java.util.*;
public class Cs3 {
    public static int run() {
        int r = 0;
        ArrayList<String> a = new ArrayList<String>(); a.add("z");
        Object[] arr = a.toArray(); r = r + arr.length;
        String[] t = a.toArray(new String[0]); r = r + t.length;
        return r;
    }
}
