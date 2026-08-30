import java.util.ArrayList;
import java.util.function.Predicate;
public class LamP {
    public static int run() {
        ArrayList<String> e = new ArrayList<String>();
        e.add("aa"); e.add("b");
        e.removeIf((String s) -> s.length() == 2);
        return e.size();
    }
}
