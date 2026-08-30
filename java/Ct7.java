import java.util.*;
public class Ct7 {
    static String nombre = "";
    public static int run() {
        try {
            Set<String> s = Set.of();
            return -1;
        } catch (Throwable t) {
            nombre = t.getClass().getName();
            return nombre.length();
        }
    }
    public static int ch0() { run(); return nombre.charAt(nombre.length() - 1); }
    public static int ch1() { run(); return nombre.charAt(nombre.lastIndexOf('.') + 1); }
    public static int ch2() { run(); return nombre.charAt(nombre.lastIndexOf('.') + 2); }
    public static int ch3() { run(); return nombre.charAt(nombre.lastIndexOf('.') + 3); }
}
