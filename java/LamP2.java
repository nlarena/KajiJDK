import java.util.ArrayList;
import java.util.function.Predicate;
public class LamP2 {
    public static int run() {
        ArrayList<String> e = new ArrayList<String>();
        e.add("aa"); e.add("b");
        Predicate<String> p = s -> s.length() == 2;
        e.removeIf(p);
        return e.size();
    }
}
