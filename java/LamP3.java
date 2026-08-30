import java.util.ArrayList;
public class LamP3 {
    public static int run() {
        ArrayList<String> e = new ArrayList<String>();
        e.add("aa"); e.add("b");
        e.removeIf(s -> true);
        return e.size();
    }
}
