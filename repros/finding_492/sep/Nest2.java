public class Nest2 {
    public static int run() {
        Sub s = new Sub();
        if (s.armar().leer() != 7) { return 1; }
        return -1;
    }
}
