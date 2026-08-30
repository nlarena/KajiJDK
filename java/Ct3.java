import java.util.*;
public class Ct3 {
    public static int run() {
        int r = 0;
        Set<String> s = Set.of("p"); r = r + (s.contains("p") ? 1 : 0);
        return r;
    }
}
