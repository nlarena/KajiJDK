import java.util.*;
public class Hr1 {
    public static int run() {
        HashMap<String,Integer> m = new HashMap<String,Integer>();
        m.put("a", 1); m.put("b", 2);
        m.remove("a");
        return m.values().size();
    }
}
