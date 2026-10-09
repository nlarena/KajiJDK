import java.util.ArrayList;
public class Control402Diamante {
    public static int run() {
        ArrayList<String> g = new ArrayList<>(10) { public String toString() { return "diamante" + size(); } };
        g.add("a");
        System.out.println("//control402diamante " + g);
        return 0;
    }
    public static void main(String[] x) { run(); }
}
