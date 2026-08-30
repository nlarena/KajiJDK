public class BoxC {
    static int toma(Object o) { return ((Integer) o).intValue(); }
    public static int run() {
        return toma(7);                   // boxeo en argumento
    }
}
