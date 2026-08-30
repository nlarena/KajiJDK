public class AmbB {
    static int f(int[] a, int[] b) { return 1; }
    static int f(long[] a, long[] b) { return 2; }
    public static int run() {
        int[] x = { 1 };
        int[] y = { 2 };
        return f(x, y);
    }
}
