import java.util.ArrayList;
public class LamP6 {
    public static int run() {
        ArrayList<String> e = new ArrayList<String>();
        e.add("aa");
        e.forEach(s -> s.length());
        return e.size();
    }
}
