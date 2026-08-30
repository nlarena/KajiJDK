import java.util.*;
public class Ct6 {
    public static int run() {
        try {
            Set<String> s = Set.of();
            return s.size() + 100;
        } catch (Throwable t) {
            String n = t.getClass().getName();
            if (n.equals("java.lang.UnsupportedOperationException")) return 1;
            if (n.equals("java.lang.NullPointerException")) return 2;
            if (n.equals("java.lang.AbstractMethodError")) return 3;
            if (n.equals("java.lang.NoSuchMethodError")) return 4;
            if (n.equals("java.lang.ClassCastException")) return 5;
            if (n.equals("java.lang.IllegalArgumentException")) return 6;
            return 999;
        }
    }
}
