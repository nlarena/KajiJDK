public class BoxA {
    public static int run() {
        Object[] a = { "x", 7 };          // el 7 tiene que boxearse
        return ((Integer) a[1]).intValue();
    }
}
