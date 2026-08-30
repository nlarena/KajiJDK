import java.util.*;
public class Cs2 {
    public static int run() {
        int r = 0;
        ArrayList<String> a = new ArrayList<String>(); a.add("x"); a.add("y");
        ArrayList<String> c = new ArrayList<String>(); c.add("y");
        a.removeAll(c); r = r + a.size();
        return r;
    }
}
