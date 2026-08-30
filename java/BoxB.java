public class BoxB {
    public static int run() {
        Object o = 7;                     // boxeo en asignacion simple
        return ((Integer) o).intValue();
    }
}
