import java.util.*;
public class Cs1 {
    public static int run() {
        int r = 0;
        ArrayList<String> a = new ArrayList<String>(); a.add("x"); a.add("y");
        ArrayList<String> b = new ArrayList<String>(); b.add("y"); b.add("z");
        a.addAll(b); r = r + a.size();
        return r;
    }
}
