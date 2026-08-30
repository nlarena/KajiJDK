import java.util.HashMap;
public class HmProbe {
    public static int run() {
        HashMap<String,Integer> m = new HashMap<String,Integer>();
        m.put("a", 1);
        return m.size() + m.entrySet().size();
    }
}
